/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thunder.lantransfserver.video;

import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;

/**
 * mock
 *
 */
public class GenVideoAndPlay {
    private static final String TAG = "DecodeEditEncode";
    private static final boolean VERBOSE = true;           // lots of logging

    private String dirName = "/sdcard/debug-video/";

    // parameters for the encoder
    private static final int FRAME_RATE = 15;               // 15fps

    private static final int TEST_R0 = 0;                   // dull green background
    private static final int TEST_G0 = 136;
    private static final int TEST_B0 = 0;
    private static final int TEST_R1 = 0;                 // pink; BT.601 YUV {120,160,200}
    private static final int TEST_G1 = 0;
    private static final int TEST_B1 = 236;

    // size of a frame, in pixels
    private int mWidth = -1;
    private int mHeight = -1;

    public void startVideoQCIF(Surface outSurface) throws Throwable {
        setParameters(176, 144, 1000000);
        runTest(outSurface);
    }
    public void startVideoQVGA(Surface outSurface) throws Throwable {
        setParameters(320, 240, 2000000);
        runTest(outSurface);
    }
    public void startVideo720p(Surface outSurface) throws Throwable {
        setParameters(1280, 720, 6000000);
        runTest(outSurface);
    }

    public void runTest(Surface sur){
        Log.i(TAG, "runTest: surFace: "+sur);
        GenVideo genVideoRunnable = new GenVideo(sur);
        Thread th = new Thread(genVideoRunnable, "codec test");
        th.start();
    }

    /**
     * Sets the desired frame size and bit rate.
     */
    private void setParameters(int width, int height, int bitRate) {
        if ((width % 16) != 0 || (height % 16) != 0) {
            Log.w(TAG, "WARNING: width or height not multiple of 16");
        }
        mWidth = width;
        mHeight = height;
    }


    class GenVideo implements Runnable{

        private Surface outSurface = null;
        GenVideo(Surface encoderSurface){
            outSurface = encoderSurface;
        }

        @Override
        public void run() {
            genVideo(outSurface);
        }

        int generateIndex = 0;
        private void genVideo(Surface outSurface){
            Log.i(TAG, "genVideo before sleep-1");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.i(TAG, "genVideo before sleep-2");
            InputSurface inputSurface = new InputSurface(outSurface);
            inputSurface.makeCurrent();
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (true) {
                generateSurfaceFrame(generateIndex);
                inputSurface.setPresentationTime(computePresentationTime(generateIndex) * 1000);
                if (VERBOSE) Log.i(TAG, "inputSurface swapBuffers");
                inputSurface.swapBuffers();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                generateIndex ++;
            }

        }
    }




    /**
     * Generates a frame of data using GL commands.
     * <p>
     * We have an 8-frame animation sequence that wraps around.  It looks like this:
     * <pre>
     *   0 1 2 3
     *   7 6 5 4
     * </pre>
     * We draw one of the eight rectangles and leave the rest set to the zero-fill color.     */
    private void generateSurfaceFrame(int frameIndex) {
        Log.i(TAG, "generateSurfaceFrame: frameIndex: "+frameIndex);
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


    /**
     * Generates the presentation time for frame N, in microseconds.
     */
    private static long computePresentationTime(int frameIndex) {
        return 123 + frameIndex * 1000000 / FRAME_RATE;
    }


}
