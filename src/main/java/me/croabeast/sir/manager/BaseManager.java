package me.croabeast.sir.manager;

import me.croabeast.common.CollectionBuilder;
import me.croabeast.common.Loadable;
import me.croabeast.common.Registrable;
import me.croabeast.common.gui.ChestBuilder;
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
    ChestBuilder getMenu();
}
