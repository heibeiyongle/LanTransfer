package com.thunder.lantransf.server.video;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;


class ServerManager implements IServerManager{
    private static final String TAG = "ServerManager";
    Context mCtx;

    private ServerManager(){}

    public static ServerManager getInstance(){
        return Holder.instance;
    }
    private static class Holder{
        static ServerManager instance = new ServerManager();
    }

    @Override
    public void init(Context context) {
        mCtx = context;
    }

    private MediaServer mMediaServer = null;
    private ITransfServer mTransfServer = null;

    public MediaServer getCurrMediaServer(){
        return mMediaServer;
    }

    ITransfServer.ITransfServerStateCallBack mTransfServerCB = new ITransfServer.ITransfServerStateCallBack() {
        @Override
        public void onSocketServerReady() {
            if(mServerStateCB != null){
                mServerStateCB.onSocketServerStarted();
            }
        }

        @Override
        public void onServicePublished() {
            if(mServerStateCB != null){
                mServerStateCB.onServicePublished();
            }
        }

        @Override
        public void onSocketServerStopped() {

        }

        @Override
        public void onServiceCanceled() {
            if(mServerStateCB != null){
                mServerStateCB.onServicePublishCanceled();
            }
        }

        @Override
        public void onGotClient(TransferServer.ClientSession clientSession) {
            if(mServerStateCB != null){{
                mServerStateCB.onGotClient(clientSession);
            }}
        }

        @Override
        public void onLoseClient(TransferServer.ClientSession clientSession) {
            if(mServerStateCB != null){
                mServerStateCB.onLoseClient(clientSession);
            }
        }
    };

    IMediaServer.IStateCallBack mInnerMediaServerCb = new IMediaServer.IStateCallBack() {
        @Override
        public void onGenerateFirstFrame() {
            if(mMediaStateNotify != null){
                mMediaStateNotify.onFirstVFrameGenerated();
            }
        }

        @Override
        public void onPublishFirstFrame() {
            if(mMediaStateNotify != null){
                mMediaStateNotify.onFirstVFramePublished();
            }
        }

        @Override
        public void onServerReady() {
            if(mMediaStateNotify != null){
                mMediaStateNotify.onServerReady();
            }
        }

        @Override
        public void onServerStopped() {
            if(mMediaStateNotify != null){
                mMediaStateNotify.onServerStopped();
            }
        }
    };


    @Override
    public void startServer() {
        if(mTransfServer != null){
            return;
        }
        if(mCtx == null){
            Log.e(TAG, "startServer: context is null, return! ");
            return;
        }
        mTransfServer = new TransferServer();
        mTransfServer.setStateCallBack(mTransfServerCB);
        mTransfServer.startTransfServer(mCtx);
//        mTransfServer.startPublishMsg();
        initClientMsgDealer();
    }

    @Override
    public void stopServer() {
        if(mTransfServer == null){
           return;
        }
        mTransfServer.stopServer();
    }

    @Override
    public void startPublishMedia() {
        if(mMediaServer != null){
            return;
        }
        mMediaServer = new MediaServer();
        mMediaServer.setStateCallBack(mInnerMediaServerCb);
        mMediaServer.startPublish(mCtx,mTransfServer.getOutputQue());
    }

    @Override
    public void stopPublishMedia() {
        if(mMediaServer == null){
            return;
        }
        mMediaServer.stopPublish();
    }

    @Override
    public List<String> getClientList() {
        List<String> res = new ArrayList<>();
        if(mTransfServer == null){
            return res;
        }
        List<String> tmpList = mTransfServer.getClientList();
        if(tmpList != null){
            res =tmpList;
        }
        return res;
    }

//    @Override
//    public void publishPlayState(boolean play) {
//        Beans.TransfPkgMsg.ResPlayState playState = new Beans.TransfPkgMsg.ResPlayState();
//        playState.playing = play;
//        mMsgDealer.sendCmd(genPublishMsg(playState));
//    }
//
//    @Override
//    public void publishAccState(int acc) {
//        Beans.TransfPkgMsg.ResAccState accState = new Beans.TransfPkgMsg.ResAccState();
//        accState.accType = acc;
//        mMsgDealer.sendCmd(genPublishMsg(accState));
//    }
//

    IServerStateCallBack mServerStateCB = null;
    @Override
    public void setServerStateCB(IServerStateCallBack cb) {
        mServerStateCB = cb;
    }

    IMediaServerStateCallBack mMediaStateNotify = null;
    @Override
    public void setMediaServerStateCB(IMediaServerStateCallBack cb) {
        mMediaStateNotify = cb;
    }

    MsgDealer mMsgDealer = new MsgDealer();
    private void initClientMsgDealer(){
        mMsgDealer.setPublisher(mTransfServer);
        mTransfServer.setMsgHandler(mMsgDealer);
    }

    IOuterMsgRec.IOutMsgHandler mOutHandler = null;
    public void setOutMsgHandler(IOuterMsgRec.IOutMsgHandler receiver){
        mOutHandler = receiver;
        mMsgDealer.setOutMsgHandler(mOutHandler);
    }

//    private Beans.TransfPkgMsg genPublishMsg(Object content){
//        if(content instanceof Beans.TransfPkgMsg)
//            throw new RuntimeException(" genPublishMsg param should not be CommandMsg.class !");
//        Beans.TransfPkgMsg destMsg = Beans.TransfPkgMsg.Builder.genBroadCastMsg(content);
//        return destMsg;
//    }



}
