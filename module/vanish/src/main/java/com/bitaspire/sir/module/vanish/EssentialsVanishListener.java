package com.bitaspire.sir.module.vanish;

import com.bitaspire.sir.Listener;
import lombok.RequiredArgsConstructor;
import net.ess3.api.IUser;
import net.ess3.api.events.VanishStatusChangeEvent;
import org.bukkit.event.EventHandler;

@RequiredArgsConstructor
final class EssentialsVanishListener extends Listener {

    private final Vanish main;

    @EventHandler
    private void onVanish(VanishStatusChangeEvent event) {
        IUser user = event.getAffected();
        new VanishEvent(main.getApi().getUserManager().getUser(user.getBase()), user.isVanished()).call();
    }
}
