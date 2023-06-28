package com.thunder.common.lib.transf;

import com.thunder.common.lib.dto.Beans;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public interface ITransf {
    void begin();

    void init(Socket soc,ISocDealCallBack cb);

    interface ISocDealCallBack {
        void onGotOus(OutputStream ous, String local);
        void onSocClosed(Socket socket, OutputStream ous);
        void onGotVideoMsg(Beans.VideoData msg, OutputStream ous);
        void onGotJsonMsg(Beans.TransfPkgMsg msg, OutputStream ous);
    }

}
