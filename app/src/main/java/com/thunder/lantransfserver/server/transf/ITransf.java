package com.thunder.lantransfserver.server.transf;

import com.thunder.lantransfserver.server.dto.Beans;

import java.io.OutputStream;
import java.net.Socket;

public interface ITransf {
    void begin();

    void init(Socket soc,ISocDealCallBack cb);

    interface ISocDealCallBack {
        void onGotOus(OutputStream ous, String remote);
        void onSocClosed(Socket socket, OutputStream ous);
        void onGotVideoMsg(Beans.VideoData msg, OutputStream ous);
        void onGotJsonMsg(Beans.CommandMsg msg, OutputStream ous);
    }


}
