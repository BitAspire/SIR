package me.croabeast.sir.plugin.manager;

import me.croabeast.lib.CollectionBuilder;
import me.croabeast.lib.Loadable;
import me.croabeast.lib.Registrable;
import me.croabeast.sir.plugin.gui.MenuCreator;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface BaseManager<T> extends Loadable, Registrable {

    default boolean isRegistered() {
        return isLoaded();
    }

    @NotNull
    Set<T> getValues();

    @NotNull
    default CollectionBuilder<T> asBuilder() {
        return CollectionBuilder.of(getValues());
    }

    T fromName(String name);

    @NotNull
    MenuCreator getMenu();
}
