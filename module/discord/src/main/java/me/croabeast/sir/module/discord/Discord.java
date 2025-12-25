package me.croabeast.sir.module.discord;

import lombok.Getter;
import me.croabeast.common.CollectionBuilder;
import me.croabeast.common.util.Exceptions;
import me.croabeast.sir.PluginDependant;
import me.croabeast.sir.module.DiscordService;
import me.croabeast.sir.module.SIRModule;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;
import java.util.function.UnaryOperator;

public class Discord extends SIRModule implements PluginDependant, DiscordService {

    @Getter
    private final String[] dependencies = {"DiscordSRV", "EssentialsDiscord"};
    private final Set<Plugin> loadedPlugins = new HashSet<>();

    private Config config;

    @Override
    public Plugin getPlugin() {
        return loadedPlugins.size() != 1 ? null : loadedPlugins.iterator().next();
    }

    @Override
    public boolean register() {
        loadedPlugins.addAll(CollectionBuilder.of(dependencies)
                .filter(Exceptions::isPluginEnabled)
                .map(Bukkit.getPluginManager()::getPlugin).toSet());

        final Plugin plugin = getPlugin();
        config = new Config(this, plugin != null && plugin.getName().equals("EssentialsDiscord"));
        return true;
    }

    @Override
    public boolean unregister() {
        return true;
    }

    public void sendMessage(String channel, Player player, UnaryOperator<String> operator) {
        config.send(channel, player, operator);
    }

    public boolean isRestricted() {
        return config.restricted;
    }
}
