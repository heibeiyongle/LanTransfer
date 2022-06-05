package com.thunder.lantransfserver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.thunder.lantransf.client.video.ClientApi;
import com.thunder.lantransf.client.video.IClientApi;
import com.thunder.lantransf.server.video.IServerApi;
import com.thunder.lantransf.server.video.ServerApi;

import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    SurfaceView mSvClient1;
    SurfaceView mSvClient2;
    TextView mTvInfo;

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
                mTvInfo.setText(mServerInfo.toString()+"\n====\n"+mClientInfo.toString());
            }
        });
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.v_close).setOnClickListener(clk);
        findViewById(R.id.tv_start_server).setOnClickListener(clk);
        findViewById(R.id.tv_start_publish_video).setOnClickListener(clk);


        mSvClient1 = findViewById(R.id.sv_client);
        findViewById(R.id.tv_start_client).setOnClickListener(clk);
        findViewById(R.id.tv_stop_client).setOnClickListener(clk);


        mSvClient2 = findViewById(R.id.sv_client2);
        findViewById(R.id.tv_show2).setOnClickListener(clk);
        findViewById(R.id.tv_hide2).setOnClickListener(clk);

        mTvInfo = findViewById(R.id.tv_info);
        initClient1();
//        initClient2();
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
                case R.id.tv_start_publish_video:{
                    startPublish();
                    break;
                }

                case R.id.tv_start_client:{
                    startClient();
                    break;
                }
                case R.id.tv_stop_client:{
                    stopClient();
                    break;
                }

//                case R.id.tv_show2:{
//                    startClient2();
//                    break;
//                }
//                case R.id.tv_hide2:{
//                    stopClient2();
//                    break;
//                }

            }
        }
    };


    private void startServer(){
        ServerApi.getInstance().init(this);
        ServerApi.getInstance().startServer();
        ServerApi.getInstance().setStateChangeCallBack(serverStateCb);
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
        Surface sinkSurface = ServerApi.getInstance().startPublishMedia();
    }




    ClientApi clientApi1;
    private void initClient1(){
        // 自动开始  鲁棒性
        clientApi1 = ClientApi.getInstance();
        clientApi1.init(MainActivity.this);
        clientApi1.autoConnectServer();
        clientApi1.setStateChangeCallBack(mClientApiCb);

        mSvClient1.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                clientApi1.startShow(surfaceHolder.getSurface());
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
                clientApi1.stopShow();
            }
        });
    }



    private void startClient(){
        mSvClient1.setVisibility(View.VISIBLE);
    }

    private void stopClient(){
        mSvClient1.setVisibility(View.INVISIBLE);
    }



    /*
    ClientApi clientApi2;
    private void initClient2(){
        // 自动开始  鲁棒性
        clientApi2 = ClientApi.getInstance();
        clientApi2.init(MainActivity.this);
        clientApi2.autoConnectServer();
        clientApi2.setStateChangeCallBack(mClientApiCb);

        mSvClient2.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                clientApi2.startShow(surfaceHolder.getSurface());
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
                clientApi2.stopShow();
            }
        });
    }

    private void startClient2(){
        mSvClient2.setVisibility(View.VISIBLE);
    }

    private void stopClient2(){
        mSvClient2.setVisibility(View.INVISIBLE);
    }

     */



    IClientApi.IClientStateChangeCallBack mClientApiCb = new IClientApi.IClientStateChangeCallBack(){

        @Override
        public void onRegFind() {
            Log.d(TAG, "onRegFind() called");
            mClientInfo.clientState = "onRegFind";
            updateInfo();
        }

        @Override
        public void onFindServer() {
            Log.d(TAG, "onFindServer() called");
            mClientInfo.clientState = "onFindServer";
            updateInfo();
        }

        @Override
        public void onServerConnected(String clientName) {
            Log.d(TAG, "onServerConnected() called");
            mClientInfo.clientState = "onServerConnected";
            mClientInfo.clientName = clientName;
            updateInfo();
        }

        @Override
        public void onServerDisConnected() {
            Log.d(TAG, "onServerDisConnected() called");
            mClientInfo.clientState = "onServerDisConnected";
            updateInfo();
        }

        @Override
        public void onVideoStart() {
            Log.d(TAG, "onVideoStart() called");
            mClientInfo.videoState = "onVideoStart";
            updateInfo();
        }

        @Override
        public void onVideoStop() {
            Log.d(TAG, "onVideoStop() called");
            mClientInfo.videoState = "onVideoStop";
            updateInfo();
        }
    };
}