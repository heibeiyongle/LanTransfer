package com.thunder.lantransfserver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.thunder.lantransf.client.video.IMediaClient;
import com.thunder.lantransf.client.video.MediaClient;
import com.thunder.lantransf.server.video.ServerManager;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    SurfaceView mSvClient ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSvClient = findViewById(R.id.sv_client);
        findViewById(R.id.tv_start_server).setOnClickListener(clk);
        findViewById(R.id.tv_start_publish_video).setOnClickListener(clk);
        findViewById(R.id.tv_start_client).setOnClickListener(clk);
        initClient();
    }


    View.OnClickListener clk = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()){
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
            }
        }
    };


    private void startServer(){
        ServerManager.getInstance().init(this);
        ServerManager.getInstance().startServer();
    }

    private void startPublish(){
        ServerManager.getInstance().startPublishMedia();
    }




    MediaClient mMediaClient;
    private void initClient(){
        mMediaClient = MediaClient.getInstance();
        mMediaClient.init(MainActivity.this);
        mMediaClient.connectServer();
        mSvClient.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
                mMediaClient.setStateChangeCallBack(changeCallBack);
                mMediaClient.startShow(surfaceHolder.getSurface());
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
                mMediaClient.stopShow();
                mMediaClient.setStateChangeCallBack(null);
            }
        });
    }
    private void startClient(){

    }

    IMediaClient.IStateChangeCallBack changeCallBack = new IMediaClient.IStateChangeCallBack() {
        @Override
        public void onVideoStart() {

        }

        @Override
        public void onVideoStop() {

        }

        @Override
        public void onPlayStateChanged(boolean play) {
            Log.i(TAG, "onPlayStateChanged: play: "+play);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                }
            });
        }

        @Override
        public void onAccStateChanged(int type) {
            Log.i(TAG, "onAccStateChanged: type: "+type);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                }
            });
        }
    };
}