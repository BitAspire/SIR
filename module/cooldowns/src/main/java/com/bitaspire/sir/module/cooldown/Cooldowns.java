package com.bitaspire.sir.module.cooldown;

import com.bitaspire.sir.chat.ChatProcessor;
import com.bitaspire.sir.module.SIRModule;
import org.jetbrains.annotations.NotNull;

public final class Cooldowns extends SIRModule implements ChatProcessor {

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

    @Override
    public int getPriority() {
        return -100;
    }

    @Override
    public void process(@NotNull ChatProcessor.Context context) {
        if (listener != null) listener.process(context);
    }
}
