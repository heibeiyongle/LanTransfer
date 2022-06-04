package com.thunder.lantransf.server.video;

import android.content.Context;

import com.thunder.common.lib.dto.Beans;

import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

interface ITransfServer {


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
    void setStateCallBack(ITransfServerStateCallBack cb);
    List<String> getClientList();

    interface IClientMsgDealer{
        void onGotCmd(Beans.CommandMsg msg, TransferServer.ClientSession session);
    }


    interface ITransfServerStateCallBack{
        void onSocketServerReady();
        void onServicePublished();
        void onSocketServerStopped();
        void onServiceCanceled();
        void onGotClient(TransferServer.ClientSession clientSession);
        void onLoseClient(TransferServer.ClientSession clientSession);
    }
}
