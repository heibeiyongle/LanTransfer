package com.thunder.lantransfserver.test.service;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.thunder.lantransfserver.IMyAidlInterface;

public class Client {




    ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

//            IMyAidlInterface.Stub stub = (IMyAidlInterface.Stub) service;
////            stub.onTransact() // 向 replay 写入数据

            // 以下为msg 打包的过程
            IMyAidlInterface myAidlInterface = IMyAidlInterface.Stub.asInterface(service);

            try {
                myAidlInterface.add(1,2);
            } catch (RemoteException e) {
                e.printStackTrace();
            }


        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };


}
