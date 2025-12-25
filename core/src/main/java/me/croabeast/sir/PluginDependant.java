package me.croabeast.sir;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public interface PluginDependant {

    @NotNull
    String[] getDependencies();

    Plugin getPlugin();

    default boolean isPluginEnabled() {
        for (String dependency : getDependencies()) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin(dependency);
            if (plugin != null && plugin.isEnabled()) return true;
        }
        return false;
    }
}
