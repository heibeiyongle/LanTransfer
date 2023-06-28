package com.thunder.common.lib.codec;

import android.util.Log;

import com.thunder.common.lib.dto.Beans;
import com.thunder.common.lib.util.GsonUtils;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

/**
 * Created by zhe on 2022/6/20 22:20
 *
 * @desc:
 */
public class ByteBufferCodecImpl implements IByteBufferDealer{

    public static final long HEAD_TAG =0xff01aa02;


    private int getPkgLen(int payloadLen){
        return 8 + 4 + 3+ payloadLen + 1;
    }

    /**
     *
     * @param readBuf
     * @return msg-obj / new-size-buffer
     */
    @Override
    public Object decodeReadBuf(ByteBuffer readBuf) {
        // [tag-flag-8 0xff01aa02 ][len-4][ver-1][pkg-type-1][channel-1][data-N][sum-x]
        // check is-enough
        int lastPos = readBuf.position();
        readBuf.flip();
        if(readBuf.remaining() <= (8+4)){
            setReadContinue(readBuf,lastPos);
            return null;
        }
        long tag = readBuf.getLong();
        int len = readBuf.getInt();
        if(len + 8 > readBuf.capacity()){
            // upsize
            ByteBuffer newBuf = ByteBuffer.allocate((len+8)*2);
            readBuf.rewind();
            newBuf.put(readBuf);
            return newBuf;
        }
        if(readBuf.remaining() < len){
            //enough
            setReadContinue(readBuf,lastPos);
            return null;
        }
        byte ver = readBuf.get();
        byte pkgType = readBuf.get();
        byte channel = readBuf.get();
        byte[] payload = new byte[len-3-1];
        readBuf.get(payload);
        byte checkSum = readBuf.get();
        Object res = null;
        if(ver == 0){
            if(channel == Beans.Channel.Command.ordinal()){
                res = decodePayLoadCmdVer_0(payload);
            }else if(channel == Beans.Channel.Video.ordinal()){
                res = decodePayLoadVideoVer_0(payload);
            }
        }
        readBuf.compact();
        return res;
    }


    private Beans.TransfPkgMsg decodePayLoadCmdVer_0(byte[] data ){
        Beans.TransfPkgMsg transfPkgMsg = null;
        try {
//            Log.i(TAG, "decodePayLoadCmdVer_0 -1 : data.size: "+data.length);
            String tmpStr = new String(Arrays.copyOfRange(data,0,data.length), StandardCharsets.UTF_8.name());
//            Log.i(TAG, "decodePayLoadCmdVer_0: tmpStr: "+tmpStr);
            transfPkgMsg = GsonUtils.parse(tmpStr, Beans.TransfPkgMsg.class);
            transfPkgMsg.onDecode();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return transfPkgMsg;
    }

    private static final String TAG = "ByteBufferCodecImpl";
    private ByteBuffer encodePayloadCmdVer_0(Beans.TransfPkgMsg msg, ByteBuffer buf){
        byte[] data = null;
        String tmp = GsonUtils.toJson(msg);
//        Log.i(TAG, "encodePayloadCmdVer_0: tmp: "+tmp);
        try {
            data = tmp.getBytes(StandardCharsets.UTF_8);
//            Log.i(TAG, "encodePayloadCmdVer_0: data.length: "+data.length);
            buf = packPayload(buf,data,0,Beans.Channel.Command.ordinal());
        }catch (Exception e){
            e.printStackTrace();
        }
        return buf;
    }


    private Beans.VideoData decodePayLoadVideoVer_0(byte[] payload){
        return new Beans.VideoData(payload,0, payload.length);
    }

    private ByteBuffer encodePayLoadVideoVer_0(Beans.VideoData videoData, ByteBuffer buf){
        byte[] payload = videoData.toBytes();
        return packPayload(buf,payload,0, Beans.Channel.Video.ordinal());
    }

    private ByteBuffer packPayload(ByteBuffer buf, byte[] payload, int ver, int channel){
        int destBufSize = getPkgLen(payload.length);
        if(destBufSize > buf.capacity()){
            buf = ByteBuffer.allocate(destBufSize*2);
        }
        buf.putLong(HEAD_TAG);
        buf.putInt(destBufSize - 8 - 4);
        buf.put((byte)ver);
        buf.put((byte) 0);
        buf.put((byte)channel);
        buf.put(payload);
        byte checkSum = 0; // mock
        buf.put(checkSum);
        return buf;
    }




    private void setReadContinue(ByteBuffer byteBuffer, int originPos){
        byteBuffer.position(originPos); // reset pos
        byteBuffer.limit(byteBuffer.capacity()); // reset limit
    }


    @Override
    public ByteBuffer encodeWriteBuf(BlockingQueue<Object> msgs /*non-block*/, ByteBuffer writeBuf) {
        // res custom one msg
        if(writeBuf.remaining() > 0){
            return writeBuf;
        }

        if(msgs.size() == 0){
            return writeBuf;
        }
        writeBuf.clear();
        try {
            Object msg = msgs.poll();
            if(msg instanceof Beans.TransfPkgMsg){
                ((Beans.TransfPkgMsg) msg).onEncode();
                writeBuf = encodePayloadCmdVer_0((Beans.TransfPkgMsg) msg,writeBuf);
            }else if( msg instanceof Beans.VideoData){
                writeBuf = encodePayLoadVideoVer_0((Beans.VideoData) msg,writeBuf);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        writeBuf.flip();
        return writeBuf;
    }

}
