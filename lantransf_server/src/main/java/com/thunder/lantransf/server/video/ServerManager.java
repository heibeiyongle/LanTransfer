package com.thunder.lantransf.server.video;

import android.content.Context;
import android.util.Log;

import com.thunder.common.lib.dto.Beans;


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
        mTransfServer.startTransfServer(mCtx);
        mTransfServer.startPublishMsg();
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
    public void publishPlayState(boolean play) {
        Beans.CommandMsg.ResPlayState playState = new Beans.CommandMsg.ResPlayState();
        playState.playing = play;
        mMsgDealer.sendCmd(genPublishMsg(playState));
    }

    @Override
    public void publishAccState(int acc) {
        Beans.CommandMsg.ResAccState accState = new Beans.CommandMsg.ResAccState();
        accState.accType = acc;
        mMsgDealer.sendCmd(genPublishMsg(accState));
    }

    MsgDealer mMsgDealer = new MsgDealer();
    private void initClientMsgDealer(){
        mMsgDealer.setPublisher(mTransfServer);
        mTransfServer.setMsgDealer(mMsgDealer);
    }

    private Beans.CommandMsg genPublishMsg(Object content){
        if(content instanceof Beans.CommandMsg)
            throw new RuntimeException(" genPublishMsg param should not be CommandMsg.class !");
        Beans.CommandMsg destMsg = Beans.CommandMsg.Builder.genBroadCastMsg(content);
        return destMsg;
    }



}
