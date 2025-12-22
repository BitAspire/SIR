package me.croabeast.sir;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public interface PluginDependant {

    @NotNull
    String[] getDependencies();

    Plugin getPlugin();

    default boolean isPluginEnabled() {
        return getPlugin() != null && getPlugin().isEnabled();
    }
}
