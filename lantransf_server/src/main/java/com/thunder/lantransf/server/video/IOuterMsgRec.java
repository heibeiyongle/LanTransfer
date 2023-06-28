package com.thunder.lantransf.server.video;


import com.thunder.common.lib.dto.Beans;

interface IOuterMsgRec {
    void setOutMsgHandler(IOutMsgHandler handler);

    interface IOutMsgHandler{
        void onGotMsg(String msg, String from);
    }

}
