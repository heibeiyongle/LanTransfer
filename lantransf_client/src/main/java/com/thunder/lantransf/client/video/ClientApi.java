package com.thunder.lantransf.client.video;

import android.content.Context;
import android.util.Log;
import android.view.Surface;

import com.thunder.common.lib.dto.Beans;
import com.thunder.lantransf.client.state.ClientStateManager;

import java.util.HashSet;
import java.util.List;

/**
 * Created by zhe on 2022/6/5 16:50
 *
 * @desc:
 */
public class ClientApi implements IClientApi{
    private static final String TAG = "ClientApi";

//    public ClientApi(String tag){
//        Log.d(TAG, "ClientApi() called with: tag = [" + tag + "]");
//    }

    private boolean mIsAutoReconnect = false;

    private ClientApi(){}
    static class Holder{
        static ClientApi instance = new ClientApi();
    }
    public static ClientApi getInstance(){
        return ClientApi.Holder.instance;
    }

    boolean mInited = false;
    MediaClient mInnerClient;
    ClientConfig mClientConfig;
    boolean mIsStartConnected = false;
    @Override
    public boolean init(Context ctx, ClientConfig clientConfig) {
        Log.i(TAG, "init: ");
        mInnerClient = MediaClient.getInstance();
        mInnerClient.init(ctx);
        mInnerClient.setStateChangeCallBack(mInnerCb);
        mInited = true;
        if(clientConfig != null){
            mClientConfig = clientConfig;
            mIsAutoReconnect = mClientConfig.isAutoReconnect();
        }
        if(mIsAutoReconnect){
            mInnerClient.startToConnectServer();
        }
        return true;
    }


    @Override
    public boolean toConnectServer() {
        // todo 防止重复调用
        Log.i(TAG, "toConnectServer mIsStartConnected： "+mIsStartConnected+", mInited: "+mInited);
        if(!mInited){
            Log.e(TAG, "toConnectServer: please init first !");
            return false;
        }
        if(mIsStartConnected){
            Log.e(TAG, "toConnectServer: return! for mIsStartConnected = true!");
            return false;
        }
        mIsStartConnected = true;
        if(mClientConfig != null){
            mIsAutoReconnect = mClientConfig.isAutoReconnect();
        }
        mInnerClient.startToConnectServer();
        return true;
    }

    @Override
    public boolean toDisConnectServer() {
        if(!mInited){
            Log.i(TAG, "toDisConnectServer: please init first !");
            return false;
        }
        mIsStartConnected = false;
        mIsAutoReconnect = false;
        mInnerClient.stopConnect();
        return false;
    }

    private boolean isVideoShown = false;
    @Override
    public boolean startShow(Surface surface) {
        Log.i(TAG, "startShow: isVideoShown: "+isVideoShown);
        if(isVideoShown){
            return false;
        }
        isVideoShown = true;
        mInnerClient.startShow(surface);
        return true;
    }

    @Override
    public boolean stopShow() {
        isVideoShown = false;
        mInnerClient.stopShow();
        return true;
    }

    @Override
    public void sendMsg(List<String> targets, String msg) {
        HashSet<String> destTarget = null;
        if(targets != null && targets.size() > 0){
            destTarget = new HashSet<>();
            destTarget.addAll(targets);
        }
        mInnerClient.sendMsg(Beans.TransfPkgMsg.Builder.genSpecTargetsMsg(msg,destTarget,0,
                ClientStateManager.getInstance().getNetInfo().getCurrNetTimeMs()));
    }

    @Override
    public void sendMsg(List<String> targets, byte[] msg) {
        //todo
    }

    IRecMsgHandler mOutMsgHandler = null;
    @Override
    public void setMsgHandler(IRecMsgHandler handler) {
        mOutMsgHandler = handler;
        mInnerClient.setRecMsgHandler(mTmpRecMsgHandler);
    }

    IMediaClient.IRecMsgHandler mTmpRecMsgHandler = new IMediaClient.IRecMsgHandler() {
        @Override
        public void onGetMsg(Beans.TransfPkgMsg msg) {
            if(mOutMsgHandler != null){
                if(msg.isOriginPayloadBytes()){
                    return; //todo
                }
                mOutMsgHandler.onGetMsg(msg.getStrPayload(), "xxx"); // todo spec xxx
            }
        }
    };



    IClientStateChangeCallBack mNotify;
    @Override
    public void setStateChangeCallBack(IClientStateChangeCallBack cb) {
        mNotify = cb;
    }

    IMediaClient.IStateChangeCallBack mInnerCb = new IMediaClient.IStateChangeCallBack() {
        @Override
        public void onStartServiceListener() {
            Log.i(TAG, "onStartServiceListener: ");
            if(mNotify != null){
                mNotify.onRegFind();
            }
        }

        @Override
        public void onFindServer() {
            Log.i(TAG, "onFindServer: mIsAutoReconnect: "+mIsAutoReconnect);
            if(mNotify != null){
                mNotify.onFindServer();
                if(mIsAutoReconnect){
                    toConnectServer();
                }
            }
        }

        @Override
        public void onServerConnected(String remoteHost) {
            Log.i(TAG, "onServerConnected: ");
            if(mNotify != null){
                mNotify.onServerConnected(remoteHost);
            }
        }

        @Override
        public void onGotClientInfo(String clientName) {
            Log.i(TAG, "onGotClientInfo: ");
            if(mNotify != null){
                mNotify.onGotClientInfo(clientName);
            }
        }

        @Override
        public void onServerDisConnected() {
            Log.i(TAG, "onServerDisConnected: mIsAutoReconnect: "+mIsAutoReconnect);
            if(mNotify != null){
                mNotify.onServerDisConnected();
            }
            if(mIsAutoReconnect){
                // reConnect
                new Thread(){
                    @Override
                    public void run() {
                        super.run();
                        try {
                            int delayMs = 1000*2;
                            Log.i(TAG, "onServerDisConnected delayMs: "+delayMs+" to reConnect!");
                            Thread.sleep(delayMs);
                            Log.i(TAG, "onServerDisConnected begin to reConnect! ");
                            mInnerClient.startToConnectServer();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        }

        @Override
        public void onVideoStart() {
            Log.i(TAG, "onVideoStart: ");
            if(mNotify != null){
                mNotify.onVideoStart();
            }
        }

        @Override
        public void onVideoStop() {
            Log.i(TAG, "onVideoStop: ");
            if(mNotify != null){
                mNotify.onVideoStop();
            }
        }

        @Override
        public void debugInfo(String info) {
            Log.i(TAG, "debugInfo: info: "+info);
            if(mNotify != null){
                mNotify.onDebugInfo(info);
            }
        }

        @Override
        public void onPlayStateChanged(boolean play) {
            Log.i(TAG, "onPlayStateChanged: ");
        }

        @Override
        public void onAccStateChanged(int type) {
            Log.i(TAG, "onAccStateChanged: ");
        }
    };

    public static class ClientConfig{
        private boolean mIsReconnect = false;
        private ClientConfig(Builder builder){
            this.mIsReconnect = builder.mIsReConnect;
        }

        public boolean isAutoReconnect(){
            return mIsReconnect;
        }

        public static class Builder{
            private boolean mIsReConnect = false;
            public Builder setAutoReconnect(boolean reconnect){
                mIsReConnect = reconnect;
                return this;
            }

            public ClientConfig build(){
                return new ClientConfig(this);
            }
        }
    }

}
