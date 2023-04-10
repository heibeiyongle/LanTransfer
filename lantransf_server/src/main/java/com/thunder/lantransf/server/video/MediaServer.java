package com.thunder.lantransf.server.video;

import static android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
import static android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.opengl.GLES20;
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
import java.util.concurrent.atomic.AtomicBoolean;

class MediaServer implements IMediaServer {
    private static final String TAG = "MediaServer";
    /**
     * 1. call transf to init
     * 2.start encode thread, set surface to sdk --thread1
     */

    Context mCtx;

    @Override
    public void startPublish(Context context, ArrayBlockingQueue<Object> videoQue) {
        Log.d(TAG, "start() called with: context = [" + context + "]");
        mCtx = context;
        if (mEncodeT != null) {
            Log.i(TAG, "startPublish: encode running, ignore! ");
            return;
        }
        this.videoQue = videoQue;
        initEncoder(1280,720);
        mSurface = encoder.createInputSurface();
//        startGenVideo(mSurface,1280,720);
        mEncodeT = new EncodeThread();
        mEncodeT.start();
        if (mNotify != null) {
            mNotify.onServerReady();
        }
    }

    @Override
    public void stopPublish() {
        // todo
        if (mEncodeT == null) {
            Log.i(TAG, "stopPublish: encode not-run, ignore! ");
            return;
        }
        mEncodeT.exit();
        mEncodeT = null;
    }

    @Override
    public Surface getCurrSUrface() {
        return mSurface;
    }

    IStateCallBack mNotify;

    @Override
    public void setStateCallBack(IStateCallBack cb) {
        mNotify = cb;
    }

    EncodeThread mEncodeT = null;
    MediaCodec encoder = null;
    Surface mSurface = null;

    private void initEncoder(int vw, int vh) {
        MediaCodecInfo codecInfo = selectCodec(MIME_TYPE);
        if (codecInfo == null) {
            // Don't fail CTS if they don't have an AVC codec (not here, anyway).
            Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
            return;
        }
        Log.i(TAG, "found codec: " + codecInfo.getName());

        // We avoid the device-specific limitations on width and height by using values that
        // are multiples of 16, which all tested devices seem to be able to handle.
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, vw, vh);
        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 1024*1024*8);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        Log.i(TAG, " format: " + format);

        try {
            encoder = MediaCodec.createByCodecName(codecInfo.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    }

    /**
     * Returns the first codec capable of encoding the specified MIME type, or null if no
     * match was found.
     */
    private static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {
                continue;
            }

            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                Log.i(TAG, "selectCodec: types[j]: " + types[j] + ", j: " + j);
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    ArrayBlockingQueue<Object> videoQue = null;
    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    class EncodeThread extends Thread {

        int videoW = 1280, videoH = 720;
        boolean exit = false;
        public void exit() {
            exit = true;
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void run() {
            super.run();
            encodeVideoData();
        }

        private void encodeVideoData() {
            Log.i(TAG, " encodeVideoData start ");
            encoder.start();
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean isFirstFrame = true;
            int encodeFrameCnt = 0;
            while (!exit) {
                while (true) {
                    int outBufIndex = encoder.dequeueOutputBuffer(info, 10000);
                    if (outBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
//                        Log.i(TAG, "run: MediaCodec.INFO_TRY_AGAIN_LATER");
                        continue;
                    } else if (outBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        Log.i(TAG, "run: MediaCodec.INFO_OUTPUT_FORMAT_CHANGED");
                        MediaFormat newFormat = encoder.getOutputFormat();
                        videoW = newFormat.getInteger(MediaFormat.KEY_WIDTH);
                        videoH = newFormat.getInteger(MediaFormat.KEY_HEIGHT);
                        Log.i(TAG, "encoder output format changed: w*h: "+videoW+"x"+videoH+" format:" + newFormat);
                        continue;
                    } else if (outBufIndex < 0) {
                        Log.i(TAG, "outBufIndex <0 , value: " + outBufIndex);
                        continue;
                    }
                    encodeFrameCnt++;
//                    Log.i(TAG, "outBufIndex: " + outBufIndex);
                    if (isFirstFrame && mNotify != null) {
                        mNotify.onGenerateFirstFrame();
                        isFirstFrame = false;
                    }
                    ByteBuffer bf = encoder.getOutputBuffer(outBufIndex);
                    Beans.VideoData tmpDataObj = genVideoData(info,bf,videoW,videoH);
                    if(tmpDataObj != null){
                        videoQue.offer(tmpDataObj);
                    }
                    if(encodeFrameCnt%10 == 0){
                        Log.i(TAG, " ============= on got frame ! encodeFrameCnt: "+encodeFrameCnt);
                    }
                    encoder.releaseOutputBuffer(outBufIndex, false);
                }
            }
            if (mNotify != null) {
                mNotify.onServerStopped();
            }
            videoQue.clear();
        }

    }

    private Beans.VideoData genVideoData(MediaCodec.BufferInfo info, ByteBuffer byteBuffer,int videoW, int videoH){
        int dataSize = byteBuffer.remaining();
//        Log.i(TAG, "genVideoData: bufferDataSize: "+dataSize);
        if(dataSize < 0){
            return null;
        }
        byte[] dataArr = new byte[dataSize];
        byteBuffer.get(dataArr);
        boolean isKeyFrame = info.flags == BUFFER_FLAG_KEY_FRAME;
        boolean isConfigFrame = info.flags == BUFFER_FLAG_CODEC_CONFIG;
//                    Log.i(TAG, " encode h264: "+Arrays.toString(Arrays.copyOfRange(tmpDest,0,100)));
//                    Log.i(TAG, " encode h264: 100-end"+ Arrays.toString(Arrays.copyOfRange(tmpDest,tmpDest.length-100 < 0 ? 0:tmpDest.length-100 ,tmpDest.length)));
        Beans.VideoData tmpDataObj = new Beans.VideoData(isConfigFrame, isKeyFrame, videoW, videoH,
                dataArr);
        if (isKeyFrame) {
            Log.i(TAG, " ENCODE BUFFER_FLAG_KEY_FRAME");
        } else if (isConfigFrame) {
            Log.i(TAG, " ENCODE BUFFER_FLAG_CODEC_CONFIG");
        }
        return tmpDataObj;
    }


    private static final int TEST_R0 = 0;                   // dull green background
    private static final int TEST_G0 = 136;
    private static final int TEST_B0 = 0;
    private static final int TEST_R1 = 0;                 // pink; BT.601 YUV {120,160,200}
    private static final int TEST_G1 = 0;
    private static final int TEST_B1 = 236;

    /**
     * Generates the presentation time for frame N, in microseconds.
     */
    private static long computePresentationTime(int frameIndex) {
        return 123 + frameIndex * 1000000 / 15;
    }


    /**
     * Generates a frame of data using GL commands.
     * <p>
     * We have an 8-frame animation sequence that wraps around.  It looks like this:
     * <pre>
     *   0 1 2 3
     *   7 6 5 4
     * </pre>
     * We draw one of the eight rectangles and leave the rest set to the zero-fill color.
     */
    private void generateSurfaceFrame(int mWidth, int mHeight, int frameIndex) {
        frameIndex %= 8;

        int startX, startY;
        if (frameIndex < 4) {
            // (0,0) is bottom-left in GL
            startX = frameIndex * (mWidth / 4);
            startY = mHeight / 2;
        } else {
            startX = (7 - frameIndex) * (mWidth / 4);
            startY = 0;
        }

        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
        GLES20.glClearColor(TEST_R0 / 255.0f, TEST_G0 / 255.0f, TEST_B0 / 255.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glScissor(startX, startY, mWidth / 4, mHeight / 2);
        GLES20.glClearColor(TEST_R1 / 255.0f, TEST_G1 / 255.0f, TEST_B1 / 255.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
    }

}
