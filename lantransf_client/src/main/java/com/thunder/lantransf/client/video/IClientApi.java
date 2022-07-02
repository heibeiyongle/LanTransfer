package com.thunder.lantransf.client.video;

import android.content.Context;
import android.view.Surface;

import java.util.List;
import java.util.Map;

/**
 * Created by zhe on 2022/6/5 16:48
 *
 * @desc:
 */
public interface IClientApi {

    boolean init(Context context);
    boolean setConfig(Map config);
    boolean autoConnectServer();
    boolean startShow(Surface surface);
    boolean stopShow();

    void sendMsg(List<String> targets, String msg);
    // TODO
    void sendMsg(List<String> targets, byte[] msg);

    void setMsgHandler( IRecMsgHandler handler);
    interface IRecMsgHandler{
        void onGetMsg(String msg, String from);
        void onGetMsg(byte[] msg, String from);
    }


    void setStateChangeCallBack( IClientStateChangeCallBack cb);

    interface IClientStateChangeCallBack{
        void onRegFind();
        void onFindServer();
        void onServerConnected(String clientHost);
        void onGotClientInfo(String clientName);
        void onServerDisConnected();
        void onVideoStart();
        void onVideoStop();
    }
}
