package com.thunder.lantransf.client.video.socketclient;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.thunder.common.lib.codec.ByteBufferCodecImpl;
import com.thunder.common.lib.codec.IByteBufferDealer;
import com.thunder.common.lib.dto.Beans;
import com.thunder.common.lib.transf.nio.AbsSocketCommon;
import com.thunder.common.lib.transf.nio.SocketWrapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by zhe on 2022/6/19 22:59
 *
 * @desc:
 */
public class ClientSocketImpl extends AbsSocketCommon implements ISocketClient {
    private static final String TAG = "ClientSocketImpl";
    IClientSocCb mNotify = null;
    IByteBufferDealer bufferDealer = new ByteBufferCodecImpl();

    @Override
    public void connect(String host, int port , IClientSocCb cb) {
        mNotify = cb;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            innerStart(host,port);
        }else {
            Log.e(TAG, "startServer failed, Build.VERSION.SDK_INT ["+Build.VERSION.SDK_INT +"] < N(24) ");
        }
    }

    @Override
    public boolean sendMsg(Beans.TransfPkgMsg msg) {
        return innerPublishMsg(msg);
    }

    private boolean innerPublishMsg(Beans.ITransferPkg msg ){
        // add msg to client wrapper que
        if(msg == null){
            Log.i(TAG, "innerPublishMsg: param is null! return! ");
            return false;
        }
        if( client == null){
            return false;
        }
        addMsgToTargetQue(client, msg);
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
    SocketWrapper client = null;
    Selector selector = null;
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void innerStart(String host, int port){
        try {
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.connect(new InetSocketAddress(host,port));
            selector = Selector.open();
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
            while (selector.isOpen()){
                int tmp = selector.select(1000);
                if(tmp <= 0){
                    continue;
                }
                Set<SelectionKey> sets = selector.selectedKeys();
                Iterator<SelectionKey> iterator = sets.iterator();
                while (iterator.hasNext()){
                    SelectionKey selectionKey = iterator.next();
                    iterator.remove();
                    if(selectionKey.isConnectable()){
                        SocketChannel tmpSc = (SocketChannel)selectionKey.channel();
                        if (tmpSc.finishConnect()) {
                            Socket socket = tmpSc.socket();
                            mNotify.onConnectSuc(socket.getLocalAddress().getHostAddress());
                        } else {
                            mNotify.onClosed();
                        }
                        tmpSc.configureBlocking(false);
                        tmpSc.register(selector,SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                        client = new SocketWrapper(tmpSc);
                    }else if(selectionKey.isReadable()){
                        // to read
//                        Log.i(TAG, " client read able! ");
                        SocketChannel sc = (SocketChannel) selectionKey.channel();
                        SocketWrapper socketWrapper = client;
                        if(socketWrapper == null){
                            continue;
                        }
                        try {
//                            Log.i(TAG, " client-read-before: buf: "+socketWrapper.readBuf);
                            int readLen = sc.read(socketWrapper.getReadBuf());
//                            Log.i(TAG, " client-read-end: buf: "+socketWrapper.readBuf);
                            printSpeed(TAG,0,readLen);
                            if(readLen < 0){
                                Log.i(TAG, " client ["+socketWrapper.getClientName()+"] lost, remove it! For read : "+readLen+"");
                                removeClient(sc);
                            }
                        }catch (Exception e){
                            Log.e(TAG, " socket read-err, remove client["+socketWrapper.getClientName()+"], errInfo: ", e);
                            removeClient(sc);
                        }

                        Object res = bufferDealer.decodeReadBuf(socketWrapper.getReadBuf());
                        if(res == null){
                            continue;
                        }else if(res instanceof Beans.VideoData){
                            mNotify.onGotVideoMsg((Beans.VideoData) res);
                        }else if(res instanceof Beans.TransfPkgMsg){
                            mNotify.onGotCmdMsg((Beans.TransfPkgMsg) res);
                        }else if(res instanceof ByteBuffer){
                            socketWrapper.setReadBuf((ByteBuffer) res);
                        }
                    }else if(selectionKey.isWritable()){
                        SocketChannel sc = (SocketChannel) selectionKey.channel();
                        SocketWrapper socketWrapper = client;
                        if(socketWrapper == null){
                            continue;
                        }
                        if(socketWrapper.getWriteBuf().remaining() == 0 && socketWrapper.getToSendMsgQue().size() > 0){
                            socketWrapper.setWriteBuf(bufferDealer.encodeWriteBuf(socketWrapper.getToSendMsgQue(),socketWrapper.getWriteBuf()));
                        }
                        if(socketWrapper.getWriteBuf().remaining() == 0){
                            continue;
                        }
                        try {
//                            Log.i(TAG, " client write-before buf: "+socketWrapper.getWriteBuf().toString() );
                            int res = sc.write(socketWrapper.getWriteBuf());
                            printSpeed(TAG,res,0);
                            if(res < 0){
                                removeClient(sc);
                            }
//                            Log.i(TAG, " client write-end write-len: "+res+", buf: "+ socketWrapper.getWriteBuf().toString());
                        }catch (Exception e){
                            Log.e(TAG, " write-data-err, remove client ["+socketWrapper.getClientName()+ "] ",e);
                            removeClient(sc);
                        }
                    }
                }
            }
            mNotify.onClosed();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "innerStart: ", e);
        }
    }

    private void removeClient(SocketChannel socketChannel){
        Log.i(TAG, "removeClient: sc: "+socketChannel);
        mNotify.onClosed();
        if(client != null){
            client.getToSendMsgQue().clear();
        }
        client = null;
        if(selector != null){
            try {
                selector.close();
            } catch (IOException e) {
                Log.e(TAG, "removeClient: ", e);
            }
        }
    }


    @Override
    public boolean updateClientInfo(String clientName) {
        if(client == null){
            return false;
        }
        client.setClientName( clientName);
        return true;
    }
}
