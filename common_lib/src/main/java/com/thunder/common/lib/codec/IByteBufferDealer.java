package com.thunder.common.lib.codec;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

/**
 * Created by zhe on 2022/6/19 22:53
 *
 * @desc:
 */
public interface IByteBufferDealer {

    // common
    Object decodeReadBuf(ByteBuffer readBuf);


    ByteBuffer encodeWriteBuf(BlockingQueue<Object> msgs, ByteBuffer writeBuf);


}
