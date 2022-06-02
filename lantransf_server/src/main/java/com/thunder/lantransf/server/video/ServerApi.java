package com.thunder.lantransf.server.video;

import android.content.Context;
import android.view.Surface;

import java.util.Map;

public class ServerApi implements IServerApi {
    private ServerApi(){}

    public static ServerApi getInstance(){
        return ServerApi.Holder.instance;
    }
    private static class Holder{
        static ServerApi instance = new ServerApi();
    }

    @Override
    public boolean init(Context ctx) {
        ServerManager.getInstance().init(ctx);
        return true;
    }

    @Override
    public boolean setConfig(Map config) {
        return false;
    }

    @Override
    public void startServer() {
        ServerManager.getInstance().startServer();
    }

    @Override
    public Surface startPublishMedia() {
        ServerManager.getInstance().startPublishMedia();
        return ServerManager.getInstance().getCurrMediaServer().getCurrSUrface();
    }

    @Override
    public void stopPublishMedia() {
        ServerManager.getInstance().stopPublishMedia();
    }

    @Override
    public String[] getClientList() {
        return new String[0];
    }

    @Override
    public void setStateChangeCallBack(IServerStateChangeCallBack cb) {

    }
}
