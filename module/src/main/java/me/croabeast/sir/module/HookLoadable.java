package me.croabeast.sir.module;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public interface HookLoadable {

    @NotNull
    String[] getDependantPlugins();

    Plugin getPlugin();

    default boolean isPluginEnabled() {
        return getPlugin() != null && getPlugin().isEnabled();
    }
}
