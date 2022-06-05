package com.thunder.lantransfserver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
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


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    SurfaceView mSvClient1;
    SurfaceView mSvClient2;
    TextView mTvInfo;
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
            if(view instanceof Button){
                mTvInfo.setText(((Button)view).getText()+" clicked ");
            }
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
        }

        @Override
        public void onMediaPublishStateChanged(int state) {
            Log.d(TAG, "onMediaPublishStateChanged() called with: state = [" + state + "]");
        }

        @Override
        public void onGotClient(String clientName) {
            Log.d(TAG, "onGotClient() called with: clientName = [" + clientName + "]");
        }

        @Override
        public void onLossClient(String clientName) {
            Log.d(TAG, "onLossClient() called with: clientName = [" + clientName + "]");
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
        }

        @Override
        public void onFindServer() {
            Log.d(TAG, "onFindServer() called");
        }

        @Override
        public void onServerConnected() {
            Log.d(TAG, "onServerConnected() called");
        }

        @Override
        public void onServerDisConnected() {
            Log.d(TAG, "onServerDisConnected() called");
        }

        @Override
        public void onConnectStateChanged(int state) {
            Log.d(TAG, "onConnectStateChanged() called with: state = [" + state + "]");
        }

        @Override
        public void onConnectServer(String clientName) {
            Log.d(TAG, "onConnectServer() called with: clientName = [" + clientName + "]");
        }

        @Override
        public void onVideoStart() {
            Log.d(TAG, "onVideoStart() called");
        }

        @Override
        public void onVideoStop() {
            Log.d(TAG, "onVideoStop() called");
        }
    };
}