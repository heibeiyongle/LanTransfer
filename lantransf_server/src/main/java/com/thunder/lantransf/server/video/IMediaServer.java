package com.thunder.lantransf.server.video;

import android.content.Context;
import android.view.Surface;

import java.util.concurrent.ArrayBlockingQueue;

interface IMediaServer {

    /**
     * 1. call transf to init
     * 2.start encode thread, set surface to sdk --thread1
     *
     */
    void startPublish(Context context, ArrayBlockingQueue<Object> videoQue);
    void stopPublish();
    Surface getCurrSUrface();
}
