package com.thunder.lantransf.server.video;


import com.thunder.common.lib.dto.Beans;

interface IMsgSender {
    void setPublisher(ITransfServer publisher);
    void sendCmd(Beans.CommandMsg msg);
}
