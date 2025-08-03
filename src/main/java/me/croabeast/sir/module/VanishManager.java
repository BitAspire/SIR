package me.croabeast.sir.module;

import com.Zrips.CMI.events.CMIPlayerUnVanishEvent;
import com.Zrips.CMI.events.CMIPlayerVanishEvent;
import de.myzelyam.api.vanish.PlayerVanishStateChangeEvent;
import lombok.Getter;
import me.croabeast.common.CollectionBuilder;
import me.croabeast.common.util.ArrayUtils;
import me.croabeast.file.Configurable;
import me.croabeast.common.util.Exceptions;
import me.croabeast.common.CustomListener;
import me.croabeast.sir.FileData;
import me.croabeast.sir.user.SIRUser;
import me.croabeast.takion.message.MessageSender;
import net.ess3.api.IUser;
import net.ess3.api.events.VanishStatusChangeEvent;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class VanishManager extends ListenerModule implements HookLoadable {

    @Getter
    final String[] supportedPlugins = {"Essentials", "CMI", "SuperVanish", "PremiumVanish"};
    private final List<Plugin> loadedHooks;

    private final Set<LoadedListener> listeners = new HashSet<>();
    @Getter
    private boolean loaded = false;

    VanishManager() {
        super(Key.VANISH);

        loadedHooks = CollectionBuilder.of(supportedPlugins)
                .filter(Exceptions::isPluginEnabled)
                .map(Bukkit.getPluginManager()::getPlugin).toList();
    }

    @Override
    public void load() {
        if (loaded) return;

        if (Exceptions.isPluginEnabled("Essentials"))
            new LoadedListener() {
                @EventHandler
                private void onVanish(VanishStatusChangeEvent event) {
                    IUser user = event.getAffected();
                    new SIRVanishEvent(plugin.getUserManager().getUser(user.getBase()), user.isVanished()).call();
                }
            }.register();

        if (Exceptions.isPluginEnabled("CMI")) {
            new LoadedListener() {
                @EventHandler
                private void onVanish(CMIPlayerVanishEvent event) {
                    new SIRVanishEvent(plugin.getUserManager().getUser(event.getPlayer()), false).call();
                }
                @EventHandler
                private void onUnVanish(CMIPlayerUnVanishEvent event) {
                    new SIRVanishEvent(plugin.getUserManager().getUser(event.getPlayer()), true).call();
                }
            }.register();
        }

        if (Exceptions.anyPluginEnabled(ArrayUtils.toList("SuperVanish", "PremiumVanish")))
            new LoadedListener() {
                @EventHandler
                private void onVanish(PlayerVanishStateChangeEvent event) {
                    Player player = Bukkit.getPlayer(event.getUUID());
                    new SIRVanishEvent(plugin.getUserManager().getUser(player), !event.isVanishing()).call();
                }
            }.register();

        loaded = true;
    }

    @Override
    public void unload() {
        if (!loaded) return;

        listeners.removeIf(LoadedListener::unregister);
        loaded = false;
    }

    @Override
    public Plugin getHookedPlugin() {
        return loadedHooks.size() != 1 ? null : loadedHooks.get(0);
    }

    @EventHandler
    private void onVanishConnectionEvent(SIRVanishEvent event) {
        if (!this.isEnabled()) return;

        boolean vanished = event.isVanished();

        SIRUser user = event.getUser();
        if (Key.LOGIN.isEnabled()) user.setLogged(true);

        JoinQuitHandler handler =
                plugin.getModuleManager().getModule(Key.JOIN_QUIT);
        if (handler == null) return;

        JoinQuitHandler.Type type = vanished ?
                JoinQuitHandler.Type.JOIN : JoinQuitHandler.Type.QUIT;

        JoinQuitHandler.Unit unit = handler.get(type, user);
        if (unit == null) return;

        int timer = handler.file.get("cooldown." + type.name, 0);
        Map<UUID, Long> players = handler.timeMap.getMap(
                vanished ?
                        JoinQuitHandler.Time.JOIN :
                        JoinQuitHandler.Time.QUIT
        );

        UUID uuid = user.getUuid();
        if (timer > 0 && players.containsKey(uuid)) {
            long rest = System.currentTimeMillis() - players.get(uuid);
            if (rest < timer * 1000L) return;
        }

        unit.performAllActions(user);
        if (timer > 0) players.put(uuid, System.currentTimeMillis());
    }

    @EventHandler
    private void onChatKeySentEvent(AsyncPlayerChatEvent event) {
        ConfigurationSection s = FileData.Module.Hook.VANISH
                .getFile()
                .getSection("chat-key");

        if (s == null ||
                event.isCancelled() || !this.isEnabled() ||
                !s.getBoolean("enabled") ||
                StringUtils.isBlank(s.getString("key")))
            return;

        String message = event.getMessage();
        Player player = event.getPlayer();

        List<String> list = Configurable.toStringList(s, "not-allowed");
        MessageSender sender = plugin
                .getLibrary().getLoadedSender().setTargets(player);

        final String key = s.getString("key", "");
        if (s.getBoolean("regex")) {
            Matcher match = Pattern.compile(key).matcher(message);

            if (!match.find()) {
                event.setCancelled(true);
                sender.send(list);
                return;
            }

            event.setMessage(message.replace(match.group(), ""));
            return;
        }

        String place = s.getString("place", "");
        boolean isPrefix = !place.matches("(?i)suffix");

        String pattern = (isPrefix ? "^" : "") +
                Pattern.quote(key) + (isPrefix ? "" : "$");

        Matcher match = Pattern.compile(pattern).matcher(message);

        if (!match.find()) {
            event.setCancelled(true);
            sender.send(list);
            return;
        }

        event.setMessage(message.replace(match.group(), ""));
    }

    @Getter
    class LoadedListener implements CustomListener {

        private final Status status = new Status();

        public boolean register() {
            listeners.add(this);
            return register(plugin);
        }

        public boolean unregister() {
            return CustomListener.super.unregister();
        }
    }

    @Getter
    static class SIRVanishEvent extends Event {

        @Getter
        private static final HandlerList handlerList = new HandlerList();

        private final SIRUser user;
        private final boolean vanished;

        SIRVanishEvent(SIRUser user, boolean vanished) {
            this.user = user;
            this.vanished = vanished;
        }

        public void call() {
            Bukkit.getPluginManager().callEvent(this);
        }

        @NotNull
        public HandlerList getHandlers() {
            return handlerList;
        }
    }
}
