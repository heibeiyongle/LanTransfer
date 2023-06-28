package com.thunder.lantransfserver.test;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.thunder.lantransfserver.IMyAidlInterface;
import com.thunder.lantransfserver.R;
import com.thunder.lantransfserver.test.service.context.MyContext;

public class BinderTestAct extends FragmentActivity {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_rebinder);
        MyContext myContext = new MyContext(this);
        findViewById(R.id.tv_bind_service).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent bindIntent = new Intent();
                myContext.bindService(bindIntent,connection,0);
            }
        });

        findViewById(R.id.btn_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addAction();
            }
        });
    }

    private static final String TAG = "BinderTestAct";
    IMyAidlInterface targetS;
    final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            targetS = IMyAidlInterface.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            targetS = null;
        }

        @Override
        public void onBindingDied(ComponentName name) {
            ServiceConnection.super.onBindingDied(name);
        }
    };

    private void addAction(){
        if(targetS == null){
            return;
        }
        try {
            Log.i(TAG, "onServiceConnected: before add");
            int res = targetS.add(1, 2);
            Log.i(TAG, "onServiceConnected: after add, res: "+res);
        }catch (Exception e){
            Log.e(TAG, "onServiceConnected: ", e);
        }
    }

}
