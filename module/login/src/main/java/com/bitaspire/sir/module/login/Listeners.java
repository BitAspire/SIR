package com.bitaspire.sir.module.login;

import com.nickuc.login.api.event.bukkit.auth.AuthenticateEvent;
import com.nickuc.openlogin.bukkit.api.events.AsyncLoginEvent;
import com.nickuc.openlogin.bukkit.api.events.AsyncRegisterEvent;
import fr.xephi.authme.events.LoginEvent;
import com.bitaspire.sir.module.JoinQuitService;

import lombok.RequiredArgsConstructor;
import me.croabeast.common.Registrable;
import com.bitaspire.sir.Listener;
import com.bitaspire.sir.user.SIRUser;
import me.croabeast.takion.logger.LogLevel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.PluginManager;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
final class Listeners implements Registrable {

    private final Login main;
    private final List<Listener> listeners = new ArrayList<>();

    void logUser(Player player) {
        SIRUser user = main.getApi().getUserManager().getUser(player);
        user.setLogged(true);

        JoinQuitService joinQuit = main.getApi().getModuleManager().getJoinQuitService();
        if (joinQuit != null) joinQuit.display(user, true);
    }

    @Override
    public boolean isRegistered() {
        return !listeners.isEmpty() && listeners.stream().allMatch(Listener::isRegistered);
    }

    @Override
    public boolean register() {
        if (isRegistered()) return true;

        PluginManager manager = Bukkit.getPluginManager();
        listeners.clear();

        if (manager.isPluginEnabled("AuthMe"))
            listeners.add(new Listener() {
                @EventHandler
                private void onLogin(LoginEvent event) {
                    logUser(event.getPlayer());
                }
            });

        if (manager.isPluginEnabled("nLogin"))
            try {
                Class.forName("com.nickuc.login.api.nLoginAPIHolder");
                listeners.add(new Listener() {
                    @EventHandler
                    private void onLogin(AuthenticateEvent event) {
                        logUser(event.getPlayer());
                    }
                });
            } catch (Exception e) {
                main.getApi().getLibrary().getLogger().log(LogLevel.WARN, "Update nLogin to version 10.0+ to use the login feature.");
            }

        if (manager.isPluginEnabled("OpeNLogin"))
            listeners.add(new Listener() {
                @EventHandler
                private void onLogin(AsyncLoginEvent event) {
                    logUser(event.getPlayer());
                }
                @EventHandler
                private void onRegister(AsyncRegisterEvent event) {
                    logUser(event.getPlayer());
                }
            });

        boolean registered = true;
        for (Listener listener : listeners)
            registered &= listener.register();

        if (!registered) unregister();
        return registered;
    }

    @Override
    public boolean unregister() {
        boolean unregistered = true;
        for (Listener listener : listeners)
            unregistered &= listener.unregister();

        listeners.clear();
        return unregistered;
    }
}
