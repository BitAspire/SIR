package com.bitaspire.sir.module.vanish;

import lombok.Getter;
import com.bitaspire.sir.PluginDependant;
import com.bitaspire.sir.module.SIRModule;

public final class Vanish extends SIRModule implements PluginDependant {

    Config config;
    Listeners listeners;

    @Getter
    private final String[] dependencies = {"Essentials", "CMI", "SuperVanish", "PremiumVanish"};

    @Override
    public boolean register() {
        config = new Config(this);
        (listeners = new Listeners(this)).register();
        return true;
    }

    @Override
    public boolean unregister() {
        listeners.unregister();
        return true;
    }
}
