package com.thunder.lantransf.server.video;

import android.content.Context;

public interface IServerManager {

    void init(Context context);
    void startServer();
    void stopServer();
    void startPublishMedia();
    void stopPublishMedia();

    void publishPlayState(boolean play);
    void publishAccState(int acc);

}
