package com.thunder.lantransf.client.state;

import com.thunder.common.lib.bean.NetTimeInfo;

public class ClientStateManager implements IStateOpt {

    NetTimeInfo mNetInfo = new NetTimeInfo();

    @Override
    public void updateNetInfo(NetTimeInfo netTimeInfo) {
        mNetInfo = netTimeInfo;
    }

    @Override
    public NetTimeInfo getNetInfo() {
        return mNetInfo;
    }
    
    private ClientStateManager() {}
    static class Holder{
        static ClientStateManager instance = new ClientStateManager();
    }
    public static IStateOpt getInstance() {
        return Holder.instance;
    }
    
}
