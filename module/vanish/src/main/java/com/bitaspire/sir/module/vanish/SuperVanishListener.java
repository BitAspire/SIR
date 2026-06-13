package com.bitaspire.sir.module.vanish;

import com.bitaspire.sir.Listener;
import de.myzelyam.api.vanish.PlayerVanishStateChangeEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

@RequiredArgsConstructor
final class SuperVanishListener extends Listener {

    private final Vanish main;

    @EventHandler
    private void onVanish(PlayerVanishStateChangeEvent event) {
        Player player = Bukkit.getPlayer(event.getUUID());
        if (player == null) return;

        new VanishEvent(main.getApi().getUserManager().getUser(player), !event.isVanishing()).call();
    }
}
