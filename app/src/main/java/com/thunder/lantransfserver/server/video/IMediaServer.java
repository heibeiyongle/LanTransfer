package com.thunder.lantransfserver.server.video;

import android.content.Context;

import java.util.concurrent.ArrayBlockingQueue;

public interface IMediaServer {

    /**
     * 1. call transf to init
     * 2.start encode thread, set surface to sdk --thread1
     *
     */
    void startPublish(Context context, ArrayBlockingQueue<Object> videoQue);
    void stopPublish();
}
