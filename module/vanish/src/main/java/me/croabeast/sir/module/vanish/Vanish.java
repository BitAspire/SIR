package me.croabeast.sir.module.vanish;

import lombok.Getter;
import me.croabeast.common.CollectionBuilder;
import me.croabeast.common.util.Exceptions;
import me.croabeast.sir.PluginDependant;
import me.croabeast.sir.module.SIRModule;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;

public final class Vanish extends SIRModule implements PluginDependant {

    Config config;
    Listeners listeners;

    @Getter
    private final String[] dependencies = {"Essentials", "CMI", "SuperVanish", "PremiumVanish"};
    private final Set<Plugin> loadedPlugins = new HashSet<>();

    @Override
    public Plugin getPlugin() {
        return loadedPlugins.size() != 1 ? null : loadedPlugins.iterator().next();
    }

    @Override
    public boolean register() {
        config = new Config(this);
        loadedPlugins.addAll(CollectionBuilder.of(dependencies)
                .filter(Exceptions::isPluginEnabled)
                .map(Bukkit.getPluginManager()::getPlugin)
                .toSet());
        (listeners = new Listeners(this)).register();
        return true;
    }

    @Override
    public boolean unregister() {
        listeners.unregister();
        return true;
    }
}
