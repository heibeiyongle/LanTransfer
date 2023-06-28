package com.thunder.lantransf.client.state;

import com.thunder.common.lib.bean.NetTimeInfo;

public interface IStateOpt {
    void updateNetInfo(NetTimeInfo netTimeInfo);
    NetTimeInfo getNetInfo();
}
