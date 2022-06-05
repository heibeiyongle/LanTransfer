package com.thunder.lantransf.client.video;

import android.content.Context;
import android.util.Log;
import android.view.Surface;

import java.util.Map;

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

    private ClientApi(){}
    static class Holder{
        static ClientApi instance = new ClientApi();
    }
    public static ClientApi getInstance(){
        return ClientApi.Holder.instance;
    }

    boolean mInited = false;
    MediaClient mInnerClient;
    @Override
    public boolean init(Context ctx) {
        mInnerClient = MediaClient.getInstance();
//        mInnerClient = new MediaClient("test-only");
        mInnerClient.init(ctx);
        mInnerClient.setStateChangeCallBack(mInnerCb);
        mInited = true;
        return true;
    }

    @Override
    public boolean setConfig(Map config) {
        return true;
    }

    @Override
    public boolean autoConnectServer() {
        if(!mInited){
            Log.e(TAG, "autoConnectServer: please init first !");
            return false;
        }
        mInnerClient.connectServer();
        return true;
    }

    @Override
    public boolean startShow(Surface surface) {
        mInnerClient.startShow(surface);
        return true;
    }

    @Override
    public boolean stopShow() {
        mInnerClient.stopShow();
        return true;
    }

    IClientStateChangeCallBack mNotify;
    @Override
    public void setStateChangeCallBack(IClientStateChangeCallBack cb) {
        mNotify = cb;
    }

    IMediaClient.IStateChangeCallBack mInnerCb = new IMediaClient.IStateChangeCallBack() {
        @Override
        public void onStartServiceListener() {
            if(mNotify != null){
                mNotify.onRegFind();
            }
        }

        @Override
        public void onFindServer() {
            if(mNotify != null){
                mNotify.onFindServer();
            }
        }

        @Override
        public void onServerConnected() {
            if(mNotify != null){
                mNotify.onServerConnected();
            }
        }

        @Override
        public void onServerDisConnected() {
            if(mNotify != null){
                mNotify.onServerDisConnected();
            }
        }

        @Override
        public void onVideoStart() {
            if(mNotify != null){
                mNotify.onVideoStart();
            }
        }

        @Override
        public void onVideoStop() {
            if(mNotify != null){
                mNotify.onVideoStop();
            }
        }

        @Override
        public void onPlayStateChanged(boolean play) {

        }

        @Override
        public void onAccStateChanged(int type) {

        }
    };
}
