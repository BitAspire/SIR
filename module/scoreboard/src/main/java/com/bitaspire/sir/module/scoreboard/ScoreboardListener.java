package com.bitaspire.sir.module.scoreboard;

import com.bitaspire.sir.Listener;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
final class ScoreboardListener extends Listener {

    private final ScoreboardModule module;

    @EventHandler
    void onJoin(PlayerJoinEvent event) {
        module.refreshAll();
        module.refreshAllLater(20L);
    }

    @EventHandler
    void onQuit(PlayerQuitEvent event) {
        module.remove(event.getPlayer());
        module.refreshAllLater(1L);
    }

    @EventHandler
    void onWorld(PlayerChangedWorldEvent event) {
        module.refresh(event.getPlayer());
        module.refreshLater(event.getPlayer(), 2L);
    }
}
