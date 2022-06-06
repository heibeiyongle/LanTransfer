package com.thunder.common.lib.transf;

import android.util.Log;


import com.thunder.common.lib.dto.Beans;
import com.thunder.common.lib.util.GsonUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class SocketDealer implements ITransf {
    private static final String TAG = "SocketDealer";
    protected Socket mSoc;
    private ISocDealCallBack mCb;
    private OutputStream mOus;
    public SocketDealer(){
    }

    @Override
    public void init(Socket socket,ISocDealCallBack cb) {
        mSoc = socket;
        mCb = cb;
    }

    public void begin(){
        dealClient();
    }

    public static final int MAX_DATA_LEN = 400*1024*1024; // 400M
    private void dealClient(){
        // read data
        new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    InputStream ins = mSoc.getInputStream();
                    mOus = mSoc.getOutputStream();
                    InetAddress localAddress = mSoc.getLocalAddress();
                    String localHost = null;
                    if(localAddress != null){
                        localHost = localAddress.getHostName();
                    }
                    if(mCb!=null){
                        mCb.onGotOus(mOus,localHost);
                    }
                    /** [len 4][type][data]
                        data:[channel][payload]
                            json: 0 {"xxxxxx"}
                            bytes:1 [xxxxxxxxx]
                                def: [head-len][cfg][KEY-FRAME][W][H][....][FRAME-VIDEO-DATA]
                     */
                    byte[] bufLen = new byte[4];
                    int tmpReadLen = -1;

                    while ((tmpReadLen = ins.read(bufLen)) > 0){
                        printRecSpeed(tmpReadLen);
                        int packageLen = genInt(bufLen);
                        if(packageLen > MAX_DATA_LEN){
                            Log.e(TAG, "run: socket datalen:"+packageLen+", too big > "+MAX_DATA_LEN);
                            mSoc.close();
                            break;
                        }
                        int payloadLen = packageLen-1;
                        byte type = (byte) ins.read();
                        printRecSpeed(1);
                        byte[] payload = new byte[payloadLen];
                        tmpReadLen = 0;
                        int totalReadLen = 0;
                        while ((tmpReadLen = ins.read(payload,totalReadLen,payloadLen-totalReadLen))>0){
                            totalReadLen += tmpReadLen;
                            printRecSpeed(tmpReadLen);
                            if(totalReadLen == payloadLen){
                                break;
                            }
                        }

                        // deal data
                        if(type == DATA_TYPE_PKG_SINGLE ){
                            onGotSingleBytes(payload,payloadLen,mOus);
                        }else if(type == DATA_TYPE_PKG_RANGE_FIRST ||
                                type == DATA_TYPE_PKG_RANGE_MIDDLE ||
                                type == DATA_TYPE_PKG_RANGE_END
                            ){
                            onGotFlowingData(DATA_TYPE_PKG_RANGE_FIRST,payload,mOus);
                        }
                    }

                }catch (Exception e){
                    Log.i(TAG, "dealClient e: "+e.getMessage());
                    e.printStackTrace();
                }finally {
                    // rm
                    mCb.onSocClosed(mSoc,mOus);
                }
            }
        }.start();

    }


    public static int genInt(byte[] data){
        if(data == null || data.length!=4){
            throw new RuntimeException(" genInt param Err! data:"+data);
        }
        int res = 0;
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        return byteBuffer.getInt();
    }

    public static long genLong(byte[] data){
        if(data == null || data.length!=8){
            throw new RuntimeException(" genLong param Err! data:"+data);
        }
        int res = 0;
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        return byteBuffer.getLong();
    }



    public static byte[] toBytes(int value){
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.putInt(value);
        byte[] res = byteBuffer.array();
//        Log.i(TAG," toBytes intValue: "+value+", res: "+ Arrays.toString(res));
        return byteBuffer.array();
    }
    public static byte[] toBytes(long value){
        ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.putLong(value);
        byte[] res = byteBuffer.array();
//        Log.i(TAG," toBytes intValue: "+value+", res: "+ Arrays.toString(res));
        return byteBuffer.array();
    }

    /**
     *
     * @param type 1-2-2-2-3
     * @param data
     * @param ous
     */
    protected void onGotFlowingData(byte type, byte[] data, OutputStream ous){
//        if(mCb != null){
//            mCb.onGotVideoMsg(type,data,len,ous);
//        }
    }

    protected void onGotSingleBytes(byte[] data, int payloadLen, OutputStream ous){
        // switch channel
        byte channelId = data[0];
//        Log.i(TAG, "onGotSingleBytes: channelId: "+channelId);
        if(mCb == null){
            Log.e(TAG, "onGotSingleBytes: mcb is null, return!");
            return;
        }
        if(channelId == Beans.Channel.Command.ordinal()){
            try {
                String tmpStr = new String(Arrays.copyOfRange(data,1,data.length),StandardCharsets.UTF_8.name());
                Beans.CommandMsg commandMsg = GsonUtils.parse(tmpStr, Beans.CommandMsg.class);
                mCb.onGotJsonMsg(commandMsg,ous);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }else if(channelId == Beans.Channel.Video.ordinal()){
            mCb.onGotVideoMsg(new Beans.VideoData(data,1,payloadLen),ous);
        }
    }




    // ======================= send ===========================================

    public static final byte DATA_TYPE_PKG_SINGLE = 0;
    public static final byte DATA_TYPE_PKG_RANGE_FIRST  = 1;
    public static final byte DATA_TYPE_PKG_RANGE_MIDDLE  = 2;
    public static final byte DATA_TYPE_PKG_RANGE_END  = 3;
    public static void sendCmdMsg(OutputStream ous, Beans.CommandMsg msg){
        Log.i(TAG, "sendControlMsg() called with: ous = [" + ous + "], msg = [" + msg + "]");
        String tmp = GsonUtils.toJson(msg);
        Log.i(TAG, "sendControlMsg() json: "+tmp);
        GsonUtils.parse(tmp, Beans.CommandMsg.class);
        try {
            byte[] data = tmp.getBytes(StandardCharsets.UTF_8);
            byte[] dest = packageSingleMsg(Beans.Channel.Command.ordinal(), data);
            Log.i(TAG," sendData 0-10: "+Arrays.toString(Arrays.copyOf(dest,10)));
            printSendSpeed(dest.length);
            ous.write(dest);
        } catch (Exception e) {
            Log.e(TAG, "sendControlMsg()",e);
        }
    }

    /*
        [len 4] [packageType 1 ] [ payload ]
            payload : [channelId 0] [json]
                1. Channel 0 command
                   [channel ] [json-data]
            payload : [channelId 1] [head-len 4][head-payload-def] [ video-data ]
                1. Channel 1 video
                    [channel ] [head-len 4][head-payload-def][video-data]
                2. Head-payload-def: 非json, json解析耗时
                    [isCfg 1]     [isIframe 1]    [w 4]   [h 4]    [frameTime 8] ...
     */
    public static void sendVideoMsg(OutputStream ous, Beans.VideoData msg ){
//        Log.i(TAG, " ---------> sendVideoMsg called with: ous = [" + ous + "], msg = [" + msg + "]");
        try {
            byte[] dest = packageSingleMsg(Beans.Channel.Video.ordinal(),msg.toBytes());
            ous.write(dest);
            printSendSpeed(dest.length);
        } catch (Exception e) {
            Log.e(TAG, "sendControlMsg()",e);
        }
    }

    private static byte[] packageSingleMsg(int channelId, byte[] data){
        // [len][pkg-type][channel][data]
        byte[] dest = new byte[4+1+1+data.length];
        System.arraycopy(toBytes(dest.length - 4),0,dest,0,4);
        dest[4] = DATA_TYPE_PKG_SINGLE;
        dest[5] = (byte) channelId;
        System.arraycopy(data,0,dest,6,data.length);
        return dest;
    }

    private static long sendSpeed = 0;
    private static long sendLastSecond = 0;
    private static void printSendSpeed(int sendSize){
        sendSpeed += sendSize;
        if(System.currentTimeMillis()/1000 != sendLastSecond){
            Log.i(TAG, "printSendSpeed: "+(sendSpeed/1024)+", kB/s ");
            sendSpeed = 0;
            sendLastSecond = System.currentTimeMillis()/1000;
        }
    }



    private static long recSpeed = 0;
    private static long recLastSecond = 0;
    private static void printRecSpeed(int recSize){
        recSpeed += recSize;
        if(System.currentTimeMillis()/1000 != recLastSecond){
            Log.i(TAG, "printSendSpeed: "+(recSpeed/1024)+", kB/s ");
            recSpeed = 0;
            recLastSecond = System.currentTimeMillis()/1000;
        }
    }


}
