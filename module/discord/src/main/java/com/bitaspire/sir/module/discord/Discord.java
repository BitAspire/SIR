package com.bitaspire.sir.module.discord;

import lombok.Getter;
import me.croabeast.common.CollectionBuilder;
import me.croabeast.common.util.Exceptions;
import com.bitaspire.sir.PluginDependant;
import com.bitaspire.sir.module.DiscordService;
import com.bitaspire.sir.module.SIRModule;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.function.UnaryOperator;

public class Discord extends SIRModule implements PluginDependant, DiscordService {

    @Getter
    private final String[] dependencies = {"DiscordSRV", "EssentialsDiscord"};
    private Config config;

    @Override
    public boolean register() {
        Set<Plugin> loadedPlugins = CollectionBuilder.of(dependencies)
                .filter(Exceptions::isPluginEnabled)
                .map(Bukkit.getPluginManager()::getPlugin).toSet();

        Plugin plugin = loadedPlugins.size() != 1 ? null : loadedPlugins.iterator().next();
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
