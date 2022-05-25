package com.thunder.lantransfserver.server.video;

import android.util.Log;

import com.google.gson.internal.LinkedTreeMap;
import com.thunder.lantransfserver.server.dto.Beans;
import com.thunder.lantransfserver.util.GsonUtils;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MsgDealer implements ITransfServer.IClientMsgDealer, IMsgSender {

    ITransfServer mPublisher;
    public MsgDealer(){
//        PlayerStateManager.getInstance().addPlayerStateChangedListener(mPlayerStateChangedListener);
    }

    /**
     * 处理的消息：
     *  sync time msg
     *  client Report msg
     *  play control msg
     *
     */


    @Override
    public void onGotCmd(Beans.CommandMsg msg, TransferServer.ClientSession session) {
        if(msg != null && session != null){
            dealCmdMsg(msg,session);
        }
    }

    @Override
    public void setPublisher(ITransfServer publisher) {
        mPublisher = publisher;
        mOutQue = mPublisher.getOutputQue();
    }
    private ArrayBlockingQueue<Object> mOutQue;

    @Override
    public void sendCmd(Beans.CommandMsg msg) {
        if(mOutQue != null){
            try {
                mOutQue.offer(msg,100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static final String TAG = "MsgDealer";
    private void dealCmdMsg(Beans.CommandMsg msg, TransferServer.ClientSession session){
        Log.d(TAG, "dealCmdMsg() called with: msg = [" + msg + "], session = [" + session + "]");
        // switch
        String msgType = msg.getType();
        if(Beans.CommandMsg.VideoChannelState.class.getSimpleName().equals(msgType)){
            Beans.CommandMsg.VideoChannelState chatMsg = GsonUtils.parseFromLinkedTreeMap(
                    (LinkedTreeMap) msg.getBody(), Beans.CommandMsg.VideoChannelState.class);
            if(chatMsg != null){
                session.isActive = chatMsg.active;
                session.isSendCfg = false;
            }
        }else if(Beans.CommandMsg.ReqSyncTime.class.getSimpleName().equals(msgType)){
            Beans.CommandMsg.ReqSyncTime tmpMsg = GsonUtils.parseFromLinkedTreeMap(
                    (LinkedTreeMap) msg.getBody(), Beans.CommandMsg.ReqSyncTime.class);
            Beans.CommandMsg.ResSyncTime res = new Beans.CommandMsg.ResSyncTime();
            res.serverTimeMs = System.currentTimeMillis();
            res.req = tmpMsg;
            sendObjectCmd(res,session.clientId);
        }else if(Beans.CommandMsg.ReqReportClientInfo.class.getSimpleName().equals(msgType)){
            Beans.CommandMsg.ReqReportClientInfo tmpMsg = GsonUtils.parseFromLinkedTreeMap(
                    (LinkedTreeMap) msg.getBody(), Beans.CommandMsg.ReqReportClientInfo.class);
            mPublisher.updateClientSessionInfo(session.mOus,tmpMsg.clientName,tmpMsg.netDelay);
        }else if(Beans.CommandMsg.ReqCommon.class.getSimpleName().equals(msgType)){
            Beans.CommandMsg.ReqCommon tmpMsg = GsonUtils.parseFromLinkedTreeMap(
                    (LinkedTreeMap) msg.getBody(), Beans.CommandMsg.ReqCommon.class);

//            if(Beans.CommandMsg.ReqCommon.Type.getPlayState.name().equals(tmpMsg.type)){
//                // getState
//                Beans.CommandMsg.ResPlayState res = new Beans.CommandMsg.ResPlayState();
//                res.playing = !ServiceManager.getSongOrderService().isPaused();
//                sendObjectCmd(res,session.clientId);
//            }else if(Beans.CommandMsg.ReqCommon.Type.getAccState.name().equals(tmpMsg.type)){
//                Beans.CommandMsg.ResAccState res = new Beans.CommandMsg.ResAccState();
//                res.accType = ServiceManager.getSongOrderService().isAcc()? 1:0;
//                sendObjectCmd(res,session.clientId);
//            }
        }else if(Beans.CommandMsg.ReqUserClickAction.class.getSimpleName().equals(msgType)){
            Beans.CommandMsg.ReqUserClickAction tmpMsg = GsonUtils.parseFromLinkedTreeMap(
                    (LinkedTreeMap) msg.getBody(), Beans.CommandMsg.ReqUserClickAction.class);
            // perform
            if(Beans.CommandMsg.ReqUserClickAction.Type.PLAY_BTN.name().equals(tmpMsg.clickType)){
                onPlayBtnClick();
            }else if(Beans.CommandMsg.ReqUserClickAction.Type.PLAY_BTN.name().equals(tmpMsg.clickType)){
                onAccBtnClick();
            }
        }
        Log.d(TAG, "dealCmdMsg() end! ");
    }



    private void sendObjectCmd(Object msg,int target) {
        if(msg instanceof Beans.CommandMsg) throw new RuntimeException(" sendObjectCmd msg should not be CommandMsg.class");
        Beans.CommandMsg destMsg = Beans.CommandMsg.Builder.genP2PMsg(msg,target);
        sendCmd(destMsg);
    }

//    PlayerStateManager.PlayerStateChangedListener mPlayerStateChangedListener = new PlayerStateManager.PlayerStateChangedListener() {
//        @Override
//        public void onPauseChanged(boolean paused) {
//            Log.i(TAG,"onPauseChanged paused: "+paused);
//            Beans.CommandMsg.ResPlayState res = new Beans.CommandMsg.ResPlayState();
//            res.playing = !paused;
//            Beans.CommandMsg destMsg = Beans.CommandMsg.Builder.genBroadCastMsg(res);
//            sendCmd(destMsg);
//        }
//
//        @Override
//        public void onTrackChanged(TrackType trackType) {
//            Log.i(TAG, "onTrackChanged: trackType: "+trackType);
//            Beans.CommandMsg.ResAccState res = new Beans.CommandMsg.ResAccState();
//            res.accType = trackType.ordinal();
//            Beans.CommandMsg destMsg = Beans.CommandMsg.Builder.genBroadCastMsg(res);
//            sendCmd(destMsg);
//        }
//    };

    private void onPlayBtnClick(){
//        Observable.just("onPlayBtnClick")
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(s -> {
//                    PlayOperate.performPlayPause(false, null);
//                });
    }

    private void onAccBtnClick(){
//        Observable.just("onAccBtnClick")
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(s -> {
//                    ServiceManager.getSongOrderService().toggleTrack();
//                });
    }
}
