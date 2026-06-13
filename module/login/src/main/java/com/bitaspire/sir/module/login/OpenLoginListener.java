package com.bitaspire.sir.module.login;

import com.bitaspire.sir.Listener;
import com.nickuc.openlogin.bukkit.api.events.AsyncLoginEvent;
import com.nickuc.openlogin.bukkit.api.events.AsyncRegisterEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;

@RequiredArgsConstructor
final class OpenLoginListener extends Listener {

    private final Listeners parent;

    @EventHandler
    private void onLogin(AsyncLoginEvent event) {
        parent.logUser(event.getPlayer());
    }

    @EventHandler
    private void onRegister(AsyncRegisterEvent event) {
        parent.logUser(event.getPlayer());
    }
}
