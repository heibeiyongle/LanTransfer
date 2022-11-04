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
//        initEncoder();
        mEncodeT = new EncodeThread();
        mEncodeT.start();
        // debug code 没有播放器sdk 的数据源，mock it !
        if (mNotify != null) {
            mNotify.onServerReady();
        }

//        VideoDecoder videoDecoder = new VideoDecoder();
//        videoDecoder.startRead("/data/local/tmp/big_buck_bunny.h264",videoQue);
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
        return surface;
    }

    IStateCallBack mNotify;

    @Override
    public void setStateCallBack(IStateCallBack cb) {
        mNotify = cb;
    }

    EncodeThread mEncodeT = null;
    MediaCodec encoder = null;
    Surface surface = null;
//
//    // parameters for the encoder
//    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
//    private static final int FRAME_RATE = 15;               // 15fps
//    private static final int IFRAME_INTERVAL = 10;          // 10 seconds between I-frames

    private void initEncoder() {
        int videoW, videoH;

        MediaCodecInfo codecInfo = selectCodec(MediaFormat.MIMETYPE_VIDEO_AVC);
        if (codecInfo == null) {
            // Don't fail CTS if they don't have an AVC codec (not here, anyway).
            Log.e(TAG, "Unable to find an appropriate codec for " + MediaFormat.MIMETYPE_VIDEO_AVC);
            return;
        }
        Log.i(TAG, "found codec: " + codecInfo.getName());
//        try {
//            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        videoW = 1280;
        videoH = 720;

        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, videoW, videoH);

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 6000000);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);

        // Create a MediaCodec for the desired codec, then configure it as an encoder with
        // our desired properties.
        try {
            encoder = MediaCodec.createByCodecName(codecInfo.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
//        surface = encoder.createInputSurface();

//        MediaFormat format = MediaFormat.createVideoFormat(
//                MediaFormat.MIMETYPE_VIDEO_AVC, videoW, videoH);
//        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
//        format.setInteger(MediaFormat.KEY_BIT_RATE, 1000*8*1024);
////                format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
////                format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ); // 3568 crash
//        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
//        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1); //关键帧间隔时间 单位s
//        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
//        surface = encoder.createInputSurface();

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
    int generateIndex = 0;
    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding

    class EncodeThread extends Thread {

        int videoW = 1280, videoH = 720;
        MediaCodec.BufferInfo mBufInfo = new MediaCodec.BufferInfo();
        long outTimeOutUs = 1000 * 3;

        boolean exit = false;

        public void exit() {
            exit = true;
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void run() {
            super.run();
            generateVideoData();
        }


        int generateIndex = 0;

        private void generateVideoData() {
            final int TIMEOUT_USEC = 10000;

            int outputCount = 0;

            MediaCodecInfo codecInfo = selectCodec(MIME_TYPE);
            if (codecInfo == null) {
                // Don't fail CTS if they don't have an AVC codec (not here, anyway).
                Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
                return;
            }
            Log.i(TAG, "found codec: " + codecInfo.getName());

            // We avoid the device-specific limitations on width and height by using values that
            // are multiples of 16, which all tested devices seem to be able to handle.
            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);

            // Set some properties.  Failing to specify some of these can cause the MediaCodec
            // configure() call to throw an unhelpful exception.
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 60000);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
            Log.i(TAG, "format: " + format);

            // Create a MediaCodec for the desired codec, then configure it as an encoder with
            // our desired properties.
            try {
                encoder = MediaCodec.createByCodecName(codecInfo.getName());
            } catch (IOException e) {
                e.printStackTrace();
            }
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);


            Runnable gen = new Runnable() {
                @Override
                public void run() {
                    Surface surface = encoder.createInputSurface();
                    Log.i(TAG, "run: surface: " + surface);
                    InputSurface inputSurface = new InputSurface(surface);
                    inputSurface.makeCurrent();
                    while (true) {
                        generateSurfaceFrame(generateIndex);
                        inputSurface.setPresentationTime(computePresentationTime(generateIndex) * 1000);
                        Log.i(TAG, "inputSurface swapBuffers");
                        inputSurface.swapBuffers();
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        generateIndex++;
                    }
                }
            };
            new Thread(gen).start();

            try {
                Thread.sleep(2000);
                Log.i(TAG, "encoder start ! ");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            encoder.start();
            ByteBuffer[] encoderOutputBuffers = encoder.getOutputBuffers();
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

            // Loop until the output side is done.
            boolean inputDone = false;
            boolean outputDone = false;
            byte[] tmp = new byte[1024 * 1024 * 10];
            boolean isFirstFrame = true;
            while (!outputDone) {
//            if (VERBOSE) Log.i(TAG, "gen loop");

                // If we're not done submitting frames, generate a new one and submit it.  The
                // eglSwapBuffers call will block if the input is full.
//            if (!inputDone) {
//                if (generateIndex == NUM_FRAMES) {
//                    // Send an empty frame with the end-of-stream flag set.
//                    if (VERBOSE) Log.i(TAG, "signaling input EOS");
//                    if (WORK_AROUND_BUGS) {
//                        // Might drop a frame, but at least we won't crash mediaserver.
//                        try { Thread.sleep(500); } catch (InterruptedException ie) {}
//                        outputDone = true;
//                    } else {
//                        encoder.signalEndOfInputStream();
//                    }
//                    inputDone = true;
//                } else {
//                    generateSurfaceFrame(generateIndex);
//                    inputSurface.setPresentationTime(computePresentationTime(generateIndex) * 1000);
//                    if (VERBOSE) Log.i(TAG, "inputSurface swapBuffers");
//                    inputSurface.swapBuffers();
//                }
//                generateIndex++;
//            }

                // Check for output from the encoder.  If there's no output yet, we either need to
                // provide more input, or we need to wait for the encoder to work its magic.  We
                // can't actually tell which is the case, so if we can't get an output buffer right
                // away we loop around and see if it wants more input.
                //
                // If we do find output, drain it all before supplying more input.


                while (true) {
                    int outBufIndex = encoder.dequeueOutputBuffer(info, 10000);
//                    Log.i(TAG, "run -----1 : outBufIndex: "+outBufIndex);
                    if (outBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
//                        Log.i(TAG, "run: MediaCodec.INFO_TRY_AGAIN_LATER");
                        continue;
                    } else if (outBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        Log.i(TAG, "run: MediaCodec.INFO_OUTPUT_FORMAT_CHANGED");
                        MediaFormat newFormat = encoder.getOutputFormat();
                        Log.i(TAG, "encoder output format changed: " + newFormat);
                        continue;
                    } else if (outBufIndex < 0) {
                        Log.i(TAG, "run: outBufIndex: " + outBufIndex);
                        continue;
                    }
                    Log.i(TAG, "run: outBufIndex: " + outBufIndex);
                    if (isFirstFrame && mNotify != null) {
                        mNotify.onGenerateFirstFrame();
                        isFirstFrame = false;
                    }

                    ByteBuffer bf = encoder.getOutputBuffer(outBufIndex);
                    int dataSize = bf.remaining();
                    bf.get(tmp, 0, dataSize);
                    boolean isKeyFrame = info.flags == BUFFER_FLAG_KEY_FRAME;
                    boolean isConfigFrame = info.flags == BUFFER_FLAG_CODEC_CONFIG;
                    byte[] tmpDest = Arrays.copyOfRange(tmp, 0, dataSize);
//                    Log.i(TAG, " encode h264: "+Arrays.toString(Arrays.copyOfRange(tmpDest,0,100)));
//                    Log.i(TAG, " encode h264: 100-end"+ Arrays.toString(Arrays.copyOfRange(tmpDest,tmpDest.length-100 < 0 ? 0:tmpDest.length-100 ,tmpDest.length)));
                    Beans.VideoData tmpDataObj = new Beans.VideoData(isConfigFrame, isKeyFrame, videoW, videoH,
                            tmpDest);

                    if (isKeyFrame) {
                        Log.i(TAG, " ENCODE BUFFER_FLAG_KEY_FRAME");
                    } else if (isConfigFrame) {
                        Log.i(TAG, " ENCODE BUFFER_FLAG_CODEC_CONFIG");
                    }
                    videoQue.offer(tmpDataObj);
                    Log.i(TAG, " ============= on got frame ! ");
                    encoder.releaseOutputBuffer(outBufIndex, false);
                }
//                if (mNotify != null) {
//                    mNotify.onServerStopped();
//                }
            }
            videoQue.clear();
            // One chunk per frame, plus one for the config data.
            Log.i(TAG, "generateVideoData: assertEquals: NUM_FRAMES + 1: outputCount: " + outputCount);
//        assertEquals("Frame count", NUM_FRAMES + 1, outputCount);
        }


    }

    private void setInputSurface(Surface sink) {
        Log.i(TAG, "onGotSurface: surface: " + sink);
        VideoDecoder videoDecoder = new VideoDecoder();
        videoDecoder.setSurface(sink);
        videoDecoder.startRead("/data/local/tmp/big_buck_bunny.h264");
    }

    int mWidth = 1280, mHeight = 720;

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
    private void generateSurfaceFrame(int frameIndex) {
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
