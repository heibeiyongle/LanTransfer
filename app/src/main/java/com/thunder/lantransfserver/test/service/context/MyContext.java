package com.thunder.lantransfserver.test.service.context;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;

import com.thunder.lantransfserver.test.service.rebinder.RebinderProxy;

public class MyContext extends ContextWrapper {
    public MyContext(Context base) {
        super(base);
    }

    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        //wrap
        /**
         * 0.
         * when process start, reg server, reg client, connect socket
         * 1. bindService
         *      client bind
         *
         *      server rec bind msg
         *      server init service Instance
         *
         *      return bind suc
         *
         * 2.
         *
         */


        // post a task to connect socket
        RebinderProxy rebinderProxy = new RebinderProxy();
        ComponentName componentName = new ComponentName("","");
        conn.onServiceConnected(componentName,rebinderProxy);

        /*
        1. serverSocket accept socket
        2. handle msg
        3. serverSocket 如何广播消息？

         */



        return true;
    }
}
