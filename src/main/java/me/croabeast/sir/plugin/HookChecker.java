package me.croabeast.sir.plugin;

import lombok.experimental.UtilityClass;
import me.croabeast.common.util.Exceptions;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

@UtilityClass
public class HookChecker {

    public final boolean PAPI_ENABLED;
    public final boolean DISCORD_ENABLED;

    static {
        PAPI_ENABLED = Exceptions.isPluginEnabled("PlaceholderAPI");
        DISCORD_ENABLED = Exceptions.isPluginEnabled("DiscordSRV");
    }

    public String getVersion(String pluginName) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        return plugin == null ? "" : plugin.getDescription().getVersion();
    }
}
