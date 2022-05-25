package com.thunder.lantransfserver.client.dto;

import androidx.annotation.Keep;


import com.thunder.lantransfserver.client.transf.SocketDealer;

import java.util.Arrays;
import java.util.HashSet;

public class Beans {

    public enum Channel{
        Command,
        Video
    }


    public static class VideoData{
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

    public static class CommandMsg {
        // null 广播消息,except self.
        // [0] server handle
        // [x1], [x2] x1 x2
        public HashSet<Integer> targets;
        private String type;
        private Object body;

        public static class Builder{
            public static CommandMsg genBroadCastMsg(Object content){
                return new CommandMsg(content);
            }
            public static CommandMsg genP2PMsg(Object content,int target){
                return new CommandMsg(content,target);
            }
            public static CommandMsg genSpecTargetsMsg(Object content,HashSet<Integer> targets){
                return new CommandMsg(content,targets);
            }
        }


        @Keep
        private CommandMsg(Object content) {
            if (content != null) {
                this.type = content.getClass().getSimpleName();
                this.body = content;
            }
        }
        private CommandMsg(Object content,int target) {
            this.targets = new HashSet<>();
            this.targets.add(target);
            if (content != null) {
                this.type = content.getClass().getSimpleName();
                this.body = content;
            }
        }

        private CommandMsg(Object content,HashSet<Integer> targets) {
            if (content != null) {
                this.targets = new HashSet<>(targets);
                this.type = content.getClass().getSimpleName();
                this.body = content;
            }
        }

        @Override
        public String toString() {
            return "ControlMsg{" +
                    "type='" + type + '\'' +
                    ", body=" + body +
                    '}';
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Object getBody() {
            return body;
        }

        public void setBody(Object body) {
            this.body = body;
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

        public static class ReqCommon{
            public static enum Type{
                getPlayState,
                getAccState,
            }
            public String type;

            @Override
            public String toString() {
                return "ReqCommon{" +
                        "type='" + type + '\'' +
                        '}';
            }
        }

        public static class ResPlayState{
            public boolean playing; // play / pause

            @Override
            public String toString() {
                return "ResPlayState{" +
                        "playing=" + playing +
                        '}';
            }
        }

        public static class ResAccState{
            public int accType; // 0/1

            @Override
            public String toString() {
                return "ResAccState{" +
                        "accType=" + accType +
                        '}';
            }
        }

        public static class ReqUserClickAction{
            public enum Type {
                PLAY_BTN,
                ACC_BTN,
            }
            public String clickType = "";

            @Override
            public String toString() {
                return "ReqUserClickAction{" +
                        "clickType=" + clickType +
                        '}';
            }
        }

    }

}
