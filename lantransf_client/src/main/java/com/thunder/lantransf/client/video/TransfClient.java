package com.thunder.lantransf.client.video;

import static com.thunder.common.lib.bean.CommonDef.NSD_CONTROL_SERVICE_NAME;
import static com.thunder.common.lib.bean.CommonDef.NSD_SERVICE_TYPE_TCP;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import com.thunder.common.lib.dto.Beans;
import com.thunder.lantransf.client.video.socketclient.ISocketClient;
import com.thunder.lantransf.client.video.socketclient.ClientSocketImpl;
import com.thunder.lantransf.msg.codec.CodecUtil;

import java.util.HashSet;

/**
 * Created by zhe on 2022/3/19 23:39
 *
 * @desc:
 */
public class TransfClient implements ITransferClient{

    private static final String TAG = "TransfClient";


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
        if(mIsConnected){
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
        Beans.TransfPkgMsg.VideoChannelState state = new Beans.TransfPkgMsg.VideoChannelState();
        state.active = true;
        addInnerMsgToQue(state,0);
    }

    @Override
    public void sendViewInActive() {
        Beans.TransfPkgMsg.VideoChannelState state = new Beans.TransfPkgMsg.VideoChannelState();
        state.active = false;
        addInnerMsgToQue(state,0);
    }

    @Override
    public void sendMsg(Beans.TransfPkgMsg msg) {
        if(mSocketClient != null){
            mSocketClient.sendMsg(msg);
        }
    }

    @Override
    public void syncNetTime() {
        Beans.TransfPkgMsg.ReqSyncTime act = new Beans.TransfPkgMsg.ReqSyncTime();
        act.clientTimeMs = System.currentTimeMillis();
        addInnerMsgToQue(act,0);
    }

    @Override
    public void reportClientInfo(long netDelay) {
        Beans.TransfPkgMsg.ReqReportClientInfo act = new Beans.TransfPkgMsg.ReqReportClientInfo();
        act.netDelay = netDelay;
        addInnerMsgToQue(act,0);
    }

    @Override
    public void updateLocalClientName(String clientName) {
        if(mSocketClient != null){
            mSocketClient.updateClientInfo(clientName);
        }
    }

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
            if(!mIsConnected){
                innerConnectSocket(host,port);
            }else {
                Log.i(TAG, "onServiceResolved , but socket is connected, ignore! host: "+host);
            }
        }
    };

    private boolean mIsConnected = false;
    ISocketClient.IClientSocCb mInnerClientCb = new ISocketClient.IClientSocCb() {
        @Override
        public void onConnectSuc(String remoteHost) {
            mIsConnected = true;
            mClientStateHandler.onConnect(remoteHost);
        }

        @Override
        public void onClosed() {
            mIsConnected = false;
            mClientStateHandler.onDisconnect();
        }

        @Override
        public void onGotCmdMsg(Beans.TransfPkgMsg msg) {
            Log.i(TAG, "onGotJsonMsg: msg:"+msg);
            mClientDataHandler.onGotCmdData(msg);
        }

        @Override
        public void onGotVideoMsg(Beans.VideoData msg) {
            mClientDataHandler.onGotVideoData(msg);
        }
    };


    ISocketClient mSocketClient = null;
    private void innerConnectSocket(String host, int port){
        Log.i(TAG, "onGotServerInfo() called with: host = [" + host + "], port = [" + port + "]");
        new Thread(){
            @Override
            public void run() {
                super.run();
                mSocketClient = new ClientSocketImpl();
                mSocketClient.connect(host,port,mInnerClientCb);
            }
        }.start();
    }



    private void addInnerMsgToQue(Object msg, int target) {
        if(msg instanceof Beans.TransfPkgMsg) throw new RuntimeException(" sendObjectCmd msg should not be CommandMsg.class");
        if( !mIsConnected ){
            Log.i(TAG, "addInnerMsgToQue mIsConnected = false , return! ");
            return;
        }

        String dest = CodecUtil.encodeMsg(msg);
        HashSet<String> ts = new HashSet<>();
        addMsgToQue(dest,ts,false);
    }

    private void addMsgToQue(String msgStr,HashSet<String> targets,boolean outerMsg){
        Beans.TransfPkgMsg destMsg = Beans.TransfPkgMsg.Builder.genSpecTargetsMsg(msgStr,targets,outerMsg?0:1);
        mSocketClient.sendMsg(destMsg);
    }

}
