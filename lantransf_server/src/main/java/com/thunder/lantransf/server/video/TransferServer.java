package com.thunder.lantransf.server.video;

import static com.thunder.common.lib.bean.CommonDef.NSD_CONTROL_SERVICE_NAME;
import static com.thunder.common.lib.bean.CommonDef.NSD_SERVICE_TYPE_TCP;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.text.TextUtils;
import android.util.Log;


import com.thunder.common.lib.bean.CommonDef;
import com.thunder.common.lib.dto.Beans;
import com.thunder.lantransf.server.video.socketserver.ISocketServer;
import com.thunder.lantransf.server.video.socketserver.SocketServerImpl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

class TransferServer implements ITransfServer {

    private static final String TAG = "TransferServer";
    Context mCtx;
    IClientMsgDealer mClientMsgDealer;
    private List<ClientSession> mOusClients = new ArrayList<>();

    public String getServerTargetName(){
        return CommonDef.serverTargetName;
    }

    class ClientSession {
        String clientId; // gen by server
        String ip;
        long connectTimeMs; // net time ms
        long netDelayMs;
        boolean isVideoActive;
        boolean isSendCfg;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClientSession that = (ClientSession) o;
            return  Objects.equals(clientId, that.clientId);
        }

        @Override
        public String toString() {
            return "ClientSession{" +
                    "clientId=" + clientId +
                    ", ip='" + ip + '\'' +
                    ", connectTimeMs=" + connectTimeMs +
                    ", isActive=" + isVideoActive +
                    ", isSendCfg=" + isSendCfg +
                    '}';
        }

        public String toSampleString(){
            return "ClientSession{" +
                    "clientId=" + clientId +
                    ", ip='" + ip + '\'' +
                    '}';
        }
    }

    int mLastServerPort = -1;

    ISocketServer.IServerSocCb mNewSocketServerSb = new ISocketServer.IServerSocCb() {
        @Override
        public void onInitSuc(int port) {
            Log.i(TAG,"onServerBindSuc port:"+port);
            if(mStateCb != null){
                mStateCb.onSocketServerReady();
            }
            mLastServerPort = port;
            restartServerCnt = 0; // reset
            regNsdServer(port,mRegL);
            startPublishMsg();
        }

        @Override
        public void onStopped() {
            mStateCb.onSocketServerStopped();
            if(!mStopByUser){
                // restart it
                onUnExceptServerStop();
            }
        }

        @Override
        public void onGotClient(String clientName, String remoteHost) {
            // onGotClient
            ClientSession session = new ClientSession();
            session.isVideoActive = false;
            session.clientId = clientName;
            session.connectTimeMs = System.currentTimeMillis();
            session.ip = remoteHost;
            Log.i(TAG, "onGotClient: "+session);
            mOusClients.add(session);
            if(mStateCb != null){
                mStateCb.onGotClient(session);
            }
        }

        @Override
        public void onLostClient(String clientName) {
            ClientSession session = findClient(clientName);
            Log.i(TAG, "onSocClosed: session: "+ session);
            mOusClients.remove(session);
            if(mStateCb != null){
                mStateCb.onLoseClient(session);
            }
        }

        @Override
        public void onGotCmdMsg(String clientName, Beans.TransfPkgMsg msg) {
            Log.i(TAG, "onGotJsonMsg: msg:"+msg);
            dealJsonMsg(msg,clientName);
        }
    };

    @Override
    public void startTransfServer(Context context) {
        mCtx = context;
        mNsdManager = (NsdManager) mCtx.getSystemService(Context.NSD_SERVICE);
        mStopByUser = false;
        mLastServerPort = -1;
        restartServerCnt = 0;
        initVideoServerSocket( -1,-1, mNewSocketServerSb);
    }

    private boolean mStopByUser = false;
    @Override
    public void stopServer() {
        mStopByUser = true;
        //todo stop
    }

    private static final int MAX_RESTART_TIMES_ZONE = 3;
    private int restartServerCnt = 0;
    private void onUnExceptServerStop(){
        restartServerCnt++;
        if(restartServerCnt > MAX_RESTART_TIMES_ZONE){
            Log.e(TAG, "onUnExceptServerStop, DO-NOT to retry! return! for restartServerCnt: "+ restartServerCnt +" > MAX_RESTART_TIMES_ZONE("+MAX_RESTART_TIMES_ZONE+")");
            return;
        }
        Log.i(TAG, "onUnExceptServerStop: reStart server ("+restartServerCnt+")");
        // wait 1s to restart
        initVideoServerSocket(1000, mLastServerPort, mNewSocketServerSb);
    }



    @Override
    public ArrayBlockingQueue<Object> getOutputQue() {
        return mOutQue;
    }

    ArrayBlockingQueue<Object> mOutQue = new ArrayBlockingQueue<>(100);
    PublishMsgThread mPublishThread = null;
    @Override
    public void startPublishMsg() {
        if(mPublishThread != null){
            return;
        }
        mPublishThread = new PublishMsgThread(mOutQue);
        mPublishThread.start();
    }

    @Override
    public void updateClientSessionInfo(String clientName, long netDelay) {
        ClientSession session = findClient(clientName);
        if(session != null){
            session.netDelayMs = netDelay;
        }
    }

    @Override
    public void setMsgHandler(IClientMsgDealer dealer) {
        mClientMsgDealer = dealer;
    }

    ITransfServerStateCallBack mStateCb;
    @Override
    public void setStateCallBack(ITransfServerStateCallBack cb) {
        mStateCb = cb;
    }

    @Override
    public List<String> getClientList() {
        if(mOusClients == null || mOusClients.size() == 0){
            return new ArrayList<>();
        }
        ArrayList<String> res = new ArrayList();
        for (ClientSession tmp:mOusClients) {
            res.add(tmp.toSampleString());
        }
        return res;
    }

    private Beans.VideoData mVideoConfigData = null;

    class PublishMsgThread extends Thread{
        ArrayBlockingQueue<Object> publishQue;
        PublishMsgThread(ArrayBlockingQueue<Object> srcQue){
            publishQue = srcQue;
        }

        @Override
        public void run() {
            super.run();
            Log.d(TAG, "run() called");
            while (true){
                Object tmpData = null;
                try {
                    tmpData = publishQue.poll(3000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(tmpData == null){
                    Log.d(TAG, "run() video-null continue! ");
                    continue;
                }

                if(mSocServer == null){
                    Log.e(TAG, "run: socketServer is null! return! ");
                    continue;
                }

                if(tmpData instanceof Beans.VideoData){
                    publishVideo((Beans.VideoData) tmpData);
                }else if(tmpData instanceof Beans.TransfPkgMsg){
                    publishCmd((Beans.TransfPkgMsg) tmpData);
                }
            }
        }
    }

    int tmp = 0;
    private void publishVideo(Beans.VideoData videoData){
        if(videoData.isConfigFrame()){
            mVideoConfigData = videoData;
        }
        tmp ++;
        if(tmp%30 == 0){
            Log.d(TAG, " -----> publishVideo clientCnt: "+mOusClients.size()+" publishVideo-length: "+videoData.getH264Data().length);
        }
        // gen videoData with targets
        /*
        1. check if need cfg
            insert
        2. gen targets set into videoData
         */
        Set<String> cfgTargets = new HashSet<>();
        Set<String> normalVideoTargets = new HashSet<>();

        for (int i = 0; i < mOusClients.size(); i++) {
            ClientSession tmpClient = mOusClients.get(i);
            if(tmpClient.isVideoActive) {
                if (!tmpClient.isSendCfg && mVideoConfigData != null) {
                    cfgTargets.add(tmpClient.clientId);
                    tmpClient.isSendCfg = true;
                }
                normalVideoTargets.add(tmpClient.clientId);
            }
        }
        if(cfgTargets.size()>0){
            mVideoConfigData.setTargets(cfgTargets);
            mSocServer.publishVideo(mVideoConfigData);
        }
        if(normalVideoTargets.size() > 0){
            videoData.setTargets(normalVideoTargets);
            mSocServer.publishVideo(videoData);
        }
    }

    private void publishCmd(Beans.TransfPkgMsg transfPkgMsg){
        Log.d(TAG, " ---> publishCmd clientCount: "+mOusClients.size()+" commandMsg = [" + transfPkgMsg + "]");
        mSocServer.publishMsg(transfPkgMsg);
    }


    private void dealJsonMsg(Beans.TransfPkgMsg msg, String clientName){
        ClientSession client = findClient(clientName);
        if(client == null){
            Log.i(TAG, "dealJsonMsg: client not found !!!! msg: "+msg);
            return;
        }
        if(mClientMsgDealer != null){
            mClientMsgDealer.onGotCmd(msg,client);
        }
    }


    private ClientSession findClient(String clientName){
        if(TextUtils.isEmpty(clientName)){
            return null;
        }
        for (int i = 0; i < mOusClients.size(); i++) {
            try {
                ClientSession tmp = mOusClients.get(i);
                if(clientName.equals(tmp.clientId)){
                    return tmp;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return null;
    }

    ISocketServer mSocServer = null;
    private void initVideoServerSocket(int delayMs,int port,ISocketServer.IServerSocCb scb){
        Thread thread = new Thread(){
            @Override
            public void run() {
                super.run();
                if(delayMs > 0){
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                mSocServer = new SocketServerImpl();
                mSocServer.startServer(port,scb);
            }
        };
        thread.start();
    }




    NsdManager mNsdManager;
    NsdManager.RegistrationListener mRegL = new NsdManager.RegistrationListener() {
        @Override
        public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.d(TAG, "onRegistrationFailed() called with: serviceInfo = [" +
                    serviceInfo + "], errorCode = [" + errorCode + "]");
        }

        @Override
        public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.d(TAG, "onUnregistrationFailed() called with: serviceInfo = [" +
                    serviceInfo + "], errorCode = [" + errorCode + "]");
        }

        @Override
        public void onServiceRegistered(NsdServiceInfo serviceInfo) {
            Log.d(TAG, "onServiceRegistered() called with: serviceInfo = [" + serviceInfo + "]");
            if(mStateCb != null){
                mStateCb.onServicePublished();
            }
        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
            Log.d(TAG, "onServiceUnregistered() called with: serviceInfo = [" + serviceInfo + "]");
            if(mStateCb != null){
                mStateCb.onServiceCanceled();
            }
        }
    };

    private void regNsdServer(int port, NsdManager.RegistrationListener mRegCb){
        Log.i(TAG,"regNsdServer control-port:"+port);
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName(NSD_CONTROL_SERVICE_NAME);
        serviceInfo.setServiceType(NSD_SERVICE_TYPE_TCP);
        serviceInfo.setPort(port);
        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegCb);
    }



}
