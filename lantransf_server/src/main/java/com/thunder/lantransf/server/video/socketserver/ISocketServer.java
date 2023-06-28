package com.thunder.lantransf.server.video.socketserver;

import com.thunder.common.lib.dto.Beans;

/**
 * Created by zhe on 2022/6/19 22:52
 *
 * @desc:
 */
public interface ISocketServer {

    void startServer(int port,IServerSocCb serverSocCb);

    interface IServerSocCb{
        void onInitSuc(int port);
        void onStopped();

        void onGotClient(String clientName, String remoteHost);
        void onLostClient(String clientInfo);

        void onGotCmdMsg(String clientInfo, Beans.TransfPkgMsg msg);
//        void onGotVideoMsg(String clientInfo, Beans.VideoData msg);
    }

    boolean publishMsg(Beans.TransfPkgMsg msg);
    boolean publishVideo(Beans.VideoData video);

}
