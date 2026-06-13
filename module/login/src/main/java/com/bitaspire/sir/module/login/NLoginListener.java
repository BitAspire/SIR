package com.bitaspire.sir.module.login;

import com.bitaspire.sir.Listener;
import com.nickuc.login.api.event.bukkit.auth.AuthenticateEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;

@RequiredArgsConstructor
final class NLoginListener extends Listener {

    private final Listeners parent;

    @EventHandler
    private void onLogin(AuthenticateEvent event) {
        parent.logUser(event.getPlayer());
    }
}
