package com.bitaspire.sir.module.moderation;

import com.bitaspire.sir.chat.ChatProcessor;
import com.bitaspire.sir.module.SIRModule;
import org.jetbrains.annotations.NotNull;

public final class Moderation extends SIRModule implements ChatProcessor {

    Config config;
    Swearing swearing;
    Caps caps;
    Format format;
    Links links;

    @Override
    public boolean register() {
        config = new Config(this);
        (swearing = new Swearing(this)).register();
        (caps = new Caps(this)).register();
        (format = new Format(this)).register();
        (links = new Links(this)).register();
        return true;
    }

    @Override
    public boolean unregister() {
        swearing.unregister();
        caps.unregister();
        format.unregister();
        links.unregister();
        return true;
    }

    @Override
    public int getPriority() {
        return -500;
    }

    @Override
    public void process(@NotNull ChatProcessor.Context context) {
        if (!isEnabled()) return;

        swearing.process(context);
        if (context.isCancelled()) return;

        caps.process(context);
        if (context.isCancelled()) return;

        format.process(context);
        if (context.isCancelled()) return;

        links.process(context);
    }
}
