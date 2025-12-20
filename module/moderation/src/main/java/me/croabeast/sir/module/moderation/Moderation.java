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
        try {
            (swearing = new Swearing(this)).register();
        } catch (Exception ignored) {}
        try {
            (caps = new Caps(this)).register();
        } catch (Exception ignored) {}
        try {
            (format = new Format(this)).register();
        } catch (Exception ignored) {}
        try {
            (links = new Links(this)).register();
        } catch (Exception ignored) {}
        return true;
    }

    @Override
    public boolean unregister() {
        if (swearing != null) swearing.unregister();
        if (caps != null) caps.unregister();
        if (format != null) format.unregister();
        if (links != null) links.unregister();
        return true;
    }
}
