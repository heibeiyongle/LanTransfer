package com.thunder.lantransf.client.video;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import com.thunder.common.lib.dto.Beans;
import com.thunder.common.lib.transf.ITransf;
import com.thunder.common.lib.transf.SocketDealer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhe on 2022/3/19 23:39
 *
 * @desc:
 */
public class TransfClient implements ITransferClient{

    private static final String TAG = "TransfClient";

    private static final String NSD_CONTROL_SERVICE_NAME = "LSLanMediaServer";
    private static final String NSD_SERVICE_TYPE_TCP = "_http._tcp.";


    Context mCtx;
    NsdManager mNsdManager;
    IClientDataHandler mClientDataHandler;
    IClientStateHandler mClientStateHandler;

    /**
     * 1. search nsd
     *      connect server
     * 2. send msg
     * 3. receive msg
     * 4. deal msg
     *
     *
     * @param context
     */


    @Override
    public void init(Context context) {
        mCtx = context;
        mNsdManager = (NsdManager) mCtx.getSystemService(Context.NSD_SERVICE);
    }

    @Override
    public void connectServer() {
        if(mSocOus != null){
            Log.i(TAG, "connectServer: soc already connected ! return!");
            return;
        }
        initializeDiscoveryListener();
    }

    @Override
    public void disconnectServer() {
        // todo
    }

    @Override
    public void sendViewActive() {
        Beans.CommandMsg.VideoChannelState state = new Beans.CommandMsg.VideoChannelState();
        state.active = true;
        addCmdToQue(state,0);
    }

    @Override
    public void sendViewInActive() {
        Beans.CommandMsg.VideoChannelState state = new Beans.CommandMsg.VideoChannelState();
        state.active = false;
        addCmdToQue(state,0);
    }

    @Override
    public void sendPlayBtnClick() {
        Beans.CommandMsg.ReqUserClickAction act = new Beans.CommandMsg.ReqUserClickAction();
        act.clickType = Beans.CommandMsg.ReqUserClickAction.Type.PLAY_BTN.name();
        addCmdToQue(act,0);
    }

    @Override
    public void sendAccBtnClick() {
        Beans.CommandMsg.ReqUserClickAction act = new Beans.CommandMsg.ReqUserClickAction();
        act.clickType = Beans.CommandMsg.ReqUserClickAction.Type.ACC_BTN.name();
        addCmdToQue(act,0);
    }

    @Override
    public void syncNetTime() {
        Beans.CommandMsg.ReqSyncTime act = new Beans.CommandMsg.ReqSyncTime();
        act.clientTimeMs = System.currentTimeMillis();
        addCmdToQue(act,0);
    }

    @Override
    public void reportClientInfo(String clientName, long netDelay) {
        Beans.CommandMsg.ReqReportClientInfo act = new Beans.CommandMsg.ReqReportClientInfo();
        act.clientName = clientName;
        act.netDelay = netDelay;
        addCmdToQue(act,0);
    }

    @Override
    public void getPlayState() {
        Beans.CommandMsg.ReqCommon act = new Beans.CommandMsg.ReqCommon();
        act.type = Beans.CommandMsg.ReqCommon.Type.getPlayState.name();
        addCmdToQue(act,0);
    }

    @Override
    public void getAccState() {
        Beans.CommandMsg.ReqCommon act = new Beans.CommandMsg.ReqCommon();
        act.type = Beans.CommandMsg.ReqCommon.Type.getAccState.name();
        addCmdToQue(act,0);    }

    @Override
    public void setClientDataHandler(IClientDataHandler cb) {
        mClientDataHandler = cb;
    }

    @Override
    public void setClientStateHandler(IClientStateHandler cb) {
        mClientStateHandler = cb;
    }


    NsdServiceInfo targetServiceInfo = null;
    public void initializeDiscoveryListener() {
        Log.d(TAG, "initializeDiscoveryListener() called");
        NsdManager.DiscoveryListener discoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onDiscoveryStarted(String regType) {
                Log.i(TAG, "Service discovery started");
                mClientStateHandler.onRegFindService();
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.i(TAG, "Service discovery success" + service);
                if (!service.getServiceType().equals(NSD_SERVICE_TYPE_TCP)) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.i(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(NSD_CONTROL_SERVICE_NAME)) {
                    if(targetServiceInfo == null){
                        mClientStateHandler.onFindServerService();
                        targetServiceInfo = service;
                        mNsdManager.resolveService(service, mResolveL);
                    }
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.e(TAG, "service lost: " + service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };

        mNsdManager.discoverServices(
                NSD_SERVICE_TYPE_TCP, NsdManager.PROTOCOL_DNS_SD, discoveryListener);

    }

    private String mServerHost = "";
    NsdManager.ResolveListener mResolveL = new NsdManager.ResolveListener() {
        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.i(TAG, "onResolveFailed() called with: serviceInfo = [" + serviceInfo + "], errorCode = [" + errorCode + "]");
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            Log.i(TAG, "onServiceResolved() called with: serviceInfo = [" + serviceInfo + "]");
            String host = serviceInfo.getHost().getHostAddress();
            int port =serviceInfo.getPort();
            mServerHost = host;
            mClientStateHandler.onGotServerInfo();
            innerConnectSocket(host,port);
        }
    };

    SocketDealer mClient = null;
    OutputStream mSocOus = null;
    MsgSenderThread mMsgSenderT = null;
    private void innerConnectSocket(String host, int port){
        Log.i(TAG, "onGotServerInfo() called with: host = [" + host + "], port = [" + port + "]");
        new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    Socket soc = new Socket(host,port);
                    mClient = new SocketDealer();
                    mClient.init(soc,mSocDealCb);
                    mClient.begin();
                    if(mMsgSenderT == null){
                        mMsgSenderQue.clear();
                        mMsgSenderT = new MsgSenderThread();
                        mMsgSenderT.start();
                    }

                } catch (IOException e) {
                    Log.e(TAG, "onGotServerInfo: ", e);
                }
            }
        }.start();
    }


    ITransf.ISocDealCallBack mSocDealCb = new ITransf.ISocDealCallBack() {
        @Override
        public void onGotOus(OutputStream ous, String remote) {
            mSocOus = ous;
            mClientStateHandler.onConnect(remote);
        }

        @Override
        public void onGotJsonMsg(Beans.CommandMsg msg, OutputStream ous) {
            // cmd
            Log.i(TAG, "onGotJsonMsg: msg:"+msg);
            mClientDataHandler.onGotCmdData(msg);
        }

        @Override
        public void onSocClosed(Socket socket, OutputStream ous) {
            mSocOus = null;
            mClientStateHandler.onDisconnect();
        }

        @Override
        public void onGotVideoMsg(Beans.VideoData msg, OutputStream ous) {
//            Log.i(TAG, "onGotVideoMsg() called with: msg = [" + msg + "], ous = [" + ous + "]");
            mClientDataHandler.onGotVideoData(msg);
        }

    };

    private void addCmdToQue(Object msg, int target) {
        if(msg instanceof Beans.CommandMsg) throw new RuntimeException(" sendObjectCmd msg should not be CommandMsg.class");
        if(mSocOus == null){
            Log.i(TAG, "addCmdToQue: mSocOus is null, return! ");
            return;
        }
        Beans.CommandMsg destMsg = Beans.CommandMsg.Builder.genP2PMsg(msg,target);
        mMsgSenderQue.offer(destMsg);
    }

    ArrayBlockingQueue<Beans.CommandMsg> mMsgSenderQue = new ArrayBlockingQueue<>(100);
    class MsgSenderThread extends Thread{

        boolean mExit = false;
        void exit(){
            mExit = true;
        }

        @Override
        public void run() {
            super.run();
            while (!mExit){
                Beans.CommandMsg msg = null;
                try {
                    msg = mMsgSenderQue.poll(100, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(msg == null){
                    continue;
                }
                SocketDealer.sendCmdMsg(mSocOus,msg);
            }
        }
    }

}
