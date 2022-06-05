package com.thunder.lantransf.client.video;

import android.content.Context;

import com.thunder.common.lib.dto.Beans;


public interface ITransferClient {

    void init(Context context);

    void connectServer();
    void disconnectServer();

    void sendViewActive();
    void sendViewInActive();
    void sendPlayBtnClick();
    void sendAccBtnClick();
    void syncNetTime();
    void reportClientInfo(String clientName, long netDelay);
    void getPlayState();
    void getAccState();

    void setClientDataHandler(IClientDataHandler cb);
    void setClientStateHandler(IClientStateHandler cb);

    interface IClientDataHandler{
        void onGotVideoData(Beans.VideoData data);
        void onGotCmdData(Beans.CommandMsg data);
    }

    interface IClientStateHandler{
        void onRegFindService();
        void onFindServerService();
        void onGotServerInfo();
        void onConnect(String clientHost);
        void onDisconnect();
    }

}
