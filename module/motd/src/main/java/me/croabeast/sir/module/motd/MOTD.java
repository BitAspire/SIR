package me.croabeast.sir.module.motd;

import me.croabeast.sir.module.SIRModule;

public final class MOTD extends SIRModule {

    Config config;
    Data data;
    IconLoader loader;
    Listener listener;

    @Override
    public boolean register() {
        config = new Config(this);
        data = new Data(this);

        (loader = new IconLoader(this)).load();
        (listener = new Listener(this)).register();
        return true;
    }

    @Override
    public boolean unregister() {
        loader.unload();
        listener.unregister();
        return true;
    }
}
