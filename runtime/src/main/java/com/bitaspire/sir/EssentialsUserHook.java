package com.bitaspire.sir;

import com.earth2me.essentials.Essentials;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@UtilityClass
class EssentialsUserHook {

    boolean isMuted(Player player) {
        return JavaPlugin.getPlugin(Essentials.class).getUser(player).isMuted();
    }

    boolean isVanished(Player player) {
        return JavaPlugin.getPlugin(Essentials.class).getUser(player).isVanished();
    }
}
