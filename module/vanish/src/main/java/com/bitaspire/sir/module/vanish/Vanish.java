package com.bitaspire.sir.module.vanish;

import com.bitaspire.sir.chat.ChatProcessor;
import lombok.Getter;
import com.bitaspire.sir.PluginDependant;
import com.bitaspire.sir.module.SIRModule;
import org.jetbrains.annotations.NotNull;

public class Vanish extends SIRModule implements PluginDependant, ChatProcessor {

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

    @Override
    public int getPriority() {
        return -200;
    }

    @Override
    public void process(@NotNull ChatProcessor.Context context) {
        if (listeners != null) listeners.processChat(context);
    }
}
