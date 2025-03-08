package me.croabeast.sir.plugin.module;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import me.croabeast.sir.api.CustomListener;

@Setter @Getter
abstract class ListenerModule extends SIRModule implements CustomListener {

    @Accessors(makeFinal = true)
    private boolean registered = false;

    ListenerModule(Key key) {
        super(key);
    }

    @Override
    public boolean register() {
        return isEnabled() && CustomListener.super.register(plugin);
    }

    @Override
    public boolean unregister() {
        return !isEnabled() && CustomListener.super.unregister();
    }
}
