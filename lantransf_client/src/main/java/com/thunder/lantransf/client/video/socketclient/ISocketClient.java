package com.thunder.lantransf.client.video.socketclient;

import com.thunder.common.lib.dto.Beans;

/**
 * Created by zhe on 2022/6/19 22:52
 *
 * @desc:
 */
public interface ISocketClient {

    void connect(String host, int port , IClientSocCb serverSocCb);

    interface IClientSocCb{
        void onConnectSuc(String remoteHost);
        void onClosed();
        void onGotCmdMsg(Beans.TransfPkgMsg msg);
        void onGotVideoMsg(Beans.VideoData msg);
    }
    boolean sendMsg(Beans.TransfPkgMsg msg);
    boolean updateClientInfo(String clientName);
}
