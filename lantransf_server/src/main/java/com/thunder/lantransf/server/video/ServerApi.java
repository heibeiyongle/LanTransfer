package com.thunder.lantransf.server.video;

import android.content.Context;
import android.view.Surface;

import java.util.Map;

public class ServerApi implements IServerApi {
    private ServerApi(){}

    public static ServerApi getInstance(){
        return ServerApi.Holder.instance;
    }
    private static class Holder{
        static ServerApi instance = new ServerApi();
    }

    @Override
    public boolean init(Context ctx) {
        ServerManager.getInstance().init(ctx);
        ServerManager.getInstance().setServerStateCB(mInnerServerStateCallBack);
        ServerManager.getInstance().setMediaServerStateCB(mInnerMediaStateCb);
        return true;
    }

    @Override
    public boolean setConfig(Map config) {
        return false;
    }

    @Override
    public void startServer() {
        ServerManager.getInstance().startServer();
    }

    @Override
    public Surface startPublishMedia() {
        ServerManager.getInstance().startPublishMedia();
        return ServerManager.getInstance().getCurrMediaServer().getCurrSUrface();
    }

    @Override
    public void stopPublishMedia() {
        ServerManager.getInstance().stopPublishMedia();
    }

    // for debug info
    @Override
    public String[] getClientList() {
        return (String[]) ServerManager.getInstance().getClientList().toArray();
    }

    @Override
    public void setStateChangeCallBack(IServerStateChangeCallBack cb) {
        mNotify = cb;
        /*
        interface IServerStateChangeCallBack{
            void onServerStateChanged(int state); // none started stoped
            void onMediaPublishStateChanged(int state); // none started stop
            void onGotClient(String clientName); //clientName server generate
            void onLossClient(String clientName);
        }
        */
    }

    IServerManager.IServerStateCallBack mInnerServerStateCallBack = new IServerManager.IServerStateCallBack() {
        @Override
        public void onSocketServerStarted() {
        }

        @Override
        public void onServicePublished() {
            if(mNotify != null){
                mNotify.onServerStateChanged(IServerStateChangeCallBack.ServerState.STARTED.ordinal());
            }
        }

        @Override
        public void onSocketServerStopped() {

        }

        @Override
        public void onServicePublishCanceled() {
            if(mNotify != null){
                mNotify.onServerStateChanged(IServerStateChangeCallBack.ServerState.STOPED.ordinal());
            }
        }

        @Override
        public void onGotClient(TransferServer.ClientSession client) {
            if (mNotify != null) {
                mNotify.onGotClient(client.toSampleString());
            }
        }

        @Override
        public void onLoseClient(TransferServer.ClientSession client) {
            if(mNotify != null){
                mNotify.onLossClient(client.toSampleString());
            }
        }
    };


    IServerManager.IMediaServerStateCallBack mInnerMediaStateCb = new IServerManager.IMediaServerStateCallBack() {
        @Override
        public void onFirstVFrameGenerated() {
            if(mNotify != null){
                mNotify.onMediaPublishStateChanged(IServerStateChangeCallBack.ServerMediaState.FIRST_FRAME_GENERATED.ordinal());
            }
        }

        @Override
        public void onFirstVFramePublished() {
            if(mNotify != null){
                mNotify.onMediaPublishStateChanged(IServerStateChangeCallBack.ServerMediaState.FIRST_FRAME_PUBLISHED.ordinal());
            }
        }

        @Override
        public void onServerReady() {
            if(mNotify != null){
                mNotify.onMediaPublishStateChanged(IServerStateChangeCallBack.ServerMediaState.STARTED.ordinal());
            }
        }

        @Override
        public void onServerStopped() {
            if(mNotify != null){
                mNotify.onMediaPublishStateChanged(IServerStateChangeCallBack.ServerMediaState.STOPPED.ordinal());
            }
        }
    };



    IServerStateChangeCallBack mNotify;
}
