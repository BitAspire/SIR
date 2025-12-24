package me.croabeast.sir.module.join;

import me.croabeast.sir.module.SIRModule;
import me.croabeast.sir.user.SIRUser;

public final class JoinQuit extends SIRModule {

    Config config;
    Messages messages;
    Listener listener;

    @Override
    public boolean register() {
        config = new Config(this);
        messages = new Messages(this);
        (listener = new Listener(this)).register();
        return true;
    }

    @Override
    public boolean unregister() {
        listener.unregister();
        return true;
    }

    public boolean isStillOnCooldown(SIRUser user, boolean join) {
        return listener.map.isStillOnCooldown(join ? Listener.Type.JOIN : Listener.Type.QUIT, user.getUuid());
    }

    public void displayJoin(SIRUser user) {
        listener.executeActions(user, true);
    }

    public void displayQuit(SIRUser user) {
        listener.executeActions(user, false);
    }
}
