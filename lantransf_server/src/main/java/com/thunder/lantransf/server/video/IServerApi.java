package com.thunder.lantransf.server.video;

import android.content.Context;
import android.view.Surface;

import java.util.List;
import java.util.Map;

public interface IServerApi {

    boolean init(Context ctx);
    boolean setConfig(Map config);
    void startServer();
    Surface startPublishMedia(); // 返回 sink surface ,业务方向此surface渲染画面
    void stopPublishMedia();
    String[] getClientList();

    // msg logic
    /*
    1. send notice
    2. deal query -> return response
    3. route msg  A -> S -> B

    4. msg content
        1. json format
        2. user def serial / on Got byte[] , { msgType, content }

    5. msg struct type
        Object T
        list<Object> T[]

    6. play control wrapper
        实现播控的通用诉求
         action -> server: play pause next replay acc/org
         server play state notice : idle, ready, running, pause , complete
         server state query handler : return server player-state

    7. 产出一个demo
        server -> show mv
        client -> control player
        server handle req , send res 



common-def

Msg{
    int msgId;
    int refMsgId;
    String msgType,
    byte[] payload,
}

transfPackage {
    string from,
    String[] to,
    Msg msg,
}

// 根据from to 做消息路由


common-util
1. String msgToStr(Object msg) // step one, json
2. byte[] msgToByte(Object msg) // step two, protocol buffer

msg-transf-api
3. sendMsg(List<String> clients, String msg)
4. sendMsg(List<String> clients, byte[] data)
5. void setMsgDealer(IMsgHandler handler)
6. interface ImsgHandler{
        void onGetMsg(Msg msg);
    }


     */

    void sendMsg(List<String> targets, String msg);
    // TODO
    void sendMsg(List<String> targets, byte[] msg);

    void setMsgHandler( IRecMsgHandler handler);
    interface IRecMsgHandler{
        void onGetMsg(String msg, String from);
        void onGetMsg(byte[] msg, String from);
    }


    void setStateChangeCallBack( IServerStateChangeCallBack cb);

    interface IServerStateChangeCallBack{

        enum ServerState{
            NONE,
            STARTED,
            STOPPED
        }

        enum ServerMediaState{
            NONE,
            STARTED,
            FIRST_FRAME_GENERATED,
            FIRST_FRAME_PUBLISHED,
            STOPPED
        }

        void onServerStateChanged(int state); // none started stoped
        void onMediaPublishStateChanged(int state); // none started stop
        void onGotClient(String clientName); //clientName server generate
        void onLossClient(String clientName);
    }


}
