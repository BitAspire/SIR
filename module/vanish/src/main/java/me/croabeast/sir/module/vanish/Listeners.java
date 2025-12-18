package me.croabeast.sir.module.vanish;

import com.Zrips.CMI.events.CMIPlayerUnVanishEvent;
import com.Zrips.CMI.events.CMIPlayerVanishEvent;
import de.myzelyam.api.vanish.PlayerVanishStateChangeEvent;
import net.ess3.api.IUser;
import net.ess3.api.events.VanishStatusChangeEvent;
import me.croabeast.common.Registrable;
import me.croabeast.sir.Listener;
import me.croabeast.sir.module.join.JoinQuit;
import me.croabeast.sir.user.SIRUser;
import me.croabeast.sir.user.UserManager;
import me.croabeast.takion.message.MessageSender;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.PluginManager;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Listeners implements Registrable {

    private final Vanish main;
    private final Listener listener;

    Listeners(Vanish main) {
        this.main = main;
        listener = new Listener() {
            @EventHandler
            private void onVanish(VanishEvent event) {
                if (!main.isEnabled()) return;

                SIRUser user = event.getUser();
                if (main.getApi().getModuleManager().isEnabled("Login")) user.setLogged(true);
                if (!main.getApi().getModuleManager().isEnabled("JoinQuit")) return;

                boolean vanished = event.isVanished();

                JoinQuit joinQuit = main.getApi().getModuleManager().getModule(JoinQuit.class);
                if (joinQuit == null || joinQuit.isStillOnCooldown(user, vanished)) return;

                if (vanished) {
                    joinQuit.displayJoin(user);
                    return;
                }

                joinQuit.displayQuit(user);
            }

            @EventHandler
            private void onChat(AsyncPlayerChatEvent event) {
                if (event.isCancelled() || !main.isEnabled() || !main.config.isChatEnabled())
                    return;

                String key = main.config.getChatKey();
                if (key == null || key.isEmpty()) return;

                String message = event.getMessage();
                Player player = event.getPlayer();

                List<String> list = main.config.getNotAllowed();

                MessageSender sender = main.getApi().getLibrary().getLoadedSender().setTargets(player);
                if (main.config.isRegex()) {
                    Matcher match = Pattern.compile(key).matcher(message);

                    if (!match.find()) {
                        event.setCancelled(true);
                        sender.send(list);
                        return;
                    }

                    event.setMessage(message.replace(match.group(), ""));
                    return;
                }

                boolean prefix = main.config.isPrefix();
                int kLen = key.length();

                boolean ok = prefix ? message.startsWith(key) : message.endsWith(key);
                if (!ok) {
                    event.setCancelled(true);
                    sender.send(list);
                    return;
                }

                event.setMessage(!prefix ?
                        message.substring(0, message.length() - kLen) :
                        message.substring(kLen));
            }
        };
    }

    private Listener listeners;

    @Override
    public boolean isRegistered() {
        return (listeners != null && listeners.isRegistered()) && listener.isRegistered();
    }

    @Override
    public boolean register() {
        PluginManager manager = Bukkit.getPluginManager();
        UserManager userManager = main.getApi().getUserManager();

        if (manager.isPluginEnabled("Essentials"))
            listeners = new Listener() {
                @EventHandler
                private void onVanish(VanishStatusChangeEvent event) {
                    IUser user = event.getAffected();
                    new VanishEvent(userManager.getUser(user.getBase()), user.isVanished()).call();
                }
            };

        if (manager.isPluginEnabled("CMI"))
            listeners = new Listener() {
                @EventHandler
                private void onVanish(CMIPlayerVanishEvent event) {
                    new VanishEvent(userManager.getUser(event.getPlayer()), false).call();
                }
                @EventHandler
                private void onUnVanish(CMIPlayerUnVanishEvent event) {
                    new VanishEvent(userManager.getUser(event.getPlayer()), true).call();
                }
            };

        if (manager.isPluginEnabled("SuperVanish") || manager.isPluginEnabled("PremiumVanish"))
            listeners = new Listener() {
                @EventHandler
                private void onVanish(PlayerVanishStateChangeEvent event) {
                    Player player = Bukkit.getPlayer(event.getUUID());
                    new VanishEvent(userManager.getUser(player), !event.isVanishing()).call();
                }
            };

        return (listeners == null || listeners.register()) && listener.register();
    }

    @Override
    public boolean unregister() {
        return (listeners == null || listeners.unregister()) && listener.unregister();
    }
}
