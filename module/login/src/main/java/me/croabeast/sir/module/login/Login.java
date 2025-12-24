package me.croabeast.sir.module.login;

import lombok.Getter;
import me.croabeast.common.CollectionBuilder;
import me.croabeast.common.util.Exceptions;
import me.croabeast.sir.PluginDependant;
import me.croabeast.sir.module.SIRModule;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;

public final class Login extends SIRModule implements PluginDependant {

    Listeners listeners;

    @Getter
    private final String[] dependencies = {"AuthMe", "UserLogin", "nLogin", "OpeNLogin", "NexAuth"};
    private final Set<Plugin> loadedPlugins = new HashSet<>();

    @Override
    public Plugin getPlugin() {
        return loadedPlugins.size() != 1 ? null : loadedPlugins.iterator().next();
    }

    @Override
    public boolean register() {
        loadedPlugins.addAll(CollectionBuilder.of(dependencies)
                .filter(Exceptions::isPluginEnabled)
                .map(Bukkit.getPluginManager()::getPlugin).toSet());
        (listeners = new Listeners(this)).register();
        return true;
    }

    @Override
    public boolean unregister() {
        listeners.unregister();
        return true;
    }
}
