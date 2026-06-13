package com.bitaspire.sir.module.login;

import com.bitaspire.sir.Listener;
import fr.xephi.authme.events.LoginEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;

@RequiredArgsConstructor
final class AuthMeListener extends Listener {

    private final Listeners parent;

    @EventHandler
    private void onLogin(LoginEvent event) {
        parent.logUser(event.getPlayer());
    }
}
