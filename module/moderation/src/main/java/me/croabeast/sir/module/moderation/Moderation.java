package me.croabeast.sir.module.moderation;

import me.croabeast.sir.module.SIRModule;

public final class Moderation extends SIRModule {

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
}
