package com.bitaspire.sir.module.custom;

import com.bitaspire.sir.Listener;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

@RequiredArgsConstructor
final class Joiner extends Listener {

    private final CustomCommands main;

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        boolean first = !event.getPlayer().hasPlayedBefore();
        for (CustomCommand command : main.loaded.values()) command.join(event.getPlayer(), first);
    }
}
