package com.thunder.lantransf.server.video;

import android.content.Context;
import android.util.Log;
import android.view.Surface;

import com.thunder.common.lib.dto.Beans;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
        String[] dest = new String[ServerManager.getInstance().getClientList().size()];
        return ServerManager.getInstance().getClientList().toArray(dest);
    }

    private static final String TAG = "ServerApi";
    @Override
    public void sendMsg(List<String> targets, String msg) {
        HashSet<String> destTarget = null;
        if(targets != null && targets.size() > 0){
            destTarget = new HashSet<String>();
            destTarget.addAll(targets);
        }
        if(ServerManager.getInstance().mMsgDealer == null){
            Log.e(TAG, "sendMsg(), mMsgDealer NOT-INIT, return ! ");
            return;
        }
        ServerManager.getInstance().mMsgDealer.sendCmd(
                Beans.TransfPkgMsg.Builder.genSpecTargetsMsg(msg,destTarget,0,System.currentTimeMillis()));
    }

    @Override
    public void sendMsg(List<String> targets, byte[] msg) {
        //todo
    }

    IRecMsgHandler mOuterMsgHandler = null;
    @Override
    public void setMsgHandler(IRecMsgHandler handler) {
        mOuterMsgHandler = handler;
        ServerManager.getInstance().setOutMsgHandler(mInnerOutMsgHandler);
    }

    IOuterMsgRec.IOutMsgHandler mInnerOutMsgHandler = new IOuterMsgRec.IOutMsgHandler() {
        @Override
        public void onGotMsg(String msg, String from) {
            if(mOuterMsgHandler != null){
                mOuterMsgHandler.onGetMsg(msg,from);
            }
        }
    };


    @Override
    public void setStateChangeCallBack(IServerStateChangeCallBack cb) {
        mNotify = cb;
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
                mNotify.onServerStateChanged(IServerStateChangeCallBack.ServerState.STOPPED.ordinal());
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
