package com.thunder.lantransf.client.video;

import android.content.Context;
import android.view.Surface;

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
    void setStateChangeCallBack( IClientStateChangeCallBack cb);

    interface IClientStateChangeCallBack{
        void onRegFind();
        void onFindServer();
        void onServerConnected(String clientHost);
        void onServerDisConnected();
        void onVideoStart();
        void onVideoStop();
    }
}
