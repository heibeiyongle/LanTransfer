package com.thunder.lantransfserver.test.service.rebinder;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.thunder.lantransfserver.IMyAidlInterface;

import java.io.FileDescriptor;

/**
 * make a binder to caller ,
 */

public class RebinderProxy implements IBinder {
    private static final String TAG = "RebinderProxy";
    @Nullable
    @Override
    public String getInterfaceDescriptor() throws RemoteException {
        return null;
    }

    @Override
    public boolean pingBinder() {
        return false;
    }

    @Override
    public boolean isBinderAlive() {
        return false;
    }

    @Nullable
    @Override
    public IInterface queryLocalInterface(@NonNull String descriptor) {
        return null;
    }

    @Override
    public void dump(@NonNull FileDescriptor fd, @Nullable String[] args) throws RemoteException {

    }

    @Override
    public void dumpAsync(@NonNull FileDescriptor fd, @Nullable String[] args) throws RemoteException {

    }

    @Override
    public boolean transact(int code, @NonNull Parcel data, @Nullable Parcel reply, int flags) throws RemoteException {
        /*
        将请求序列化，通过socket发送出去,
        将req-serial 添加到map
        同时根据是否oneway ,处理socket 的返回值
        根据req-serial 分发返回值
        */
        Log.i(TAG, "transact() called with: code = [" + code + "], data = [" + data + "], reply = [" + reply + "], flags = [" + flags + "]");
        res.asBinder().transact(code, data, reply, flags);

        data.marshall();

        /**
         * 将req 序列化以后，发送到服务端
         *
         * 服务端 调用Android 标准api bind service
         *
         * 将method 调用传递到transact
         *
          */

        Context context = null;

        context.bindService(new Intent(), new ServiceConnection(){

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
//                service.transact(code,data,reply,flags);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }

            @Override
            public void onBindingDied(ComponentName name) {
                ServiceConnection.super.onBindingDied(name);
            }

            @Override
            public void onNullBinding(ComponentName name) {
                ServiceConnection.super.onNullBinding(name);
            }
        } ,Context.BIND_AUTO_CREATE);


        /**
         *
         * 1. bind service
         * 2. 注册服务监听
         * 3. 发现服务
         * 4. 连接服务
         *
         *
         *
         *
         *
         * msg Obj
         * client :
         * function call
         *  p0 p1 p2 ...
         *  pack-1
         *  function-code, in, res, flags
         *  pack-2
         *  msg-object: v-function-code, v-in, v-res, v-flags
         *  pack-3
         *  msg-object to transfer-object: [head][payload]
         *
         *
         * service:
         *  read-data
         *  1. transfer-object to msg-object
         *  2. msg-object to service-onTransfer
         *  3. invoke real function
         *
         *  3-1. a-syn-call
         *  3-2. syn-call,
         *      1. block transact-function wait-socket-res
         *      2. on-socket-res, notify the wait-object
         *      3. count-down logic
         *
         *
         */




        return false;
    }


    IMyAidlInterface res = new IMyAidlInterface.Stub() {
        @Override
        public void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString) throws RemoteException {

        }

        @Override
        public int add(int a, int b) throws RemoteException {
            Log.i(TAG, "add: a: "+a +" b: "+b);
            return -1;
        }

        @Override
        public void sendMsg(String msg) throws RemoteException {

        }
    };

    @Override
    public void linkToDeath(@NonNull DeathRecipient recipient, int flags) throws RemoteException {
//        recipient.binderDied();
    }

    @Override
    public boolean unlinkToDeath(@NonNull DeathRecipient recipient, int flags) {
        return false;
    }
}
