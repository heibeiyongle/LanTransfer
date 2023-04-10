package com.thunder.lantransf.server.video.socketserver;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.thunder.common.lib.dto.Beans;
import com.thunder.common.lib.codec.ByteBufferCodecImpl;
import com.thunder.common.lib.codec.IByteBufferDealer;
import com.thunder.common.lib.transf.nio.AbsSocketCommon;
import com.thunder.common.lib.transf.nio.SocketWrapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by zhe on 2022/6/19 22:59
 *
 * @desc:
 */
public class SocketServerImpl extends AbsSocketCommon implements ISocketServer{
    private static final String TAG = "SocketServerImpl";
    IServerSocCb mNotify = null;
    IByteBufferDealer bufferDealer = new ByteBufferCodecImpl();
    private HashMap<SocketChannel,SocketWrapper> clientsMap = new HashMap<>();

    @Override
    public void startServer(int port , IServerSocCb serverSocCb) {
        mNotify = serverSocCb;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            innerStart(port);
        }else {
            Log.e(TAG, "startServer failed, Build.VERSION.SDK_INT ["+Build.VERSION.SDK_INT +"] < N(24) ");
        }
    }

    @Override
    public boolean publishVideo(Beans.VideoData video) {
        return innerPublishMsg(video);
    }

    @Override
    public boolean publishMsg(Beans.TransfPkgMsg msg) {
        Log.i(TAG, "publishMsg: msg: "+msg+", clientSize: "+clientsMap.size());
        return innerPublishMsg(msg);
    }

    private boolean innerPublishMsg(Beans.ITransferPkg msg ){
        // add msg to client wrapper que
        if(msg == null){
            Log.i(TAG, "innerPublishMsg: param is null! return! ");
            return false;
        }
        if(clientsMap.size() == 0){
            return true;
        }
        Set<String> targets = msg.getTargets();
        Iterator<SocketChannel> iterator = clientsMap.keySet().iterator();
        while (iterator.hasNext()){
            SocketWrapper tmp = clientsMap.get(iterator.next());
            if(tmp != null){
                if(targets == null /* null for broadcast */||
                        targets.contains(tmp.getClientName())){
                    addMsgToTargetQue(tmp,msg);
                }
            }
        }
        return true;
    }


    private void addMsgToTargetQue(SocketWrapper target, Object msg){
        if(target == null) {
            return;
        }
        if(target.getToSendMsgQue().remainingCapacity() == 0){
            //todo 是否要主动断开client
            Object toRemove = target.getToSendMsgQue().poll();
            Log.e(TAG, "publishMsg: client-que-full, so rm first item :"+toRemove );
        }
        target.getToSendMsgQue().add(msg);
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    private void innerStart(int port){
        try {
            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);
            if(port != -1){
                ssc.bind(new InetSocketAddress(port));
            }else {
                ssc.bind(null);
            }
            if(mNotify != null){
                mNotify.onInitSuc(ssc.socket().getLocalPort());
            }
            clientsMap.clear();
            Selector selector = Selector.open();
            ssc.register(selector, SelectionKey.OP_ACCEPT);
            while (selector.isOpen()){
                long tmpStartMs = System.currentTimeMillis();

                int tmp = selector.select(1000);
                if((System.currentTimeMillis() - tmpStartMs) > 100){
                    Log.i(TAG, "innerStart: selector wake, key-count:"+tmp+" cost-Ms: "+(System.currentTimeMillis() - tmpStartMs));
                }
                if(tmp <= 0){
                    continue;
                }
                Set<SelectionKey> sets = selector.selectedKeys();
                Iterator<SelectionKey> iterator = sets.iterator();
                while (iterator.hasNext()){
                    SelectionKey selectionKey = iterator.next();
                    iterator.remove();
                    if(selectionKey.isAcceptable()){
                        Log.i(TAG, "innerStart: isAcceptable");
                        ServerSocketChannel serverC = (ServerSocketChannel) selectionKey.channel();
                        SocketChannel sc = serverC.accept();
                        sc.configureBlocking(false);
                        Log.i(TAG, "innerStart: sc.getOption(StandardSocketOptions.TCP_NODELAY): "+sc.getOption(StandardSocketOptions.TCP_NODELAY));
                        Log.i(TAG, "innerStart--read: SO_SNDBUF/REC: "+sc.getOption(StandardSocketOptions.SO_SNDBUF)+" / "+sc.getOption(StandardSocketOptions.SO_RCVBUF));
                        sc.register(selector,SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                        SocketWrapper sw = new SocketWrapper(sc);
                        sw.setClientName(genClientName());
                        clientsMap.put(sc,sw);
                        mNotify.onGotClient(sw.getClientName(),sc.socket().getLocalAddress().getHostName());
                    }else if(selectionKey.isReadable()){
//                        Log.i(TAG, "innerStart: isReadable");
                        // to read
                        SocketChannel sc = (SocketChannel) selectionKey.channel();
                        SocketWrapper socketWrapper = clientsMap.get(sc);
                        if(socketWrapper == null){
                            continue;
                        }
                        try {
                            int readLen = sc.read(socketWrapper.getReadBuf());
                            printSpeed(TAG,0,readLen);
                            if(readLen < 0){
                                Log.i(TAG, " client ["+socketWrapper.getClientName()+"] lost, remove it! For read : "+readLen+"");
                                removeClient(sc);
                                continue;
                            }
//                            Log.i(TAG, "innerStart--read: SO_SNDBUF/REC: "+sc.getOption(StandardSocketOptions.SO_SNDBUF)+" / "+sc.getOption(StandardSocketOptions.SO_RCVBUF));
                        }catch (Exception e){
                            Log.e(TAG, " socket read-err, remove client["+socketWrapper.getClientName()+"], errInfo: ", e);
                            removeClient(sc);
                            continue;
                        }
                        Object tmpMsg = null;
                        int i = 0;
//                        Log.i(TAG, "innerStart before-decode: readBuf: "+socketWrapper.getReadBuf());
                        while (socketWrapper.getReadBuf().position() > 0 &&
                                (tmpMsg = bufferDealer.decodeReadBuf(socketWrapper.getReadBuf())) != null){
//                            Log.i(TAG, "innerStart decode-index: "+(i++)+" readBuf: "+socketWrapper.getReadBuf());
                            if(tmpMsg instanceof Beans.VideoData){
                                // nothing
                            }else if(tmpMsg instanceof Beans.TransfPkgMsg){
                                mNotify.onGotCmdMsg(socketWrapper.getClientName(), (Beans.TransfPkgMsg) tmpMsg);
                            }else if(tmpMsg instanceof ByteBuffer){
                                socketWrapper.setReadBuf( (ByteBuffer) tmpMsg);
                            }
                        }
                    }else if(selectionKey.isWritable()){
//                        Log.i(TAG, "innerStart: isWritable");
                        SocketChannel sc = (SocketChannel) selectionKey.channel();
                        SocketWrapper socketWrapper = clientsMap.get(sc);
                        if(socketWrapper == null){
                            continue;
                        }
                        if(socketWrapper.getWriteBuf().remaining() == 0 && socketWrapper.getToSendMsgQue().size() > 0){
//                            Log.i(TAG, "server encode-before buf: "+socketWrapper.writeBuf);
                            socketWrapper.setWriteBuf(bufferDealer.encodeWriteBuf(socketWrapper.getToSendMsgQue(),socketWrapper.getWriteBuf()));
//                            Log.i(TAG, "server encode-end buf: "+socketWrapper.writeBuf);
                        }
                        if(socketWrapper.getWriteBuf().remaining() == 0) {
                            continue;
                        }
                        try {
//                            Log.i(TAG, "server-2 write-before buf: "+socketWrapper.getWriteBuf());
                            int res = sc.write(socketWrapper.getWriteBuf());
                            printSpeed(TAG,res,0);
                            // note: que size always 0, 说明发送端没有浪费时间
                            // 目前存在延迟，sc write 出去， 到 sc read 后， costMs > 400ms, 当消息密集的时候
//                            Log.i(TAG, "server write-end toSendQueSize: "+socketWrapper.getToSendMsgQue().size()+" len: " +res+", buf: "+socketWrapper.getWriteBuf());
//                            Log.i(TAG, "innerStart--write: SO_SNDBUF/REC: "+sc.getOption(StandardSocketOptions.SO_SNDBUF)+" / "+sc.getOption(StandardSocketOptions.SO_RCVBUF));
                        }catch (Exception e){
                            Log.e(TAG, " write-data-err, remove client ["+socketWrapper.getClientName()+ "] ",e);
                            removeClient(sc);
                            continue;
                        }
                    }
                }
                if(System.currentTimeMillis() - tmpStartMs > 100){
                    Log.i(TAG, "innerStart: per-loop-COST-LONG(>100ms) cost-Ms: "+(System.currentTimeMillis() - tmpStartMs));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "innerStart: ", e);
        }finally {
            mNotify.onStopped();
            clientsMap.clear();
        }
    }

    private void removeClient(SocketChannel socketChannel){
        Log.i(TAG, "removeClient: sc: "+socketChannel);
        SocketWrapper tmp = clientsMap.remove(socketChannel);
        if(tmp != null){
            tmp.getToSendMsgQue().clear();
            mNotify.onLostClient(tmp.getClientName());
            Log.d(TAG, "removeClient() remove["+tmp.getClientName()+"] remain-Cnt: "+clientsMap.size());
        }
    }

    private HashSet<String> clientNameCacheSet = new HashSet<>();
    String genClientName(){
        String res ="xxxx-"+System.nanoTime();
        while (clientNameCacheSet.contains(res)){
            res = "xxxx-"+(System.nanoTime()+1);
        }
        clientNameCacheSet.add(res);
        return  res;
    }


}
