package me.croabeast.sir.plugin.module;

import me.croabeast.common.Loadable;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public interface HookLoadable extends Loadable {

    @NotNull
    String[] getSupportedPlugins();

    Plugin getHookedPlugin();

    default boolean isPluginEnabled() {
        return getHookedPlugin() != null && getHookedPlugin().isEnabled();
    }
}
