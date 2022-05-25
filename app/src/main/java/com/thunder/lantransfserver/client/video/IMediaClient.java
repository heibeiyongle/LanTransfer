package com.thunder.lantransfserver.client.video;

import android.content.Context;
import android.view.Surface;

public interface IMediaClient {

    void init(Context context);
    /**
     * connect to server
     */
    void connectServer();

    void startShow(Surface surface);
    void stopShow();

    void onPlayBtnClick();
    void onAccBtnClick();

    void setStateChangeCallBack(IStateChangeCallBack cb);

    interface IStateChangeCallBack{
        void onVideoStart();
        void onVideoStop();
        void onPlayStateChanged(boolean play);
        void onAccStateChanged(int type);
    }


}
