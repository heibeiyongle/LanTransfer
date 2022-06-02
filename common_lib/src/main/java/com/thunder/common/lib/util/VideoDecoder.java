package com.thunder.common.lib.util;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import com.thunder.common.lib.dto.Beans;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by shuozhe on
 * email jiashuozhe@zjinnova.com
 */
public class VideoDecoder {
    private static final String TAG = "VideoDecoder";

    private final static int DEF_GET_IN_BUF_TIMEOUT = 100;
    private final static int DEF_DECODE_TIMEOUT = 3000;

    InputThread mDecodeThread;
    MediaCodec.BufferInfo mBufferInfo;
    //step1
    static String H264_FILE_PATH = "/sdcard/tmp/h264.data";

    private Surface mSurface;

    public void setSurface(Surface surface) {
        if (surface == null) {
            throw new RuntimeException("surface is null...");
        }
        mSurface = surface;
    }

    public VideoDecoder() {
        mBufferInfo = new MediaCodec.BufferInfo();
    }


    private void startDecode() {
        Log.i(TAG, "startDecode....mDecodeThread null" + (mDecodeThread == null));
        if (mDecodeThread == null) {
            mDecodeThread = new InputThread();
            mDecodeThread.start();
        }
    }

    public void startRead(String file) {
        H264_FILE_PATH = file;
        startDecode();
    }


    public void startRead(String file, ArrayBlockingQueue<Object> queue) {
        H264_FILE_PATH = file;
        try {
            FileInputStream inputStream = new FileInputStream(file);
            MyReadThread myReadThread = new MyReadThread(inputStream,queue,"");
            myReadThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private MediaCodec initMediaCodec() throws Exception {
        Log.d(TAG, "initMediaCodec() called");
        Log.i(TAG, " surface:" + mSurface.toString());
        MediaCodec codec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        MediaFormat format = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC, 1024, 600);
        codec.configure(format, mSurface, null, 0);
        codec.start();
        Log.i(TAG, "initMediaCodec end...");
        return codec;
    }


    class InputThread extends Thread {

        @Override
        public void run() {
            Log.i(TAG, "InputThread begin run...");
            MediaCodec codec = null;
            try {
                codec = initMediaCodec();
            } catch (Exception e) {
                Log.e(TAG, "err1");
                return;
            }

            boolean ifInputBufUsed = true;
            int mInputBufferId = -1;

            long lastS = 0;
            int fps = 0;

            File file = new File(H264_FILE_PATH);
            InputStream inputStream = null;
            boolean endOfData = false;

            int cnt = 0;
            try {
                inputStream = new FileInputStream(file);

                MyReadThread myReadThread = new MyReadThread(inputStream,frames);
                myReadThread.start();

            } catch (Exception e) {
                e.printStackTrace();
            }

            while (!endOfData) {
                try {
                    if (ifInputBufUsed) {
                        mInputBufferId = codec.dequeueInputBuffer(DEF_GET_IN_BUF_TIMEOUT);
                    }
                    // input data
                    if (mInputBufferId >= 0) {
                        ifInputBufUsed = false;
                        ByteBuffer byteBuffer = getBuffer(codec, mInputBufferId);
                        byteBuffer.clear();

                        // in ding wei t３, size must lt  byteBuffer.capacity()/2
                        byte[] buf = frames.take();
//                        Log.i(TAG, "read cnt:" + buf.length);
                        if (buf.length > 0) {
                            byteBuffer.put(buf, 0, buf.length);
                        }

                        byteBuffer.flip();
                        if (byteBuffer.limit() > 0) {
                            ifInputBufUsed = true;
                            codec.queueInputBuffer(mInputBufferId, 0, byteBuffer.limit(), 0, MediaCodec.BUFFER_FLAG_KEY_FRAME);
                        }

                    }

                    //decode data
                    int outIndex = codec.dequeueOutputBuffer(mBufferInfo, DEF_DECODE_TIMEOUT);
//                    Log.i(TAG," outIndex:"+outIndex);
                    while (outIndex >= 0) {
                        codec.releaseOutputBuffer(outIndex, true);
                        outIndex = codec.dequeueOutputBuffer(mBufferInfo, DEF_DECODE_TIMEOUT);
                        long tmpS = System.currentTimeMillis() / 1000;
                        if (tmpS == lastS) {
                            fps++;
                        } else {
                            Log.i(TAG, "===== decode ================ FPS:" + fps + " second:" + (++cnt)+", queSize:"+frames.size());
                            fps = 0;
                            lastS = tmpS;
                        }
                    }

                    if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                        Log.i(TAG, "end of data!");
                        break;
                    }

                } catch (Exception e) {
                    Log.e(TAG, "err2");
                    mDecodeThread = null;
                    break;
                }
            }
            Log.i(TAG, " =========================== decode stop1 ");
            codec.stop();
            codec.release();
//            mSv.post(new Runnable() {
//                @Override
//                public void run() {
//                    Toast.makeText(mSv.getContext(), "play end!", Toast.LENGTH_SHORT).show();
//                    mSv.setVisibility(View.INVISIBLE);
//                }
//            });
            mDecodeThread = null;
            Log.i(TAG, " ============================= decode stop2");
        }


        private ByteBuffer getBuffer(MediaCodec codec, int inputBufferId) {
            ByteBuffer inputBuffer;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                inputBuffer = codec.getInputBuffer(inputBufferId);
            } else {
                inputBuffer = codec.getInputBuffers()[inputBufferId];
            }
            return inputBuffer;
        }

    }

    ArrayBlockingQueue<byte[]> frames = new ArrayBlockingQueue(10);
    static byte[] frameFlag = {0, 0, 0, 1};

    static class MyReadThread extends Thread {
        InputStream mIns = null;
        ArrayBlockingQueue<byte[]> mInnerFrames;
        ArrayBlockingQueue<Object> mInnerFramesObj;
        MyReadThread(InputStream ins,ArrayBlockingQueue<byte[]> que) {
            mIns = ins;
            mInnerFrames = que;
        }

        MyReadThread(InputStream ins,ArrayBlockingQueue<Object> que,String tag) {
            mIns = ins;
            mInnerFramesObj = que;
        }

        @Override
        public void run() {
            try {
                byte[] headFlag = new byte[4];
                mIns.read(headFlag);
                int checkFlagIndex = 0;
                boolean findFlag = false;
//                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024*1024);

                int readSize = 0;

                long lastS = 0;
                int fps = 0;

                byte[] readed = new byte[1024 * 4096];
                boolean isFirstFrame = true;

                int readFrameSize = 0;
                Object lock= new Object();
                while (true) {
                    long last = System.currentTimeMillis();
                    readSize = mIns.read(readed);
                    long readCost = System.currentTimeMillis() - last;
                    if(readSize == -1){
                        mIns.close();
                        mIns = new FileInputStream(new File(H264_FILE_PATH));
                        mIns.read(headFlag);
                        isFirstFrame = true;
                        Log.i(TAG," run =================== END FILE, CONTINUE ");
                        continue;
                    }

                    long beginScan = System.currentTimeMillis();
                    int index = 0;
                    int dataBeginIndex = 0;
                    while (index < readSize) {
                        byte data = readed[index];
                        index ++;

                        if( data == frameFlag[checkFlagIndex] ){
                            checkFlagIndex ++;
                            if(checkFlagIndex == 4){
                                findFlag = true;
                            }
                        }else if(checkFlagIndex != 0){
                            checkFlagIndex = 0;
                        }

                        if (findFlag) {
                            byteBuffer.put(readed,dataBeginIndex,(index - dataBeginIndex));
                            byte[] rs = new byte[byteBuffer.position()];
                            byteBuffer.flip();
                            System.arraycopy(frameFlag, 0, rs, 0, frameFlag.length);
                            byteBuffer.get(rs,4,rs.length - frameFlag.length);
                            if(mInnerFrames != null){
                                mInnerFrames.put(rs);
                            }
                            if(mInnerFramesObj != null){
                                Beans.VideoData tmpDataObj = new Beans.VideoData(isFirstFrame,false,1024,768, rs);
                                mInnerFramesObj.put(tmpDataObj);
//                                Log.i(TAG, "run: read file frame ---- size: "+ readFrameSize++);
                                isFirstFrame = false;
                                synchronized (lock){
                                    lock.wait(30);
                                }

                            }

                            byteBuffer.clear();
                            dataBeginIndex = index;

                            checkFlagIndex = 0;
                            findFlag = false;

                            long tmpS = System.currentTimeMillis() / 1000;
                            if (tmpS == lastS) {
                                fps++;
                            } else {
                                if(mInnerFramesObj != null){
                                    Log.i(TAG, "===== read File ================ FPS:" + fps + ", queSize:"+mInnerFramesObj.size());
                                }
                                if(mInnerFrames != null) {
                                    Log.i(TAG, "===== read File ================ FPS:" + fps + ", queSize:" + mInnerFrames.size());
                                }
                                fps = 0;
                                lastS = tmpS;
                            }

                        }else if( index == readSize ){
                            // read end ,cache the data
                            byteBuffer.put(readed,dataBeginIndex,(index-dataBeginIndex));
                        }

                    }
                    long scanCost = System.currentTimeMillis() - beginScan;
                    Log.i(TAG," run, readDataCost-ms:"+ readCost +",-- scanCost:"+scanCost);

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    @Deprecated
    private byte[] getFrameData(InputStream ins) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] rs = {};
        try {
            int d = ins.read();
            int checkFlagIndex = 0;
            boolean findFlag = false;
            while (d >= 0) {
                byte data = (byte) d;
//                Log.i(TAG," getFrameData, data:"+data+",checkIndex:"+checkFlagIndex+",flagData:"+frameFlag[checkFlagIndex]);
                if (checkFlagIndex == 0 && data == frameFlag[0]) {
                    checkFlagIndex = 1;
                } else if (checkFlagIndex == 1 && data == frameFlag[1]) {
                    checkFlagIndex = 2;
                } else if (checkFlagIndex == 2 && data == frameFlag[2]) {
                    checkFlagIndex = 3;
                } else if (checkFlagIndex == 3 && data == frameFlag[3]) {
                    findFlag = true;
                } else {
                    checkFlagIndex = 0;
                }
                outputStream.write(data);
                if (findFlag) {
                    break;
                }
                d = ins.read();
            }
            byte[] tmp = outputStream.toByteArray();
            if (findFlag) {
                rs = new byte[tmp.length];
                System.arraycopy(frameFlag, 0, rs, 0, frameFlag.length);
                System.arraycopy(tmp, 0, rs, frameFlag.length, tmp.length - frameFlag.length);
            } else {
                rs = tmp;
            }


        } catch (IOException e) {
            e.printStackTrace();
        }

        return rs;
    }


}
