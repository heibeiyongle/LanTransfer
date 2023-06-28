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

    boolean init(Context ctx, ClientApi.ClientConfig clientConfig);


    /**
     * 1. find server
     * 2. connect socket
     * 3. check surface send video-start
     * 4. start videoDecodeThread
     * 5. start msg-handler thread
     *
     */
    boolean toConnectServer();

    /**
     * stop connect
     * 1. break socket
     * 2. stop reg-find
     * 3. stop decodeThread
     * 4. stop msg Thread
      */
    boolean toDisConnectServer();

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

        /**
         * sdk 内部开始decodeThread
         * decode 依赖项
         * surface
         * VideoHead
         *
         * --------------
         *
         *
         *
          */

        void onServerConnected(String clientHost);

        void onGotClientInfo(String clientName);
        void onServerDisConnected();// sdk 内部停止decodeThread
        void onVideoStart();
        void onVideoStop();
        void onDebugInfo(String info);
    }
}
