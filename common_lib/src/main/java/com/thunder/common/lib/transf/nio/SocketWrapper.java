package com.thunder.common.lib.transf.nio;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class SocketWrapper{
    public SocketWrapper(SocketChannel sc){
        this.sc = sc;
        writeBuf.position(writeBuf.capacity());
    }

    private int msgThrowCnt = 0;
    private String clientName;
    private SocketChannel sc;
    private ByteBuffer readBuf = ByteBuffer.allocate(1024*1024);
    private ByteBuffer writeBuf = ByteBuffer.allocate(1024*1024);
    private BlockingQueue<Object> toSendMsgQue = new ArrayBlockingQueue<>(100);

    public int getMsgThrowCnt() {
        return msgThrowCnt;
    }
    public void increaseThrowCnt(){
        msgThrowCnt ++;
    }
    public void resetThrowCnt(){
        msgThrowCnt = 0;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public SocketChannel getSc() {
        return sc;
    }

    public void setSc(SocketChannel sc) {
        this.sc = sc;
    }

    public ByteBuffer getReadBuf() {
        return readBuf;
    }

    public void setReadBuf(ByteBuffer readBuf) {
        this.readBuf = readBuf;
    }

    public ByteBuffer getWriteBuf() {
        return writeBuf;
    }

    public void setWriteBuf(ByteBuffer writeBuf) {
        this.writeBuf = writeBuf;
    }

    public BlockingQueue<Object> getToSendMsgQue() {
        return toSendMsgQue;
    }

    public void setToSendMsgQue(BlockingQueue<Object> toSendMsgQue) {
        this.toSendMsgQue = toSendMsgQue;
    }
}