package com.bitaspire.sir;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

@UtilityClass
class ExternalUserHooks {

    boolean isEssentialsMuted(Player player) {
        try {
            return isAvailable("Essentials", "com.earth2me.essentials.Essentials")
                    && EssentialsUserHook.isMuted(player);
        } catch (Throwable ignored) {
            return false;
        }
    }

    boolean isEssentialsVanished(Player player) {
        try {
            return isAvailable("Essentials", "com.earth2me.essentials.Essentials")
                    && EssentialsUserHook.isVanished(player);
        } catch (Throwable ignored) {
            return false;
        }
    }

    boolean isAdvancedBanMuted(String name) {
        try {
            return isAvailable(
                    "AdvancedBan",
                    "me.leoko.advancedban.manager.UUIDManager",
                    "me.leoko.advancedban.manager.PunishmentManager"
            ) && AdvancedBanUserHook.isMuted(name);
        } catch (Throwable ignored) {
            return false;
        }
    }

    boolean isCmiMuted(Player player) {
        try {
            return isAvailable("CMI", "com.Zrips.CMI.Containers.CMIUser")
                    && CmiUserHook.isMuted(player);
        } catch (Throwable ignored) {
            return false;
        }
    }

    boolean isCmiVanished(Player player) {
        try {
            return isAvailable("CMI", "com.Zrips.CMI.Containers.CMIUser")
                    && CmiUserHook.isVanished(player);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean isAvailable(String pluginName, String... classNames) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin == null || !plugin.isEnabled()) return false;

        ClassLoader loader = plugin.getClass().getClassLoader();
        for (String className : classNames)
            try {
                Class.forName(className, false, loader);
            } catch (Throwable ignored) {
                return false;
            }

        return true;
    }
}
