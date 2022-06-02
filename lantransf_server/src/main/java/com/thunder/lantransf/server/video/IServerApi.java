package com.thunder.lantransf.server.video;

import android.content.Context;
import android.view.Surface;

import java.util.Map;

public interface IServerApi {

    boolean init(Context ctx);
    boolean setConfig(Map config);
    void startServer();
    Surface startPublishMedia(); // 返回 sink surface ,业务方向此surface渲染画面
    void stopPublishMedia();
    String[] getClientList();
    void setStateChangeCallBack( IServerStateChangeCallBack cb);

    interface IServerStateChangeCallBack{
        void onServerStateChanged(int state); // none started stoped
        void onMediaPublishStateChanged(int state); // none started stop
        void onGotClient(String clientName); //clientName server generate
        void onLossClient(String clientName);
    }


}
