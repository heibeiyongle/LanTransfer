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

    // parameters for the encoder
    private static final int FRAME_RATE = 15;               // 15fps

    private static final int TEST_R0 = 0;                   // dull green background
    private static final int TEST_G0 = 136;
    private static final int TEST_B0 = 0;
    private static final int TEST_R1 = 0;                 // pink; BT.601 YUV {120,160,200}
    private static final int TEST_G1 = 0;
    private static final int TEST_B1 = 236;

    int generateIndex = 0;
    public void startGenVideo(Surface renderSur, int w, int h){
        Runnable gen = new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "run: surface: " + renderSur);
                InputSurface inputSurface = new InputSurface(renderSur);
                inputSurface.makeCurrent();
                while (true) {
                    generateSurfaceFrame(w,h,generateIndex);
                    inputSurface.setPresentationTime(computePresentationTime(generateIndex) * 1000);
//                    Log.i(TAG, "inputSurface swapBuffers generateIndex: "+ generateIndex);
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
    private void generateSurfaceFrame(int mWidth, int mHeight, int frameIndex) {
        if(frameIndex %20 == 0){
            Log.i(TAG, "generateSurfaceFrame: frameIndex: "+frameIndex);
        }
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
