package me.croabeast.sir.module.login;

import com.elchologamer.userlogin.api.event.AuthenticationEvent;
import com.nickuc.login.api.event.bukkit.auth.AuthenticateEvent;
import com.nickuc.openlogin.bukkit.api.events.AsyncLoginEvent;
import com.nickuc.openlogin.bukkit.api.events.AsyncRegisterEvent;
import fr.xephi.authme.events.LoginEvent;
import su.nexmedia.auth.api.event.AuthPlayerLoginEvent;
import su.nexmedia.auth.api.event.AuthPlayerRegisterEvent;

import lombok.RequiredArgsConstructor;
import me.croabeast.common.Registrable;
import me.croabeast.sir.Listener;
import me.croabeast.sir.module.join.JoinQuit;
import me.croabeast.sir.user.SIRUser;
import me.croabeast.takion.logger.LogLevel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.PluginManager;

@RequiredArgsConstructor
final class Listeners implements Registrable {

    private final Login main;
    private Listener listener;

    void logUser(Player player) {
        SIRUser user = main.getApi().getUserManager().getUser(player);
        user.setLogged(true);

        if (!main.getApi().getModuleManager().isEnabled("JoinQuit"))
            return;

        JoinQuit joinQuit = main.getApi().getModuleManager().getModule(JoinQuit.class);
        if (joinQuit != null && joinQuit.isEnabled()) joinQuit.displayJoin(user);
    }

    @Override
    public boolean isRegistered() {
        return listener != null && listener.isRegistered();
    }

    @Override
    public boolean register() {
        PluginManager manager = Bukkit.getPluginManager();

        if (manager.isPluginEnabled("UserLogin"))
            listener = new Listener() {
                @EventHandler
                private void onLogin(AuthenticationEvent event) {
                    logUser(event.getPlayer());
                }
            };

        if (manager.isPluginEnabled("AuthMe"))
            listener = new Listener() {
                @EventHandler
                private void onLogin(LoginEvent event) {
                    logUser(event.getPlayer());
                }
            };

        if (manager.isPluginEnabled("nLogin"))
            try {
                Class.forName("com.nickuc.login.api.nLoginAPIHolder");
                listener = new Listener() {
                    @EventHandler
                    private void onLogin(AuthenticateEvent event) {
                        logUser(event.getPlayer());
                    }
                };
            } catch (Exception e) {
                main.getApi().getLibrary().getLogger().log(LogLevel.WARN, "Update nLogin to version 10.0+ to use the login feature.");
            }

        if (manager.isPluginEnabled("OpeNLogin"))
            listener = new Listener() {
                @EventHandler
                private void onLogin(AsyncLoginEvent event) {
                    logUser(event.getPlayer());
                }
                @EventHandler
                private void onRegister(AsyncRegisterEvent event) {
                    logUser(event.getPlayer());
                }
            };

        if (manager.isPluginEnabled("NexAuth"))
            listener = new Listener() {
                @EventHandler
                private void onLogin(AuthPlayerLoginEvent event) {
                    logUser(event.getPlayer());
                }
                @EventHandler
                private void onRegister(AuthPlayerRegisterEvent event) {
                    logUser(event.getPlayer());
                }
            };

        return listener == null || listener.register();
    }

    @Override
    public boolean unregister() {
        return listener == null || listener.unregister();
    }
}
