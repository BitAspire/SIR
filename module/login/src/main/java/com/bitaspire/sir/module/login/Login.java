package com.bitaspire.sir.module.login;

import lombok.Getter;
import com.bitaspire.sir.PluginDependant;
import com.bitaspire.sir.module.SIRModule;

public final class Login extends SIRModule implements PluginDependant {

    Listeners listeners;

    @Getter
    private final String[] dependencies = {"AuthMe", "UserLogin", "nLogin", "OpeNLogin", "NexAuth"};

    @Override
    public boolean register() {
        (listeners = new Listeners(this)).register();
        return true;
    }

    @Override
    public boolean unregister() {
        listeners.unregister();
        return true;
    }
}
