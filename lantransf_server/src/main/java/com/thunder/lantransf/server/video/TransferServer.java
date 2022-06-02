package com.thunder.lantransf.server.video;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;


import com.thunder.common.lib.dto.Beans;
import com.thunder.common.lib.transf.SocketDealer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TransferServer implements ITransfServer {

    private static final String TAG = "TransferServer";
    private static final String NSD_CONTROL_SERVICE_NAME = "LSLanMediaServer";
    private static final String NSD_SERVICE_TYPE_TCP = "_http._tcp.";
    Context mCtx;
    IClientMsgDealer mClientMsgDealer;
    private List<ClientSession> mOusClients = new ArrayList<>();

    class ClientSession {
        int clientId; // auto inCrease
        String name;
        String ip;
        long connectTimeMs; // net time ms
        long netDelayMs;
        boolean isActive;
        boolean isSendCfg;
        OutputStream mOus;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClientSession that = (ClientSession) o;
            return  Objects.equals(mOus, that.mOus);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mOus );
        }

        @Override
        public String toString() {
            return "ClientSession{" +
                    "clientId=" + clientId +
                    ", ip='" + ip + '\'' +
                    ", connectTimeMs=" + connectTimeMs +
                    ", isActive=" + isActive +
                    ", isSendCfg=" + isSendCfg +
                    ", mOus=" + mOus +
                    '}';
        }
    }



    @Override
    public void startTransfServer(Context context) {
        mCtx = context;
        mNsdManager = (NsdManager) mCtx.getSystemService(Context.NSD_SERVICE);
        initVideoServerSocket(mVideoServerSocketCb);
    }

    @Override
    public void stopServer() {
        //todo stop
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
    public void updateClientSessionInfo(OutputStream clientOus, String name, long netDelay) {
        ClientSession session = findClient(clientOus);
        if(session != null){
            session.name = name;
            session.netDelayMs = netDelay;
        }
    }

    @Override
    public void setMsgDealer(IClientMsgDealer dealer) {
        mClientMsgDealer = dealer;
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
                if(tmpData instanceof Beans.VideoData){
                    publishVideo((Beans.VideoData) tmpData);
                }else if(tmpData instanceof Beans.CommandMsg){
                    publishCmd((Beans.CommandMsg) tmpData);
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
        for (int i = 0; i < mOusClients.size(); i++) {
            try {
                ClientSession tmpClient = mOusClients.get(i);
                if(tmpClient.isActive){
                    if(!tmpClient.isSendCfg && mVideoConfigData != null){
                        Log.i(TAG, " TransfServer clientIndex:"+i+" send videoCfg, msg: "+mVideoConfigData);
                        Log.i(TAG, " --> videoCfg-v-data: "+ Arrays.toString(mVideoConfigData.getH264Data()));
                        SocketDealer.sendVideoMsg(tmpClient.mOus,mVideoConfigData);
                        tmpClient.isSendCfg = true;
                    }
                    SocketDealer.sendVideoMsg(tmpClient.mOus,videoData);
                }
                //todo timeout deal
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void publishCmd(Beans.CommandMsg commandMsg){
        Log.d(TAG, " ---> publishCmd clientCount: "+mOusClients.size()+" commandMsg = [" + commandMsg + "]");
        for (int i = 0; i < mOusClients.size(); i++) {
            try {
                ClientSession tmpClient = mOusClients.get(i);
                if(tmpClient.isActive){
                    if(commandMsg.targets == null /* broadcast */
                            || commandMsg.targets.contains(tmpClient.clientId) /* p2p */){
                        SocketDealer.sendCmdMsg(tmpClient.mOus,commandMsg);
                    }
                }
                //todo timeout deal
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private int mClientId = 1; // client index start
    private int genClientID(){
        return ++mClientId;
    }

    SocketDealer.ISocDealCallBack clientSocDataHandler = new SocketDealer.ISocDealCallBack() {

        @Override
        public void onGotOus(OutputStream ous, String remoteHost) {
            ClientSession session = new ClientSession();
            session.mOus = ous;
            session.isActive = true;
            session.clientId = genClientID();
            session.connectTimeMs = System.currentTimeMillis();
            session.ip = remoteHost;
            Log.i(TAG, "onGotClient: "+session);
            mOusClients.add(session);
        }

        @Override
        public void onSocClosed(Socket socket, OutputStream ous) {
            ClientSession session = findClient(ous);
            Log.i(TAG, "onSocClosed: session: "+ session);
            mOusClients.remove(session);
        }

        @Override
        public void onGotVideoMsg(Beans.VideoData msg, OutputStream ous) {
            // nothing
        }

        @Override
        public void onGotJsonMsg(Beans.CommandMsg msg, OutputStream ous) {
            // open or stop channel
            Log.i(TAG, "onGotJsonMsg: msg:"+msg);
            dealJsonMsg(msg,ous);
        }
    };

    private void dealJsonMsg(Beans.CommandMsg msg, OutputStream srcOus){
        ClientSession client = findClient(srcOus);
        if(client == null){
            Log.i(TAG, "dealJsonMsg: client not found !!!! msg: "+msg);
            return;
        }
        if(mClientMsgDealer != null){
            mClientMsgDealer.onGotCmd(msg,client);
        }
    }


    private ClientSession findClient(OutputStream ous){
        if(ous == null){
            return null;
        }
        for (int i = 0; i < mOusClients.size(); i++) {
            try {
                ClientSession tmp = mOusClients.get(i);
                if(ous.equals(tmp.mOus)){
                    return tmp;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return null;
    }



    IServerSocketCallBack mVideoServerSocketCb = new IServerSocketCallBack() {
        @Override
        public void onGotClient(Socket socket) {
            // onGotClient
            SocketDealer client = new SocketDealer();
            client.init(socket,clientSocDataHandler);
            client.begin();
        }

        @Override
        public void onServerBindSuc(int port) {
            Log.i(TAG,"onServerBindSuc port:"+port);
            regNsdServer(port,mRegL);
        }
    };

    private void initVideoServerSocket(IServerSocketCallBack scb){
        Thread thread = new Thread(){
            @Override
            public void run() {
                super.run();
                startServerSocket(scb);
            }
        };
        thread.start();
    }

    private int startServerSocket(IServerSocketCallBack scb){
        int port = -1;
        try {
            ServerSocket serverSocket = new ServerSocket(0);
            port = serverSocket.getLocalPort();
            if(scb != null){
                scb.onServerBindSuc(port);
            }
            Log.i(TAG,"initSocket port:"+port);
            while (!serverSocket.isClosed()){
                Socket socket = serverSocket.accept();
                if(scb != null){
                    scb.onGotClient(socket);
                }
            }
        } catch (IOException e) {
            Log.e(TAG,e.getMessage());
        }
        return port;
    }




    private interface IServerSocketCallBack{
        void onGotClient(Socket socket);
        void onServerBindSuc(int port);
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
        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
            Log.d(TAG, "onServiceUnregistered() called with: serviceInfo = [" + serviceInfo + "]");
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
