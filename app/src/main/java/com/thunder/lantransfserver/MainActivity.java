package com.thunder.lantransfserver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.gson.internal.LinkedTreeMap;
import com.thunder.common.lib.util.GsonUtils;
import com.thunder.lantransf.client.video.ClientApi;
import com.thunder.lantransf.client.video.IClientApi;
import com.thunder.lantransf.msg.CmdMsg;
import com.thunder.lantransf.msg.TransfMsgWrapper;
import com.thunder.lantransf.msg.codec.CodecUtil;
import com.thunder.lantransf.server.video.IServerApi;
import com.thunder.lantransf.server.video.ServerApi;
import com.thunder.lantransfserver.video.GenVideoAndPlay;

import java.util.ArrayList;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity {

    /**
     * 0. 自动打包 添加版本号
     *      内网maven
     *
     *  公版逻辑 + server-sdk
     *
     *  demo + client-sdk
     *  client-demo  surface-view UI 要与公版一致
     *
     *  公版 + client-sdk
     *  播放状态msg 同步
     *  歌单列表同步
     *  用户信息同步
     *  首页数据同步
     *  收藏逻辑同步
     *      收藏action --> local-server --> web-server
     *                           ^              \
     *                           \--------------\
     *
     *
     *
     *
     * 1. server 停止后,自动恢复, 同时给出状态回调
     *      a. 自动恢复, msg-server , socket-server ,
     *          尝试使用原来端口号重启, done
     *          重启完成后,再次注册mdns done
     *      todo b. video-server
     *
     * 2. server 向client 推送数据, 多个 client 不互相阻塞, 通过nio 监听解决, 可写状态
     *  a. socket obj:
     *      read buf
     *      write msg queue
     *      write buf
     *  b. 将client消息队列的实时情况打印出来,
     *
     * 3. 分配clientName done
     * 4. nio 改造 server done
     * 5. msg pack 添加tag-flag done
     *
     * client
     * 1. 断开后重连.
     *      重连上一个地址,
     *      监听新地址, 发起连接
     * 2. 从服务端获取clientName d
     * 3. 连接速率提高
     * 4. nio 改造 d
     * 5. 视频防止黑屏 d
     *
     *
     *
     *
     *  common
     *  1. h264 frame 类型检测util
     *  2.
     *
     *
     */


    private static final String TAG = "MainActivity";
    SurfaceView mSvClient1;
    TextView mTvServerInfo;
    TextView mTvServerMsgInfo;

    TextView mTvClientInfo;
    TextView mTvClientMsgInfo;

    ServerStateInfo mServerInfo = new ServerStateInfo();
    ClientStateInfo mClientInfo = new ClientStateInfo();

    Handler mHandler = new Handler();

    class ServerStateInfo{
        int serverState;
        int mediaServerState;
        String[] clients;

        String getServerStateStr(){
            if(serverState == IServerApi.IServerStateChangeCallBack.ServerState.STARTED.ordinal()){
                return "server-started";
            }else {
                return "server-none";
            }
        }

        String getServerMediaStateStr(){
            if(mediaServerState == IServerApi.IServerStateChangeCallBack.ServerMediaState.STARTED.ordinal()){
                return "media-started";
            } else if(mediaServerState == IServerApi.IServerStateChangeCallBack.ServerMediaState.FIRST_FRAME_GENERATED.ordinal()){
                return "media-generated";
            } else if(mediaServerState == IServerApi.IServerStateChangeCallBack.ServerMediaState.FIRST_FRAME_PUBLISHED.ordinal()){
                return "media-published";
            } else if(mediaServerState == IServerApi.IServerStateChangeCallBack.ServerMediaState.STOPPED.ordinal()){
                return "media-stopped";
            } else {
                return "media-none";
            }
        }

        @Override
        public String toString() {
            return "Server: " +
                    "\n serverState " + getServerStateStr() +
                    "\n mediaServerState " + getServerMediaStateStr() +
                    "\n clients=" + Arrays.toString(clients);
        }
    }

    class ClientStateInfo{
        String clientState;
        String videoState;
        String clientHost;
        String clientName;

        @Override
        public String toString() {
            return "Client: " +
                    "\n clientState " + clientState +
                    "\n videoState " + videoState +
                    "\n clientName " + clientName ;
        }
    }

    private void updateInfo(){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mServerInfo.clients = ServerApi.getInstance().getClientList();
                mTvServerInfo.setText(mServerInfo.toString());
                mTvClientInfo.setText(mClientInfo.toString());
            }
        });
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.v_close).setOnClickListener(clk);
        findViewById(R.id.tv_start_server).setOnClickListener(clk);
        findViewById(R.id.tv_stop_server).setOnClickListener(clk);
        findViewById(R.id.tv_start_publish_video).setOnClickListener(clk);
        findViewById(R.id.tv_send_msg_play_state).setOnClickListener(clk);
        mSvBox = findViewById(R.id.sv_box);
        mSvClient1 = findViewById(R.id.sv_client);


        findViewById(R.id.tv_client_start).setOnClickListener(clk);
        findViewById(R.id.tv_client_stop).setOnClickListener(clk);
        findViewById(R.id.tv_client_show).setOnClickListener(clk);
        findViewById(R.id.tv_client_hidden).setOnClickListener(clk);
        findViewById(R.id.tv_client_btnckl).setOnClickListener(clk);
        findViewById(R.id.tv_client_get_state).setOnClickListener(clk);

        mTvServerInfo = findViewById(R.id.tv_server_info);
        mTvServerMsgInfo = findViewById(R.id.rec_msg_info);

        mTvClientInfo = findViewById(R.id.tv_client_info);
        mTvClientMsgInfo = findViewById(R.id.client_rec_msg_info);

    }


    View.OnClickListener clk = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.v_close:{
                    finish();
                    System.exit(0);
                    break;
                }
                case R.id.tv_start_server:{
                    startServer();
                    break;
                }
                case R.id.tv_stop_server:{
//                    stop
                    break;
                }
                case R.id.tv_start_publish_video:{
                    startPublish();
                    break;
                }
                case R.id.tv_send_msg_play_state:{
                    serverBroadMsg();
                    break;
                }

                // client
                case R.id.tv_client_start:{
                    initClient1();
                    break;
                }
                case R.id.tv_client_stop:{
                    if(clientApi1 != null){
                        clientApi1.toDisConnectServer();
                    }
                    break;
                }
                case R.id.tv_client_show:{
                    startShow();
                    break;
                }
                case R.id.tv_client_hidden:{
                    stopShow();
                    break;
                }
                case R.id.tv_client_btnckl:{
                    sendBtnClickMsgToServer();
                    break;
                }
                case R.id.tv_client_get_state:{
                    sendGetPlayStateMsgToServer();
                    break;
                }
            }
        }
    };


    private void startServer(){
        ServerApi.getInstance().init(this);
        ServerApi.getInstance().startServer();
        ServerApi.getInstance().setStateChangeCallBack(serverStateCb);
        ServerApi.getInstance().setMsgHandler(mServerRecMsgHandler);
    }

    IServerApi.IServerStateChangeCallBack serverStateCb = new IServerApi.IServerStateChangeCallBack() {
        @Override
        public void onServerStateChanged(int state) {
            Log.d(TAG, "onServerStateChanged() called with: state = [" + state + "]");
            mServerInfo.serverState = state;
            updateInfo();
        }

        @Override
        public void onMediaPublishStateChanged(int state) {
            Log.d(TAG, "onMediaPublishStateChanged() called with: state = [" + state + "]");
            mServerInfo.mediaServerState = state;
            updateInfo();
        }

        @Override
        public void onGotClient(String clientName) {
            Log.d(TAG, "onGotClient() called with: clientName = [" + clientName + "]");
            mServerInfo.clients = ServerApi.getInstance().getClientList();
            updateInfo();
        }

        @Override
        public void onLossClient(String clientName) {
            Log.d(TAG, "onLossClient() called with: clientName = [" + clientName + "]");
            mServerInfo.clients = ServerApi.getInstance().getClientList();
            updateInfo();
        }
    };

    private void startPublish(){
        Log.d(TAG, "startPublish() called");
        Surface sinkSurface = ServerApi.getInstance().startPublishMedia();
        //todo check encoder is started
        GenVideoAndPlay genVideoAndPlay = new GenVideoAndPlay();
        genVideoAndPlay.startGenVideo(sinkSurface,1280,720);
    }

    private void serverBroadMsg(){
        Log.d(TAG, "serverBroadMsg() called");
        CmdMsg.ResPlayState playState = new CmdMsg.ResPlayState();
        playState.playing = true;
        String tmpStr = CodecUtil.encodeMsg(playState);
        ServerApi.getInstance().sendMsg(null,tmpStr);
        printServerMsg("server -> send play state ");
    }

    // serverMsg
    private void printServerMsg(String text){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mTvServerMsgInfo.setText(text+"\n"+mTvServerMsgInfo.getText());
            }
        });
    }

    private void printClientMsg(String text){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mTvClientMsgInfo.setText(text+"\n"+mTvClientMsgInfo.getText());
            }
        });
    }

//    ["msg":{"clickType":"ACC_BTN"},"msgClassName":"ReqUserClickAction"}], from = [2]

    IServerApi.IRecMsgHandler mServerRecMsgHandler = new IServerApi.IRecMsgHandler() {
        @Override
        public void onGetMsg(String msg, String from) {
            Log.d(TAG, " Server -> onGetMsg() called with: msg = [" + msg + "], from = [" + from + "]");
            TransfMsgWrapper msgWrapper = CodecUtil.decodeMsg(msg);
            String msgType = msgWrapper.getMsgClassName();
            printServerMsg("server <- get "+msgType);
            if(CmdMsg.ReqCommon.class.getSimpleName().equals(msgType)){
                CmdMsg.ReqCommon tmpMsg = GsonUtils.parseFromLinkedTreeMap(
                    (LinkedTreeMap) msgWrapper.getMsg(), CmdMsg.ReqCommon.class);

                if(CmdMsg.ReqCommon.Type.getPlayState.name().equals(tmpMsg.type)){
                    // getState
                    CmdMsg.ResPlayState res = new CmdMsg.ResPlayState();
                    res.playing = true; // mock
                    String tmpDest = CodecUtil.encodeMsg(res);
                    ArrayList<String> targets = new ArrayList<>();
                    targets.add(from);
                    ServerApi.getInstance().sendMsg(targets,tmpDest);
                }
            }else if(CmdMsg.ReqUserClickAction.class.getSimpleName().equals(msgType)){
                    CmdMsg.ReqUserClickAction tmpMsg = GsonUtils.parseFromLinkedTreeMap(
                        (LinkedTreeMap) msgWrapper.getMsg(), CmdMsg.ReqUserClickAction.class);
                // perform
                if(CmdMsg.ReqUserClickAction.Type.PLAY_BTN.name().equals(tmpMsg.clickType)){
                    Log.i(TAG, "onGetMsg: PLAY_BTN");
                }else if(CmdMsg.ReqUserClickAction.Type.ACC_BTN.name().equals(tmpMsg.clickType)){
                    Log.i(TAG, "onGetMsg: ACC_BTN");
                }
            }
        }

        @Override
        public void onGetMsg(byte[] msg, String from) {
            //todo
        }
    };




//========================================================================================
//========================================================================================
//================== client api demo =====================================================
//========================================================================================
//========================================================================================
//========================================================================================

    View mSvBox = null;
    private Surface mSurface;
    ClientApi clientApi1;
    private void initClient1(){
        Log.i(TAG, "initClient1: ");
        // 自动开始  鲁棒性
        clientApi1 = ClientApi.getInstance();
        clientApi1.init(MainActivity.this,new ClientApi.ClientConfig.Builder().setAutoReconnect(true).build());
//        clientApi1.toConnectServer();
        clientApi1.setStateChangeCallBack(mClientApiCb);
        mSvClient1.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                Log.i(TAG, "surfaceCreated: ");
                clientApi1.startShow(surfaceHolder.getSurface());
                mSurface = surfaceHolder.getSurface();
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
                clientApi1.stopShow();
                mSurface = null;
            }
        });

        clientApi1.setMsgHandler(mClientRecMsgHandler);
    }

    IClientApi.IRecMsgHandler mClientRecMsgHandler = new IClientApi.IRecMsgHandler() {
        @Override
        public void onGetMsg(String msg, String from) {
            Log.d(TAG, " Client --> onGetMsg() called with: msg = [" + msg + "], from = [" + from + "]");
            TransfMsgWrapper wrapper = CodecUtil.decodeMsg(msg);
            String msgType = wrapper.getMsgClassName();
            printClientMsg("client <- get "+msgType);
            if(CmdMsg.ResAccState.class.getSimpleName().equals(msgType)){
                CmdMsg.ResAccState tmpMsg = GsonUtils.parseFromLinkedTreeMap(
                        (LinkedTreeMap) wrapper.getMsg(), CmdMsg.ResAccState.class);
                Log.d(TAG, "onGetMsg() called with: CmdMsg.ResAccState "+tmpMsg);
            }
        }

        @Override
        public void onGetMsg(byte[] msg, String from) {
            //todo
        }
    };

    private void startShow(){
        mSvClient1.setVisibility(View.VISIBLE);
    }

    private void stopShow(){
        mSvClient1.setVisibility(View.INVISIBLE);
    }

    private void sendGetPlayStateMsgToServer(){
        CmdMsg.ReqCommon reqCommon = new CmdMsg.ReqCommon();
        reqCommon.type = CmdMsg.ReqCommon.Type.getPlayState.name();
        String tmpStr = CodecUtil.encodeMsg(reqCommon);
        clientApi1.sendMsg(null,tmpStr);
        printClientMsg("client -> send getPlayState ");
    }

    private void sendBtnClickMsgToServer(){
        CmdMsg.ReqUserClickAction clkAct = new CmdMsg.ReqUserClickAction();
        clkAct.clickType = CmdMsg.ReqUserClickAction.Type.ACC_BTN.name();
        String tmpStr = CodecUtil.encodeMsg(clkAct);
        clientApi1.sendMsg(null,tmpStr);
        printClientMsg("client -> send ACC_BTN ");
    }


    IClientApi.IClientStateChangeCallBack mClientApiCb = new IClientApi.IClientStateChangeCallBack(){

        @Override
        public void onRegFind() {
            Log.i(TAG, "onRegFind() called");
            mClientInfo.clientState = "onRegFind";
            updateInfo();
        }

        @Override
        public void onFindServer() {
            Log.i(TAG, "onFindServer() called");
            mClientInfo.clientState = "onFindServer";
            updateInfo();
        }

        @Override
        public void onServerConnected(String clientHost) {
            Log.i(TAG, "onServerConnected() called");
            mClientInfo.clientState = "onServerConnected";
            mClientInfo.clientHost = clientHost;
            if(mSurface!=null){
                // auto start show
                clientApi1.startShow(mSurface);
            }
            updateInfo();
        }

        @Override
        public void onGotClientInfo(String clientName) {
            Log.i(TAG, "onGotClientInfo() called with: clientName = [" + clientName + "]");
            mClientInfo.clientName = clientName;
            updateInfo();
        }

        @Override
        public void onServerDisConnected() {
            Log.i(TAG, "onServerDisConnected() called");
            mClientInfo.clientState = "onServerDisConnected";
            clientApi1.stopShow();
            updateInfo();
        }

        @Override
        public void onVideoStart() {
            Log.i(TAG, "onVideoStart() called");
            mClientInfo.videoState = "V--Start";
            updateInfo();
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    int boxW =mSvBox.getWidth();
                    int boxH =mSvBox.getHeight();
                    int maxW = 0;
                    int maxH = 0;
                    if(boxW*1f/boxH > 400f/300){ // w / h
                        maxH = boxH;
                    }else {
                        maxW = boxW;
                    }

                    int destW = 0;
                    int destH = 0;
                    if(maxW > 0){
                        destW = maxW;
                        destH = (int) (300f/400*destW);
                    }else {
                        destH = maxH;
                        destW = (int) (400f/300*destH);
                    }
                    ConstraintLayout.LayoutParams llp = (ConstraintLayout.LayoutParams) mSvClient1.getLayoutParams();
                    llp.width = destW;
                    llp.height = destH;
                    mSvClient1.setLayoutParams(llp);
                }
            });
        }

        @Override
        public void onVideoStop() {
            Log.i(TAG, "onVideoStop() called");
            mClientInfo.videoState = "V--Stop";
            updateInfo();
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    ConstraintLayout.LayoutParams llp = (ConstraintLayout.LayoutParams) mSvClient1.getLayoutParams();
                    llp.width = 100;
                    llp.height = 100;
                    mSvClient1.setLayoutParams(llp);
                }
            });
        }

        @Override
        public void onDebugInfo(String info) {
            printClientMsg("debug: "+info);
        }
    };
}