package com.thunder.lantransf.client.video;

import android.content.Context;

import com.thunder.common.lib.dto.Beans;


public interface ITransferClient {

    void init(Context context);

    void startToConnectServer();
    void disconnectServer();

    void sendViewActive();
    void sendViewInActive();
    void syncNetTime();
    void reportClientInfo(long netDelay);
    void updateLocalClientName(String clientName);

    void sendMsg(Beans.TransfPkgMsg msg);

    void setClientDataHandler(IClientDataHandler cb);
    void setClientStateHandler(IClientStateHandler cb);

    interface IClientDataHandler{
        void onGotVideoData(Beans.VideoData data);
        void onGotCmdData(Beans.TransfPkgMsg data);
    }

    interface IClientStateHandler{
        void onRegFindService();
        void onFindServerService();
        void onGotServerInfo();
        void onConnect(String clientHost);
        void onDisconnect();
    }

}
