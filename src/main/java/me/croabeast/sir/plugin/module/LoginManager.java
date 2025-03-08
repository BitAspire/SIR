package me.croabeast.sir.plugin.module;

import com.elchologamer.userlogin.api.event.AuthenticationEvent;
import com.nickuc.login.api.event.bukkit.auth.AuthenticateEvent;
import com.nickuc.openlogin.bukkit.api.events.AsyncLoginEvent;
import com.nickuc.openlogin.bukkit.api.events.AsyncRegisterEvent;
import fr.xephi.authme.events.LoginEvent;
import lombok.Getter;
import lombok.Setter;
import me.croabeast.lib.CollectionBuilder;
import me.croabeast.lib.util.Exceptions;
import me.croabeast.sir.api.CustomListener;
import me.croabeast.sir.plugin.misc.SIRUser;
import me.croabeast.takion.logger.TakionLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.Plugin;
import su.nexmedia.auth.api.event.AuthPlayerLoginEvent;
import su.nexmedia.auth.api.event.AuthPlayerRegisterEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
final class LoginManager extends SIRModule implements HookLoadable {

    private final String[] supportedPlugins =
            {"AuthMe", "UserLogin", "nLogin", "OpeNLogin", "NexAuth"};
    private final List<Plugin> loadedHooks;

    private final Set<LoadedListener> listeners = new HashSet<>();
    private boolean loaded = false;

    LoginManager() {
        super(Key.LOGIN);
        loadedHooks = CollectionBuilder.of(supportedPlugins)
                .filter(Exceptions::isPluginEnabled)
                .map(Bukkit.getPluginManager()::getPlugin).toList();
    }

    void logInUserFromEvent(Player player) {
        SIRUser user = plugin.getUserManager().getUser(player);
        user.setLogged(true);

        JoinQuitHandler handler =
                plugin.getModuleManager().getModule(Key.JOIN_QUIT);
        if (handler == null) return;

        JoinQuitHandler.Type type = player.hasPlayedBefore() ?
                JoinQuitHandler.Type.JOIN : JoinQuitHandler.Type.FIRST;

        JoinQuitHandler.ConnectionUnit unit = handler.get(type, user);
        if (unit != null)
            handler.performConnectionActions(unit, user);
    }

    @Override
    public void load() {
        if (loaded) return;

        if (Exceptions.isPluginEnabled("UserLogin"))
            new LoadedListener() {
                @EventHandler
                private void onLogin(AuthenticationEvent event) {
                    logInUserFromEvent(event.getPlayer());
                }
            }.register();

        if (Exceptions.isPluginEnabled("AuthMe"))
            new LoadedListener() {
                @EventHandler
                private void onLogin(LoginEvent event) {
                    logInUserFromEvent(event.getPlayer());
                }
            }.register();

        if (Exceptions.isPluginEnabled("nLogin"))
            try {
                Class.forName("com.nickuc.login.api.nLoginAPIHolder");
                new LoadedListener() {
                    @EventHandler
                    private void onLogin(AuthenticateEvent event) {
                        logInUserFromEvent(event.getPlayer());
                    }
                }.register();
            }
            catch (Exception e) {
                TakionLogger.getLogger().log("&cUpdate nLogin to v10.");
            }

        if (Exceptions.isPluginEnabled("OpeNLogin"))
            new LoadedListener() {
                @EventHandler
                private void onLogin(AsyncLoginEvent event) {
                    logInUserFromEvent(event.getPlayer());
                }
                @EventHandler
                private void onRegister(AsyncRegisterEvent event) {
                    logInUserFromEvent(event.getPlayer());
                }
            }.register();

        if (Exceptions.isPluginEnabled("NexAuth"))
            new LoadedListener() {
                @EventHandler
                private void onLogin(AuthPlayerLoginEvent event) {
                    logInUserFromEvent(event.getPlayer());
                }
                @EventHandler
                private void onRegister(AuthPlayerRegisterEvent event) {
                    logInUserFromEvent(event.getPlayer());
                }
            }.register();

        loaded = true;
    }

    @Override
    public void unload() {
        if (!loaded) return;

        listeners.forEach(LoadedListener::unregister);
        loaded = false;
    }

    @Override
    public boolean register() {
        return false;
    }

    @Override
    public boolean unregister() {
        return false;
    }

    @Setter @Getter
    class LoadedListener implements CustomListener {

        private boolean registered = false;

        public boolean register() {
            listeners.add(this);
            return register(plugin);
        }

        public boolean unregister() {
            listeners.remove(this);
            return CustomListener.super.unregister();
        }
    }
}
