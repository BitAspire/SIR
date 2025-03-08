package me.croabeast.sir.plugin.module;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface HookLoadable {

    @NotNull
    String[] getSupportedPlugins();

    @NotNull
    List<Plugin> getLoadedHooks();

    boolean isLoaded();

    void load();

    void unload();

    @Nullable
    default Plugin getHookedPlugin() {
        return getLoadedHooks().size() != 1 ? null : getLoadedHooks().get(0);
    }
}
