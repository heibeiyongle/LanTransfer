package com.thunder.lantransf.msg.codec;

import com.thunder.lantransf.msg.TransfMsgWrapper;

/**
 * Created by zhe on 2022/6/12 16:36
 *
 * @desc:
 */
public interface ICodec {

    String encodeMsg(Object msg);

    TransfMsgWrapper decodeMsg(String msg);

}
