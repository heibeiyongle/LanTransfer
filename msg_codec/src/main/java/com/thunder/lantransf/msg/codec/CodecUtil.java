package com.thunder.lantransf.msg.codec;

import android.util.Log;

import com.thunder.common.lib.dto.Beans;
import com.thunder.common.lib.util.GsonUtils;
import com.thunder.lantransf.msg.TransfMsgWrapper;

/**
 * Created by zhe on 2022/6/12 22:15
 *
 * @desc:
 */
public class CodecUtil  {


    private static final String TAG = "CodecUtil";
    public static String encodeMsg(Object msg) {
        TransfMsgWrapper transfMsgWrapper = new TransfMsgWrapper(msg.getClass().getSimpleName(),msg);
        String tmp = GsonUtils.toJson(transfMsgWrapper);
//        Log.d(TAG, "encodeMsg() called with: msg = [" + msg + "]");
//        Log.d(TAG, "encodeMsg() res = [" + tmp + "]");
        return tmp;
    }

    public static TransfMsgWrapper decodeMsg(String msg) {
        TransfMsgWrapper transfPkgMsg = GsonUtils.parse(msg, TransfMsgWrapper.class);
        return transfPkgMsg;
    }



}
