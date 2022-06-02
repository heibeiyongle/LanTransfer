package com.thunder.lantransf.client.video;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;


import com.google.gson.internal.LinkedTreeMap;
import com.thunder.common.lib.bean.NetTimeInfo;
import com.thunder.common.lib.dto.Beans;
import com.thunder.common.lib.util.GsonUtils;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhe on 2022/3/19 23:17
 *
 * @desc:
 */
public class MediaClient implements IMediaClient{

    private static final String TAG = "MediaClient";

    private MediaClient(){}
    static class Holder{
        static MediaClient instance = new MediaClient();
    }
    public static MediaClient getInstance(){
        return Holder.instance;
    }

    
    ITransferClient mTransfClient = null;
    Context mCtx;
    Surface mSurface;

    DecodeThread mDecThread ;

    ArrayBlockingQueue<Beans.VideoData> mVideoQue = new ArrayBlockingQueue<>(100);
    IStateChangeCallBack mStateChangedCb;
    @Override
    public void init(Context context) {
        mCtx = context;
        mTransfClient = new TransfClient();
        mTransfClient.init(context);
        mTransfClient.setClientHandler(mRecDataHandler);
    }

    @Override
    public void connectServer() {
        // search and connect
        mTransfClient.connectServer();
    }

    @Override
    public void startShow(Surface surface) {
        Log.i(TAG, "startShow() called with: surface = [" + surface + "]");
        mVideoQue.clear();
        mSurface = surface;
        mDecThread = new DecodeThread();
        mDecThread.init(surface);
        mDecThread.start();

        mTransfClient.sendViewActive();
        mTransfClient.getAccState();
        mTransfClient.getPlayState();

    }

    @Override
    public void stopShow() {
        Log.i(TAG, "stopShow: ");
        mTransfClient.sendViewInActive();
        mSurface = null;
        mVideoQue.clear();
        if(mDecThread != null){
            mDecThread.exitLoop();
            mDecThread = null;
        }
    }

    @Override
    public void onPlayBtnClick() {
        mTransfClient.sendPlayBtnClick();
    }

    @Override
    public void onAccBtnClick() {
        mTransfClient.sendAccBtnClick();
    }

    @Override
    public void setStateChangeCallBack(IStateChangeCallBack cb) {
        mStateChangedCb = cb;
    }

    ITransferClient.IClientHandler mRecDataHandler = new ITransferClient.IClientHandler() {
        @Override
        public void onConnect() {
            mVideoQue.clear();
            //sync time , start msg
            mTransfClient.syncNetTime();
        }

        @Override
        public void onDisconnect() {
            mVideoQue.clear();
        }

        @Override
        public void onGotVideoData(Beans.VideoData data) {
//            Log.d(TAG, "onGotVideoData() called with: data = [" + data + "]");
            try {
                if(mSurface != null){
                    mVideoQue.offer(data,10, TimeUnit.MILLISECONDS);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onGotCmdData(Beans.CommandMsg data) {
            // cmd msg
            dealCmdMsg(data);
        }
    };


    private void dealCmdMsg(Beans.CommandMsg msg){
        Log.i(TAG, "dealCmdMsg: msg: "+msg);
        /*
         1. sync time res
            report client info
         2. play state res
         */
        if(Beans.CommandMsg.ResSyncTime.class.getSimpleName().equals(msg.getType())){
            // save it
            // report client info
            Beans.CommandMsg.ResSyncTime tmpMsg = GsonUtils.parseFromLinkedTreeMap(
                    (LinkedTreeMap) msg.getBody(), Beans.CommandMsg.ResSyncTime.class);
            dealSyncTimeRes(tmpMsg);
            mTransfClient.reportClientInfo("xxx",mNetTimeInfo.getTransfCostTime());

        }else if(Beans.CommandMsg.ResAccState.class.getSimpleName().equals(msg.getType())){
            Beans.CommandMsg.ResAccState tmpMsg = GsonUtils.parseFromLinkedTreeMap(
                    (LinkedTreeMap) msg.getBody(), Beans.CommandMsg.ResAccState.class);
            if(mStateChangedCb != null && tmpMsg != null){
                mStateChangedCb.onAccStateChanged(tmpMsg.accType);
            }
        }else if(Beans.CommandMsg.ResPlayState.class.getSimpleName().equals(msg.getType())){
            Beans.CommandMsg.ResPlayState tmpMsg = GsonUtils.parseFromLinkedTreeMap(
                    (LinkedTreeMap) msg.getBody(), Beans.CommandMsg.ResPlayState.class);
            if(mStateChangedCb != null && tmpMsg != null){
                mStateChangedCb.onPlayStateChanged(tmpMsg.playing);
            }
        }


    }

    NetTimeInfo mNetTimeInfo = null;

    private void dealSyncTimeRes(Beans.CommandMsg.ResSyncTime res){
        long startTime = res.req.clientTimeMs;
        long endTime = System.currentTimeMillis();
        long transfCostTime = (endTime - startTime)/2;
        long currNetTime = res.serverTimeMs + transfCostTime;

        Log.i(TAG, "dealSyncTimeRes (ms) currNetTime: "+currNetTime+" startTime:"+startTime+
                ", endTime: "+endTime+", transfCost: "+transfCostTime+" netTimeSrc: "+res.serverTimeMs );

        mNetTimeInfo = new NetTimeInfo();
        mNetTimeInfo.setNetTimeMs(currNetTime);


        mNetTimeInfo.setTransfCostTime(transfCostTime);



    }



    class DecodeThread extends Thread {
        private boolean mExit;
        boolean ifFirstFrameOuted = false;
        boolean isRunning = false;
        public DecodeThread(){
            ifFirstFrameOuted = false;
        }
        public void exitLoop() {
            mExit = true;
        }
        int DEF_GET_IN_BUF_TIMEOUT = 1000*3;
        int DEF_DECODE_TIMEOUT = 1000*3;

        Surface mSurf;
        void init(Surface surface){
            mSurf = surface;
        }

        @Override
        public void run() {
            Log.i(TAG, "DecodeThread begin run...");
            Beans.VideoData firstFrame = null;
            while (!mExit){
                firstFrame = getVideoData(5000);
                if (firstFrame != null) {
                    Log.i(TAG, " on got Video-Data! ");
                    break;
                }else {
                    Log.i(TAG, " waiting Video-Data! ");
                }
            }
            if(mExit){
                Log.i(TAG, "run: mExit = true! return;");
                return;
            }
            MediaCodec codec = null;
            try {
                codec = initMediaCodec(firstFrame.w, firstFrame.h,mSurf);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            boolean ifInputBufUsed = true;
            int mInputBufferId = -1;

            long lastS = 0;
            int fps = 0;

            boolean inputFirstFrame = false;

            try {
                MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
                isRunning = true;

//                File outTmpFile = new File(mCtx.getExternalCacheDir()+"/tmp.h264");
//                Log.i(TAG, "run: path: "+outTmpFile.getPath());
//                if(outTmpFile.exists()){
//                    outTmpFile.delete();
//                }
//                outTmpFile.createNewFile();
//                OutputStream out = new FileOutputStream(outTmpFile);


                while (!mExit) {
                    if (ifInputBufUsed) {
                        mInputBufferId = codec.dequeueInputBuffer(DEF_GET_IN_BUF_TIMEOUT);
                    }
                    // input data
                    if (mInputBufferId >= 0) {
                        ifInputBufUsed = false;
                        if ( !mVideoQue.isEmpty() || firstFrame != null) {
                            ByteBuffer byteBuffer = getBuffer(codec, mInputBufferId);
                            byteBuffer.clear();
                            if (firstFrame != null) {
//                                out.write(firstFrame.h264Data);
                                byteBuffer.put(firstFrame.h264Data);
                                checkVideoFrameTime(firstFrame);
                                firstFrame = null;
                            } else {
                                Beans.VideoData videoData = getVideoData(3);
                                if (videoData != null) {
//                                    Log.i(TAG, " decode h264: 0-100 "+ Arrays.toString(Arrays.copyOfRange(videoData.h264Data,0,100)));
//                                    Log.i(TAG, " decode h264: -100-end"+ Arrays.toString(
//                                            Arrays.copyOfRange(videoData.h264Data,videoData.h264Data.length-100 < 0 ?0:videoData.h264Data.length-100,
//                                                    videoData.h264Data.length)));
                                    byteBuffer.put(videoData.h264Data);
                                    checkVideoFrameTime(videoData);
//                                    out.write(videoData.h264Data);
                                }
                            }
                            byteBuffer.flip();
                            if (byteBuffer.limit() > 0) {
                                ifInputBufUsed = true;
                                if(!inputFirstFrame){
                                    inputFirstFrame = true;
                                    Log.i(TAG," ---> INPUT FIRST FRAME!");
                                }
//                                Log.i(TAG, "run: input video data, bufferId: "+mInputBufferId);
                                codec.queueInputBuffer(mInputBufferId, 0, byteBuffer.limit(),
                                        0, MediaCodec.BUFFER_FLAG_KEY_FRAME);
                            }
                        }
                    }

                    if (mExit) {
                        break;
                    }
                    //decode data
                    int outIndex = codec.dequeueOutputBuffer(mBufferInfo, DEF_DECODE_TIMEOUT);
//                    Log.i(TAG, "run: outPut buffIndex : "+outIndex);
                    while (outIndex >= 0 && !mExit) {
                        if(!ifFirstFrameOuted){
                            Log.i(TAG," <---- OUTPUT FIRST FRAME!");
                            ifFirstFrameOuted = true;
                        }
                        codec.releaseOutputBuffer(outIndex, true);
                        outIndex = codec.dequeueOutputBuffer(mBufferInfo, DEF_DECODE_TIMEOUT);
                        long tmpS = System.currentTimeMillis() / 1000;
                        if (tmpS == lastS) {
                            fps++;
                        } else {
                            Log.i(TAG, "===== decode fps================:" + fps+", cacheCnt:"+mVideoQue.size());
                            fps = 0;
                            lastS = tmpS;
                        }
                    }

//                LogUtil.i(TAG, "loop_end ...");
                }
                isRunning = false;
            } catch (Exception e) {
                Log.e(TAG, "decode Err!===== ",e);
            }
            try {
                codec.stop();
                Log.i(TAG,"======= stop suc!");
            }catch (Exception e){
                Log.i(TAG," == CODEC STOP Err!============================  ",e);
            }
            try {
                codec.release();
                Log.i(TAG,"======= release suc!");
            }catch (Exception e){
                Log.i(TAG," == CODEC RELEASE Err!============================  ",e);
            }
            Log.i(TAG, "Decode Thread stop");
        }


        private ByteBuffer getBuffer(MediaCodec codec, int inputBufferId) {
            ByteBuffer inputBuffer;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                inputBuffer = codec.getInputBuffer(inputBufferId);
            } else {
                inputBuffer = codec.getInputBuffers()[inputBufferId];
            }
            return inputBuffer;
        }


        private MediaCodec initMediaCodec(final int videoW, final int videoH, Surface surface) throws Exception {
            Log.i(TAG, "initMediaCodec...start.");
            MediaCodec codec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            MediaFormat format = MediaFormat.createVideoFormat(
                    MediaFormat.MIMETYPE_VIDEO_AVC, videoW, videoH);
            codec.configure(format,surface, null, 0);
            codec.start();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                Log.i(TAG,"codec:"+codec.getName());
            }
            Log.i(TAG, "initMediaCodec end...");
            return codec;
        }

        private Beans.VideoData getVideoData(long waitMs){
            try {
                Beans.VideoData res = mVideoQue.poll(waitMs,TimeUnit.MILLISECONDS);
                if(res != null && (res.keyFrame || res.isConfigFrame)){
                    Log.i(TAG, " print-video-data: "+res);
                }
//                Log.i(TAG, "getVideoData videoData: "+ res);
                return res;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }


    }

    long mTmpGapMax = 0;
    long mTmpGapMin = Integer.MIN_VALUE;
    long mTmpAbsGapMax = 0;
    long mTmpAbsGapMin = Integer.MAX_VALUE;

    long mTmpGapTotal = 0;
    long mTmpGapCnt = 0;

    long mTmpGapLastSec = 0;
    private void checkVideoFrameTime(Beans.VideoData videoData){
        if(mNetTimeInfo == null){
            return;
        }
        long gap = videoData.frameTimeMs - mNetTimeInfo.getCurrNetTimeMs();
        if(gap > mTmpGapMax){
            mTmpGapMax = gap;
        }
        if(gap < mTmpGapMin){
            mTmpGapMin = gap;
        }
        long absGap = (gap<0 ? -gap: gap);
        if(absGap > mTmpAbsGapMax){
            mTmpAbsGapMax = absGap;
        }else if(absGap < mTmpAbsGapMin){
            mTmpAbsGapMin = absGap;
        }
        mTmpGapTotal += absGap;
        mTmpGapCnt ++;
        if(System.currentTimeMillis()/1000 != mTmpGapLastSec){
            Log.i(TAG, " VIDEO-TRANSF-COST(MS): GAP-avg: "+(1.0f*mTmpGapTotal/mTmpGapCnt)+
                    ", max: "+mTmpGapMax+", min: "+mTmpGapMin+", absMax: "+mTmpAbsGapMax+
                    ", absMin: "+mTmpAbsGapMin+", net-delay: "+mNetTimeInfo.getTransfCostTime());
            mTmpGapMax = 0;
            mTmpGapMin = 0;
            mTmpAbsGapMin = Integer.MAX_VALUE;
            mTmpAbsGapMax = 0;
            mTmpGapTotal = 0;
            mTmpGapCnt = 0;
            mTmpGapLastSec = System.currentTimeMillis()/1000;
        }
    }









}
