package com.bitaspire.sir.module.discord;

import lombok.Getter;
import me.croabeast.common.CollectionBuilder;
import me.croabeast.common.util.Exceptions;
import com.bitaspire.sir.PluginDependant;
import com.bitaspire.sir.module.DiscordService;
import com.bitaspire.sir.module.SIRModule;
import me.croabeast.takion.logger.LogLevel;
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
        Backend backend = backend();
        if (backend == Backend.NONE)
            getLogger().log(LogLevel.WARN, "No compatible Discord backend is available.");

        config = new Config(this, backend);
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

    @Override
    public boolean areDependenciesEnabled() {
        return backend() != Backend.NONE;
    }

    Backend backend() {
        Set<Plugin> loadedPlugins = CollectionBuilder.of(dependencies)
                .filter(Exceptions::isPluginEnabled)
                .map(Bukkit.getPluginManager()::getPlugin).toSet();

        for (Plugin plugin : loadedPlugins)
            if (plugin.getName().equals("DiscordSRV") && isDiscordSrvApiAvailable())
                return Backend.DISCORD_SRV;

        for (Plugin plugin : loadedPlugins)
            if (plugin.getName().equals("EssentialsDiscord") && isEssentialsDiscordApiAvailable())
                return Backend.ESSENTIALS;

        return Backend.NONE;
    }

    static boolean isDiscordSrvApiAvailable() {
        return areClassesAvailable("DiscordSRV",
                "github.scarsz.discordsrv.DiscordSRV",
                "github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder",
                "github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel",
                "github.scarsz.discordsrv.util.WebhookUtil"
        );
    }

    static boolean isEssentialsDiscordApiAvailable() {
        return areClassesAvailable("EssentialsDiscord",
                "net.essentialsx.api.v2.ChatType",
                "net.essentialsx.api.v2.events.discord.DiscordChatMessageEvent",
                "net.essentialsx.api.v2.services.discord.DiscordService"
        );
    }

    private static boolean areClassesAvailable(String pluginName, String... names) {
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
            ClassLoader loader = plugin != null ? plugin.getClass().getClassLoader() : Discord.class.getClassLoader();

            for (String name : names)
                Class.forName(name, false, loader);

            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    enum Backend {
        DISCORD_SRV,
        ESSENTIALS,
        NONE
    }
}
