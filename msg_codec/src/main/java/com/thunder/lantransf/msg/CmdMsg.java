package com.thunder.lantransf.msg;

public class CmdMsg {

    public static class ReqCommon{
        public enum Type{
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