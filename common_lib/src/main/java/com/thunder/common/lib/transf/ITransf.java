package com.thunder.common.lib.transf;

import com.thunder.common.lib.dto.Beans;

import java.io.OutputStream;
import java.net.Socket;

public interface ITransf {
    void begin();

    void init(Socket soc,ISocDealCallBack cb);

    interface ISocDealCallBack {
        void onGotOus(OutputStream ous, String local);
        void onSocClosed(Socket socket, OutputStream ous);
        void onGotVideoMsg(Beans.VideoData msg, OutputStream ous);
        void onGotJsonMsg(Beans.CommandMsg msg, OutputStream ous);
    }


}
