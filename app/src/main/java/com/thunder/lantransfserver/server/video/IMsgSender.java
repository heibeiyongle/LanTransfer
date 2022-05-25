package com.thunder.lantransfserver.server.video;

import com.thunder.lantransfserver.server.dto.Beans;

public interface IMsgSender {
    void setPublisher(ITransfServer publisher);
    void sendCmd(Beans.CommandMsg msg);
}
