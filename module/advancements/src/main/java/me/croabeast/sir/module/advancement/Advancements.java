package me.croabeast.sir.module.advancement;

import me.croabeast.common.util.ServerInfoUtils;
import me.croabeast.sir.ChatToggleable;
import me.croabeast.sir.module.SIRModule;

public final class Advancements extends SIRModule implements ChatToggleable {

    Listener listener;
    Config config;
    DataHandler data;
    Messages messages;

    @Override
    public boolean register() {
        if (ServerInfoUtils.SERVER_VERSION < 12)
            return false;

        config = new Config(this);

        (data = new DataHandler(this)).load();
        messages = new Messages(this);

        (listener = new Listener(this)).register();
        return true;
    }

    @Override
    public boolean unregister() {
        listener.unregister();
        data.unload();
        return true;
    }
}
