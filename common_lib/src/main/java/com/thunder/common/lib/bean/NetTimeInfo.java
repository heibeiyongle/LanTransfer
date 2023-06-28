package com.thunder.common.lib.bean;

import androidx.annotation.Keep;

@Keep
public class NetTimeInfo {

    private long mNetTimeMs;
    private long mLocalTimeMs;
    private long mTransfCostTime;

    private long getNetTimeMs() {
        return mNetTimeMs;
    }

    public void setNetTimeMs(long mNetTimeMs) {
        this.mNetTimeMs = mNetTimeMs;
        this.mLocalTimeMs = System.currentTimeMillis();
    }

    public long getLocalTimeMs() {
        return mLocalTimeMs;
    }

    public long getCurrNetTimeMs() {
        long res = getNetTimeMs() + (System.currentTimeMillis() - getLocalTimeMs());
        return res;
    }

    public long getTransfCostTime() {
        return mTransfCostTime;
    }

    public void setTransfCostTime(long mTransfCostTime) {
        this.mTransfCostTime = mTransfCostTime;
    }

    @Override
    public String toString() {
        return "NetTimeInfo{" +
                "mNetTimeMs=" + mNetTimeMs +
                ", mLocalTimeMs=" + mLocalTimeMs +
                '}';
    }
}
