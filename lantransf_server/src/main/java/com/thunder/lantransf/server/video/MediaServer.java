package com.thunder.lantransf.server.video;

import static android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
import static android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import com.thunder.common.lib.dto.Beans;
import com.thunder.common.lib.util.VideoDecoder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

class MediaServer implements IMediaServer{
    private static final String TAG = "MediaServer";
    /**
     * 1. call transf to init
     * 2.start encode thread, set surface to sdk --thread1
     *
     */

    Context mCtx;
    @Override
    public void startPublish(Context context,ArrayBlockingQueue<Object> videoQue) {
        Log.d(TAG, "start() called with: context = [" + context + "]");
        mCtx = context;
        if(mEncodeT != null){
            Log.i(TAG, "startPublish: encode running, ignore! ");
            return;
        }
        this.videoQue = videoQue;
        initEncoder();
//        mEncodeT = new EncodeThread();
//        mEncodeT.start();
        // debug code 没有播放器sdk 的数据源，mock it !
        VideoDecoder videoDecoder = new VideoDecoder();
        videoDecoder.startRead("/data/local/tmp/big_buck_bunny.h264",videoQue);
    }

    @Override
    public void stopPublish() {
        // todo
        if(mEncodeT == null){
            Log.i(TAG, "stopPublish: encode not-run, ignore! ");
            return;
        }
        mEncodeT.exit();
        mEncodeT = null;
    }

    @Override
    public Surface getCurrSUrface() {
        return surface;
    }

    EncodeThread mEncodeT = null;
    MediaCodec encoder = null;
    Surface surface = null;

    private void initEncoder(){
        int videoW,videoH;
        try {
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        } catch (IOException e) {
            e.printStackTrace();
        }
        videoW = 1920;
        videoH = 1080;
        MediaFormat format = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC, videoW, videoH);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 1000*8*1024);
//                format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
//                format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ); // 3568 crash
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1); //关键帧间隔时间 单位s
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        surface = encoder.createInputSurface();

    }


    ArrayBlockingQueue<Object> videoQue = null;
    class EncodeThread extends Thread{

        int videoW,videoH;
        MediaCodec.BufferInfo mBufInfo = new MediaCodec.BufferInfo();
        long outTimeOutUs = 1000*3;

        boolean exit = false;
        public void exit(){
            exit = true;
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void run() {
            super.run();
            Log.d(TAG, "run() called");
            try {
//                MediaCodec encoder = null;
//                encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
//                videoW = 1920;
//                videoH = 1080;
//                MediaFormat format = MediaFormat.createVideoFormat(
//                        MediaFormat.MIMETYPE_VIDEO_AVC, videoW, videoH);
//                format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
//
//                format.setInteger(MediaFormat.KEY_BIT_RATE, 1000*8*1024);
////                format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
//
////                format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ); // 3568 crash
//                format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
//                format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1); //关键帧间隔时间 单位s
//                encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
//
//                Surface surface = encoder.createInputSurface();
                encoder.start();
                setInputSurface(surface);
//                File saveFile = new File(DirConstants.DATA_CACHE+"/tmp.h264");
//                Log.i(TAG, "run: path: "+saveFile.getPath());
//                if(saveFile.exists()){
//                    saveFile.delete();
//                }
//                saveFile.createNewFile();

                byte[] tmp = new byte[1048576];
                while (!exit){
                    int outBufIndex = encoder.dequeueOutputBuffer(mBufInfo,1000*1000*2);
                    Log.i(TAG, "run -----1 : outBufIndex: "+outBufIndex);
                    if(outBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER){
                        continue;
                    }else if(outBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                        Log.i(TAG, "run: MediaCodec.INFO_OUTPUT_FORMAT_CHANGED");
                        continue;
                    }else if(outBufIndex < 0){
                        Log.i(TAG, "run: outBufIndex: "+ outBufIndex);
                        continue;
                    }
                    Log.i(TAG, "run: outBufIndex: "+outBufIndex);

                    ByteBuffer bf = encoder.getOutputBuffer(outBufIndex);
                    int dataSize = bf.remaining();
                    bf.get(tmp,0,dataSize);
                    boolean isKeyFrame = mBufInfo.flags ==  BUFFER_FLAG_KEY_FRAME;
                    boolean isConfigFrame = mBufInfo.flags == BUFFER_FLAG_CODEC_CONFIG;
                    byte[] tmpDest = Arrays.copyOfRange(tmp,0,dataSize);
//                    Log.i(TAG, " encode h264: "+Arrays.toString(Arrays.copyOfRange(tmpDest,0,100)));
                    Log.i(TAG, " encode h264: -100-end"+ Arrays.toString(Arrays.copyOfRange(tmpDest,tmpDest.length-100 < 0 ? 0:tmpDest.length-100 ,tmpDest.length)));
                    Beans.VideoData tmpDataObj = new Beans.VideoData(isConfigFrame,isKeyFrame,videoW,videoH,
                          tmpDest );

                    if(isKeyFrame){
                        Log.i(TAG, " ENCODE BUFFER_FLAG_KEY_FRAME queSize:"+videoQue.size()+", video-data: "+tmpDataObj);
                    }else if(isConfigFrame){
                        Log.i(TAG, " ENCODE BUFFER_FLAG_CODEC_CONFIG queSize:"+videoQue.size()+", video-data: "+tmpDataObj);
                    }
                    videoQue.offer(tmpDataObj);
                    encoder.releaseOutputBuffer(outBufIndex,true);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            // todo
//            ServiceManager.getSongOrderService().setSecondSurface(null);
            videoQue.clear();
            Log.d(TAG, "run() end ");
        }
    }

    private void setInputSurface(Surface sink){
        Log.i(TAG, "onGotSurface: surface: "+sink);
        VideoDecoder videoDecoder = new VideoDecoder();
        videoDecoder.setSurface(sink);
        videoDecoder.startRead("/data/local/tmp/big_buck_bunny.h264");
    }


}
