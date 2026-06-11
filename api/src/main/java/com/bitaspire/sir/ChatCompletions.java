package com.bitaspire.sir;

import com.bitaspire.sir.user.SIRUser;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Applies per-player custom chat completions without requiring SIR to compile against
 * modern Bukkit/Paper APIs.
 */
@RequiredArgsConstructor
public final class ChatCompletions {

    private static final String[] ADD_METHODS = {
            "addCustomChatCompletions",
            "addAdditionalChatCompletions"
    };

    private static final String[] REMOVE_METHODS = {
            "removeCustomChatCompletions",
            "removeAdditionalChatCompletions"
    };

    private final SIRApi api;
    private final Function<SIRUser, Collection<String>> supplier;

    private final Map<UUID, Set<String>> applied = new HashMap<>();

    private final Listener listener = new Listener() {
        @EventHandler(priority = EventPriority.MONITOR)
        private void onJoin(PlayerJoinEvent event) {
            api.getScheduler().runTaskLater(() -> refresh(event.getPlayer()), 1L);
        }

        @EventHandler(priority = EventPriority.MONITOR)
        private void onQuit(PlayerQuitEvent event) {
            applied.remove(event.getPlayer().getUniqueId());
        }
    };

    private boolean methodsResolved;
    private Method addMethod;
    private Method removeMethod;

    public boolean register() {
        listener.register();
        refreshAll();
        return true;
    }

    public boolean unregister() {
        clearAll();
        return listener.unregister();
    }

    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) refresh(player);
    }

    public void refresh(Player player) {
        if (player == null || !player.isOnline()) return;
        if (notSupported(player)) return;

        SIRUser user = api.getUserManager().getUser(player);
        Collection<String> supplied = user == null ? Collections.emptyList() : supplier.apply(user);
        Set<String> next = sanitize(supplied);

        UUID uuid = player.getUniqueId();
        Set<String> previous = applied.get(uuid);
        if (next.equals(previous)) return;

        if (previous != null && !previous.isEmpty() && !invoke(removeMethod, player, previous))
            return;

        if (next.isEmpty()) {
            applied.remove(uuid);
            return;
        }

        if (invoke(addMethod, player, next)) applied.put(uuid, next);
        else applied.remove(uuid);
    }

    public void clearAll() {
        for (Player player : Bukkit.getOnlinePlayers()) clear(player);
        applied.clear();
    }

    public void clear(Player player) {
        if (player == null) return;
        if (notSupported(player)) return;

        Set<String> previous = applied.remove(player.getUniqueId());
        if (previous != null && !previous.isEmpty())
            invoke(removeMethod, player, previous);
    }

    private Set<String> sanitize(Collection<String> values) {
        if (values == null || values.isEmpty()) return Collections.emptySet();

        Set<String> completions = new LinkedHashSet<>();
        for (String value : values) {
            if (StringUtils.isBlank(value)) continue;

            String trimmed = value.trim();
            if (trimmed.indexOf(' ') >= 0) continue;

            completions.add(trimmed);
        }
        return completions;
    }

    private boolean notSupported(Player player) {
        resolveMethods(player);
        return addMethod == null || removeMethod == null;
    }

    private void resolveMethods(Player player) {
        if (methodsResolved || player == null) return;

        addMethod = findMethod(player, ADD_METHODS);
        removeMethod = findMethod(player, REMOVE_METHODS);
        methodsResolved = true;
    }

    private Method findMethod(Player player, String[] names) {
        Class<?> type = player.getClass();
        for (String name : names) {
            try {
                return type.getMethod(name, Collection.class);
            } catch (NoSuchMethodException ignored) {}
        }
        return null;
    }

    private boolean invoke(Method method, Player player, Collection<String> completions) {
        try {
            method.invoke(player, completions);
            return true;
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException ignored) {
            return false;
        }
    }
}
