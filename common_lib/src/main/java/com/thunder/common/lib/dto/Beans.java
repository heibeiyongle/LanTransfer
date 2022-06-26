package com.thunder.common.lib.dto;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Keep;


import com.thunder.common.lib.transf.SocketDealer;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Beans {

    public enum Channel{
        Command,
        Video
    }

    public interface ITransferPkg{
        void setTargets(Set<String> targets);
        Set<String> getTargets();
    }

    public static class VideoData implements ITransferPkg{
        Set<String> targets;

        private int head_len = 4+1+1+4+4+8;
        public boolean isConfigFrame;
        public boolean keyFrame;
        public int w;
        public int h;
        public long frameTimeMs;
        public byte[] h264Data;
        private byte[] destBytes;


        public VideoData(boolean isConfigFrame, boolean isKeyFrame, int width, int height, byte[] videoData){
            this.isConfigFrame = isConfigFrame;
            keyFrame = isKeyFrame;
            w = width;
            h = height;
            h264Data = videoData;
        }

        /**
        bytes:
        DEF: [MSG-TYPE 4][KEY-FRAME 1][W 4][H 4][FRAME-VIDEO-DATA]
         */
        public VideoData(byte[] data,int offSet,int dataLen){
            isConfigFrame = data[4+offSet]==1;
            keyFrame = data[5+offSet]==1;
            w = SocketDealer.genInt(Arrays.copyOfRange(data,6+offSet,10+offSet));
            h = SocketDealer.genInt(Arrays.copyOfRange(data,10+offSet,14+offSet));
            frameTimeMs = SocketDealer.genLong(Arrays.copyOfRange(data,14+offSet,22+offSet));
            h264Data = Arrays.copyOfRange(data,head_len+offSet,dataLen);
        }

        @Override
        public Set<String> getTargets() {
            return targets;
        }

        public void setTargets(Set<String> targets){
            this.targets = targets;
        }

        public byte[] toBytes(){
            if(destBytes != null){
                return destBytes;
            }
            //[head-len 4][head-payload-def][video-data]
            destBytes = new byte[head_len+h264Data.length];
            System.arraycopy(SocketDealer.toBytes(head_len-4),0,destBytes,0,4);
            destBytes[4] = (byte) (isConfigFrame? 1:0);
            destBytes[5] = (byte) (keyFrame? 1:0);
            System.arraycopy(SocketDealer.toBytes(w),0,destBytes,6,4);
            System.arraycopy(SocketDealer.toBytes(h),0,destBytes,10,4);
            System.arraycopy(SocketDealer.toBytes(frameTimeMs),0,destBytes,14,4);
            System.arraycopy(h264Data,0,destBytes,head_len,h264Data.length);
            return destBytes;
        }

        public boolean isConfigFrame() {
            return isConfigFrame;
        }

        public byte[] getH264Data() {
            return h264Data;
        }

        @Override
        public String toString() {

            return "VideoData{" +
                    "keyFrame=" + keyFrame +
                    ", isConfigFrame=" + isConfigFrame +
                    ", w=" + w +
                    ", h=" + h +
                    ", h264Data len =" + (h264Data==null? "null" : h264Data.length) +
                    '}';
        }

    }

    public static class TransfPkgMsg implements ITransferPkg {
        // null 广播消息,except self.
        // [0] server handle
        // [x1], [x2] x1 x2
        private int srcType; // 0 outer, 1 inner
        /*
            server: null for broadCast, [x1,x2] for dest-x1 dest-x2
            client: x1, x2, s1 ...
         */
        public Set<String> targets;

        private int formatType; // 0 string 1 byte[]
        private byte[] payload;


        public static class Builder{
            public static TransfPkgMsg genBroadCastMsg(String msg,int srcT){
                checkEmpty(msg);
                return new TransfPkgMsg(strToBytes(msg),0,srcT);
            }
            public static TransfPkgMsg genP2PMsg(String msg, String target, int srcT){
                checkEmpty(msg);
                return new TransfPkgMsg(target,strToBytes(msg),0,srcT);
            }
            public static TransfPkgMsg genSpecTargetsMsg(String msg, HashSet<String> targets, int srcT){
                checkEmpty(msg);
                return new TransfPkgMsg(targets,strToBytes(msg),0, srcT);
            }
        }

        private static void checkEmpty(String msg){
            if(TextUtils.isEmpty(msg)){
                throw new RuntimeException("checkEmpty msg should not empty! msg:["+msg+"]");
            }
        }

        private static final String TAG = "TransfPkgMsg";
        private static byte[] strToBytes(String src){
//            Log.d(TAG, "strToBytes() called with: src = [" + src + "]");
            if(TextUtils.isEmpty(src)){
                byte[] dest = new byte[0];
                return dest;
            }
            byte[] dest = src.getBytes(StandardCharsets.UTF_8);
            return dest;
        }

        public String getStrPayload(){
            String tmpStr = "";
            try {
                tmpStr = new String(payload,StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return tmpStr;
        }

        @Override
        public void setTargets(Set<String> targets) {
            this.targets = targets;
        }

        @Override
        public Set<String> getTargets() {
            return targets;
        }

        @Keep
        private TransfPkgMsg(byte[] data, int originFormat, int srcT) {
            this.srcType = srcT;
            this.payload = data;
            this.formatType = originFormat;
        }

        private TransfPkgMsg(String target, byte[] data, int originFormat, int srcT) {
            this.srcType = srcT;
            this.targets = new HashSet<>();
            this.targets.add(target);
            this.payload = data;
            this.formatType = originFormat;
        }

        private TransfPkgMsg(HashSet<String> targets, byte[] data, int originFormat, int srcT) {
            this.srcType = srcT;
            if(targets != null && targets.size() > 0){
                this.targets = targets;
            }
            this.payload = data;
            this.formatType = originFormat;
        }

        @Override
        public String toString() {
            return "ControlMsg{" +
                    "formatBytes=" + (this.isOriginPayloadBytes()?" byteArray ":" String ") +
                    ", body=" + (isOriginPayloadBytes()? " byte-s " :getStrPayload() ) +
                    ", targets =" + this.targets +
                    '}';
        }

        public byte[] getPayload() {
            return payload;
        }

        public boolean isInnerCmdMsg(){
            return srcType == 1;
        }

        public boolean isOriginPayloadBytes(){
            return formatType == 1;
        }

        public static class VideoChannelState{
            public boolean active;

            @Override
            public String toString() {
                return "VideoChannelState{" +
                        "active=" + active +
                        '}';
            }
        }

        public static class ReqSyncTime{
            public long clientTimeMs;

            @Override
            public String toString() {
                return "ReqSyncTime{" +
                        "clientTimeMs=" + clientTimeMs +
                        '}';
            }
        }

        public static class ResSyncTime{
            public ReqSyncTime req;
            public long serverTimeMs;

            @Override
            public String toString() {
                return "ResSyncTime{" +
                        "req=" + req +
                        ", serverTimeMs=" + serverTimeMs +
                        '}';
            }
        }

        public static class ResClientInfo{
            public String clientName;

            @Override
            public String toString() {
                return "resClientInfo{" +
                        "clientName='" + clientName + '\'' +
                        '}';
            }
        }

        public static class ReqReportClientInfo{
            public String clientName;
            public long netDelay;

            @Override
            public String toString() {
                return "ReqReportClientInfo{" +
                        "clientName='" + clientName + '\'' +
                        ", netDelay=" + netDelay +
                        '}';
            }
        }

    }

}
