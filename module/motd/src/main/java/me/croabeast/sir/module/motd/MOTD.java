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
        return (listener = new Listener(this)).register();
    }

    @Override
    public boolean unregister() {
        loader.unload();
        return listener.unregister();
    }
}
