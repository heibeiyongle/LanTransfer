package com.thunder.lantransf.client.video;

import android.content.Context;
import android.view.Surface;

import com.thunder.common.lib.dto.Beans;

public interface IMediaClient {

    void init(Context context);
    /**
     * connect to server
     */
    void connectServer();

    void startShow(Surface surface);
    void stopShow();

    void sendMsg(Beans.TransfPkgMsg msg);
    void setRecMsgHandler(IRecMsgHandler handler);

    interface IRecMsgHandler{
        void onGetMsg(Beans.TransfPkgMsg msg);
    }

    void setStateChangeCallBack(IStateChangeCallBack cb);

    interface IStateChangeCallBack{
        void onStartServiceListener();
        void onFindServer();
        void onServerConnected(String clientHost);
        void onGotClientInfo(String clientName);
        void onServerDisConnected();
        void onVideoStart();
        void onVideoStop();
        void onPlayStateChanged(boolean play);
        void onAccStateChanged(int type);
    }


}
