package com.bitaspire.sir.module.cooldown;

import com.bitaspire.sir.module.SIRModule;

public final class Cooldowns extends SIRModule {

    Data data;
    Listener listener;

    @Override
    public boolean register() {
        data = new Data(this);
        return (listener = new Listener(this)).register();
    }

    @Override
    public boolean unregister() {
        return listener.unregister();
    }
}
