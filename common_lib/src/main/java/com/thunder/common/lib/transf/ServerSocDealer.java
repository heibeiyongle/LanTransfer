package com.thunder.common.lib.transf;


import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerSocDealer {
    private static final String TAG = "ServerSocDealer";

    public void startServer(IServerSocketCallBack scb){
        Thread thread = new Thread(){
            @Override
            public void run() {
                super.run();
                initServerSocket(scb);
            }
        };
        thread.start();
    }

    private int initServerSocket(IServerSocketCallBack scb){
        int port = -1;
        try {
            ServerSocket serverSocket = new ServerSocket(0);
            port = serverSocket.getLocalPort();
            if(scb != null){
                scb.onServerBindSuc(port);
            }
            Log.i(TAG,"initSocket port:"+port);
            while (!serverSocket.isClosed()){
                Socket socket = serverSocket.accept();
                if(scb != null){
                    scb.onGotClient(socket);
                }
            }
        } catch (IOException e) {
            Log.e(TAG,e.getMessage());
        }
        return port;
    }

    public interface IServerSocketCallBack{
        void onGotClient(Socket socket);
        void onServerBindSuc(int port);
    }


}
