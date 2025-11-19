package me.croabeast.sir.module.advancement;

import me.croabeast.sir.module.SIRModule;

public final class Advancements extends SIRModule {

    Listener listener;
    Config config;
    DataHandler data;
    Messages messages;

    @Override
    public void load() {
        config = new Config(this);

        (data = new DataHandler(this)).load();
        messages = new Messages(this);

        (listener = new Listener(this)).register();
    }

    @Override
    public void unload() {
        listener.unregister();
        data.unload();
    }
}
