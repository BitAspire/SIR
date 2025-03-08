package me.croabeast.sir.plugin.hook;

import lombok.experimental.UtilityClass;
import me.croabeast.lib.util.Exceptions;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

@UtilityClass
public class HookChecker {

    public final boolean VAULT_ENABLED;
    public final boolean PAPI_ENABLED;
    public final boolean DISCORD_ENABLED;

    static {
        VAULT_ENABLED = Exceptions.isPluginEnabled("Vault");
        PAPI_ENABLED = Exceptions.isPluginEnabled("PlaceholderAPI");
        DISCORD_ENABLED = Exceptions.isPluginEnabled("DiscordSRV");
    }

    public String getVersion(String pluginName) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        return plugin == null ? "" : plugin.getDescription().getVersion();
    }
}
