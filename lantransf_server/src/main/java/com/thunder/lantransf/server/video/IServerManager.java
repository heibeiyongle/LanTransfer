package com.thunder.lantransf.server.video;

import android.content.Context;

import java.util.List;

interface IServerManager {

    void init(Context context);
    void startServer();
    void stopServer();
    void startPublishMedia();
    void stopPublishMedia();
    List<String> getClientList();

    void publishPlayState(boolean play);
    void publishAccState(int acc);

    void setServerStateCB(IServerStateCallBack cb);
    void setMediaServerStateCB(IMediaServerStateCallBack cb);

    interface IServerStateCallBack{
        void onSocketServerStarted();
        void onServicePublished();
        void onSocketServerStopped();
        void onServicePublishCanceled();
        void onGotClient(TransferServer.ClientSession client);
        void onLoseClient(TransferServer.ClientSession client);
    }

    interface IMediaServerStateCallBack{
        void onServerReady();
        void onFirstVFrameGenerated(); // got first Frame
        void onFirstVFramePublished(); // publish Fv
        void onServerStopped();
    }


}
