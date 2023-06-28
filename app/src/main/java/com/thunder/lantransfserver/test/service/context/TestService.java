package com.thunder.lantransfserver.test.service.context;

import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import com.thunder.lantransfserver.IMyAidlInterface;

public class TestService extends android.app.Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        IMyAidlInterface res = new IMyAidlInterface.Stub() {
            @Override
            public void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString) throws RemoteException {

            }

            @Override
            public int add(int a, int b) throws RemoteException {
                return 0;
            }

            @Override
            public void sendMsg(String msg) throws RemoteException {

            }
        };

//        IMyAidlInterface.Stub tmp = (IMyAidlInterface.Stub) res;
//
//        tmp.onTransact()

        return res.asBinder();
    }





}
