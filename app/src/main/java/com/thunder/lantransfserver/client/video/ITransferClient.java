package com.thunder.lantransfserver.client.video;

import android.content.Context;

import com.thunder.lantransfserver.client.dto.Beans;


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

    void setClientHandler(IClientHandler cb);

    interface IClientHandler{
        void onConnect();
        void onDisconnect();
        void onGotVideoData(Beans.VideoData data);
        void onGotCmdData(Beans.CommandMsg data);
    }
}
