package com.thunder.lantransf.server.video;

import android.util.Log;


import com.google.gson.internal.LinkedTreeMap;
import com.thunder.common.lib.dto.Beans;
import com.thunder.common.lib.util.GsonUtils;
import com.thunder.lantransf.msg.TransfMsgWrapper;
import com.thunder.lantransf.msg.codec.CodecUtil;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MsgDealer implements ITransfServer.IClientMsgDealer, IMsgSender,IOuterMsgRec {

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
    public void onGotCmd(Beans.TransfPkgMsg msg, TransferServer.ClientSession session) {
        if(msg != null && session != null){
            if(msg.isInnerCmdMsg()){
                // inner
                dealInnerCmdMsg(msg,session);
            }else {
                // outer
                dealOuterCmdMsg(msg,session);
            }
        }
    }

    @Override
    public void setPublisher(ITransfServer publisher) {
        mPublisher = publisher;
        mOutQue = mPublisher.getOutputQue();
    }
    private ArrayBlockingQueue<Object> mOutQue;

    @Override
    public void sendCmd(Beans.TransfPkgMsg msg) {
        if(mOutQue != null){
            try {
                mOutQue.offer(msg,100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    private static final String TAG = "MsgDealer";

    private void dealInnerCmdMsg(Beans.TransfPkgMsg msg, TransferServer.ClientSession session){
        Log.d(TAG, "dealCmdMsg() called with: msg = [" + msg + "], session = [" + session + "]");
        // targets
        // format type
        // data
        if(msg.targets == null){
            unpackMsg(msg,session);
            return;
        }
        if(msg.targets.contains(mPublisher.getServerTargetName())){
            unpackMsg(msg,session);
            msg.targets.remove(mPublisher.getServerTargetName());
        }
        if(msg.targets.size() > 0){
            rerouteMsg(msg);
        }
        Log.d(TAG, "dealCmdMsg() end! ");
    }

    private void unpackMsg(Beans.TransfPkgMsg msg, TransferServer.ClientSession session){

        if(msg.isOriginPayloadBytes()){
            // todo ,step 2 to support byte[]
            return;
        }

        TransfMsgWrapper msgWrapper = CodecUtil.decodeMsg(msg.getStrPayload());
        String msgType = msgWrapper.getMsgClassName();
        // switch
        if(Beans.TransfPkgMsg.VideoChannelState.class.getSimpleName().equals(msgType)){
            Beans.TransfPkgMsg.VideoChannelState chatMsg = GsonUtils.parseFromLinkedTreeMap(
                    (LinkedTreeMap) msgWrapper.getMsg(), Beans.TransfPkgMsg.VideoChannelState.class);
            if(chatMsg != null){
                session.isActive = chatMsg.active;
                session.isSendCfg = false;
            }
        }else if(Beans.TransfPkgMsg.ReqSyncTime.class.getSimpleName().equals(msgType)){
            Beans.TransfPkgMsg.ReqSyncTime tmpMsg = GsonUtils.parseFromLinkedTreeMap(
                    (LinkedTreeMap) msgWrapper.getMsg(), Beans.TransfPkgMsg.ReqSyncTime.class);
            Beans.TransfPkgMsg.ResSyncTime res = new Beans.TransfPkgMsg.ResSyncTime();
            res.serverTimeMs = System.currentTimeMillis();
            res.req = tmpMsg;
            sendInnerCmd(res,session.clientId);
        }else if(Beans.TransfPkgMsg.ReqReportClientInfo.class.getSimpleName().equals(msgType)){
            Beans.TransfPkgMsg.ReqReportClientInfo tmpMsg = GsonUtils.parseFromLinkedTreeMap(
                    (LinkedTreeMap) msgWrapper.getMsg(), Beans.TransfPkgMsg.ReqReportClientInfo.class);
            mPublisher.updateClientSessionInfo(session.mOus,tmpMsg.clientName,tmpMsg.netDelay);
        }

    }

    private void rerouteMsg(Beans.TransfPkgMsg msg){
        sendCmd(msg);
    }


    private void dealOuterCmdMsg(Beans.TransfPkgMsg msg, TransferServer.ClientSession session){
        if(mOutMsgHandler != null){
            if(msg.isOriginPayloadBytes()){
                // todo
            }else {
                mOutMsgHandler.onGotMsg(msg.getStrPayload(),session.clientId);
            }
        }
    }


    private void sendInnerCmd(Object msg,String target) {
        if(msg instanceof Beans.TransfPkgMsg)
            throw new RuntimeException(" sendObjectCmd msg should not be CommandMsg.class");
        String tmp = CodecUtil.encodeMsg(msg);
        Beans.TransfPkgMsg destMsg = Beans.TransfPkgMsg.Builder.genP2PMsg(tmp,target,1);
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

    IOutMsgHandler mOutMsgHandler = null;
    @Override
    public void setOutMsgHandler(IOutMsgHandler handler) {
        mOutMsgHandler = handler;
    }
}
