package com.bitaspire.sir.module.vanish;

import com.Zrips.CMI.events.CMIPlayerUnVanishEvent;
import com.Zrips.CMI.events.CMIPlayerVanishEvent;
import com.bitaspire.sir.Listener;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;

@RequiredArgsConstructor
final class CmiVanishListener extends Listener {

    private final Vanish main;

    @EventHandler
    private void onVanish(CMIPlayerVanishEvent event) {
        new VanishEvent(main.getApi().getUserManager().getUser(event.getPlayer()), false).call();
    }

    @EventHandler
    private void onUnVanish(CMIPlayerUnVanishEvent event) {
        new VanishEvent(main.getApi().getUserManager().getUser(event.getPlayer()), true).call();
    }
}
