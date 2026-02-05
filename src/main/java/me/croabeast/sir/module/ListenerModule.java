package me.croabeast.sir.module;

import lombok.Getter;
import lombok.experimental.Accessors;
import me.croabeast.common.CustomListener;

@Accessors(makeFinal = true)
@Getter
abstract class ListenerModule extends SIRModule implements CustomListener {

    private final Status status = new Status();

    ListenerModule(Key key) {
        super(key);
    }

    @Override
    public boolean register() {
        return isEnabled() && CustomListener.super.register(plugin);
    }

    @Override
    public boolean unregister() {
        return CustomListener.super.unregister();
    }
}
