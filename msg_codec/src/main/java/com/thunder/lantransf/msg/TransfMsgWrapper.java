package com.thunder.lantransf.msg;

/**
 * Created by zhe on 2022/6/12 16:41
 *
 * @desc:
 */
public class TransfMsgWrapper {

    private String msgClassName;
    private Object msg;

    public String getMsgClassName() {
        return msgClassName;
    }

    public void setMsgClassName(String msgClassName) {
        this.msgClassName = msgClassName;
    }

    public Object getMsg() {
        return msg;
    }

    public void setMsg(Object msg) {
        this.msg = msg;
    }

    public TransfMsgWrapper(String className, Object msg){
        this.msgClassName = className;
        this.msg = msg;
    }
}
