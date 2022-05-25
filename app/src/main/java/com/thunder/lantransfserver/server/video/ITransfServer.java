package com.thunder.lantransfserver.server.video;

import android.content.Context;

import com.thunder.lantransfserver.server.dto.Beans;

import java.io.OutputStream;
import java.util.concurrent.ArrayBlockingQueue;

public interface ITransfServer {


    /**
     * 1. start serverSocket-video --thread1
     *      1.reg nsd
     *      2.accept client
     *      3.thread read que, broad cast to client --thread2
     *          1. data define HEAD + PAYLOAD
     *              head: [keyframe][w][h][date-len]
     *              PAYLOAD: [h264]
     * 2. start serverSocket-cmd --delay to do
     *
     */


    void startTransfServer(Context context);
    void stopServer();
    void startPublishMsg();
    ArrayBlockingQueue<Object> getOutputQue();
    void updateClientSessionInfo(OutputStream clientOus, String name, long netDelay);
    void setMsgDealer(IClientMsgDealer dealer);

    interface IClientMsgDealer{
        void onGotCmd(Beans.CommandMsg msg, TransferServer.ClientSession session);
    }

}
