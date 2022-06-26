package com.thunder.common.lib.transf.nio;

import android.util.Log;

/**
 * Created by zhe on 2022/6/26 22:04
 *
 * @desc:
 */
public class AbsSocketCommon {

    protected long sendSpeed = 0;
    protected long recSpeed = 0;
    protected long sendLastSecond = 0;
    protected void printSpeed(String tag , int sendSize,int recSize){
        if(sendSize > 0){
            sendSpeed += sendSize;
        }
        if(recSize > 0){
            recSpeed += recSize;
        }
        if(System.currentTimeMillis()/1000 != sendLastSecond){
            Log.i(tag, String.format(" [[--Speed--]] |---> %4d kB/s  |<--- %4d kB/s",sendSpeed/1024, recSpeed/1024));
            recSpeed = 0;
            sendSpeed = 0;
            sendLastSecond = System.currentTimeMillis()/1000;
        }
    }


}
