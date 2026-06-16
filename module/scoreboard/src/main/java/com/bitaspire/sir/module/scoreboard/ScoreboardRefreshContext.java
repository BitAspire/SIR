package com.bitaspire.sir.module.scoreboard;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
final class ScoreboardRefreshContext {

    private static final DecimalFormat TPS_FORMAT = new DecimalFormat("0.0");
    private static final Map<Class<?>, Method> HANDLE_METHODS = new ConcurrentHashMap<>();
    private static final Set<Class<?>> MISSING_HANDLE_METHODS = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<Class<?>, Field> PING_FIELDS = new ConcurrentHashMap<>();
    private static final Set<Class<?>> MISSING_PING_FIELDS = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static volatile boolean tpsResolved;
    private static volatile Method tpsMethod;

    private final long tick;
    private final int online;
    private final int maxOnline;
    private final String tps;
    private final Map<UUID, Integer> pings = new HashMap<>();

    static ScoreboardRefreshContext create(long tick) {
        return new ScoreboardRefreshContext(tick, Bukkit.getOnlinePlayers().size(), Bukkit.getMaxPlayers(), currentTps());
    }

    int ping(Player player) {
        UUID uuid = player.getUniqueId();
        Integer cached = pings.get(uuid);
        if (cached != null) return cached;

        int ping = resolvePing(player);
        pings.put(uuid, ping);
        return ping;
    }

    private static int resolvePing(Player player) {
        try {
            return player.getPing();
        } catch (Throwable ignored) {
            return resolveLegacyPing(player);
        }
    }

    private static int resolveLegacyPing(Player player) {
        Method method = handleMethod(player);
        return method == null ? 0 : resolveLegacyPing(player, method);
    }

    private static int resolveLegacyPing(Player player, Method method) {
        try {
            Object handle = method.invoke(player);
            Field field = handle == null ? null : pingField(handle.getClass());
            Object ping = field == null ? null : field.get(handle);
            return ping instanceof Number ? ((Number) ping).intValue() : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static Method handleMethod(Player player) {
        Class<?> type = player.getClass();
        Method cached = HANDLE_METHODS.get(type);
        if (cached != null || MISSING_HANDLE_METHODS.contains(type)) return cached;

        try {
            Method method = type.getMethod("getHandle");
            HANDLE_METHODS.put(type, method);
            return method;
        } catch (Throwable ignored) {
            MISSING_HANDLE_METHODS.add(type);
            return null;
        }
    }

    private static Field pingField(Class<?> type) {
        Field cached = PING_FIELDS.get(type);
        if (cached != null || MISSING_PING_FIELDS.contains(type)) return cached;

        try {
            Field field = type.getField("ping");
            PING_FIELDS.put(type, field);
            return field;
        } catch (Throwable ignored) {
            MISSING_PING_FIELDS.add(type);
            return null;
        }
    }

    private static String currentTps() {
        Method method = tpsMethod();
        if (method == null) return "20.0";

        try {
            Object result = method.invoke(null);
            if (result instanceof double[]) {
                double[] values = (double[]) result;
                if (values.length > 0) return TPS_FORMAT.format(values[0]);
            }
        } catch (Throwable ignored) {
        }

        return "20.0";
    }

    private static Method tpsMethod() {
        if (tpsResolved) return tpsMethod;

        try {
            tpsMethod = Bukkit.class.getMethod("getTPS");
        } catch (Throwable ignored) {
        }

        tpsResolved = true;
        return tpsMethod;
    }
}
