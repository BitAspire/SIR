package com.bitaspire.sir.module.advancement;

import com.bitaspire.sir.ChatToggleable;
import com.bitaspire.sir.module.SIRModule;
import me.croabeast.vnc.VNC;

public final class Advancements extends SIRModule implements ChatToggleable {

    Listener listener;
    Config config;
    DataHandler data;
    Messages messages;

    @Override
    public boolean register() {
        if (VNC.isBefore("1.12"))
            return false;

        config = new Config(this);

        (data = new DataHandler(this)).load();
        messages = new Messages(this);

        (listener = new Listener(this)).register();
        return true;
    }

    @Override
    public boolean unregister() {
        if (listener != null) {
            listener.unregister();
            listener = null;
        }

        if (data != null) {
            data.unload();
            data = null;
        }

        config = null;
        messages = null;
        return true;
    }
}
