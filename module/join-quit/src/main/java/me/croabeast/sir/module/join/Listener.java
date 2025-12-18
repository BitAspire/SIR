package me.croabeast.sir.module.join;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import me.croabeast.sir.user.SIRUser;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class Listener extends me.croabeast.sir.Listener {

    private final JoinQuit main;
    final CooldownMap map = new CooldownMap();

    void executeActions(SIRUser user, boolean join) {
        MessageUnit unit = main.messages.get(user, join);
        if (unit == null) return;

        unit.execute(user);

        if (join) {
            map.put(Type.JOIN, user.getUuid());
            map.put(Type.BETWEEN, user.getUuid());
            return;
        }

        map.put(Type.QUIT, user.getUuid());
    }

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        if (!main.isEnabled()) return;

        Player player = event.getPlayer();
        SIRUser user = main.getApi().getUserManager().getUser(player);

        MessageUnit unit = main.messages.get(user, true);
        if (unit == null) return;

        if (main.config.isJoinDisabled()) event.setJoinMessage(null);

        UUID uuid = player.getUniqueId();
        if (map.isStillOnCooldown(Type.JOIN, uuid) || user.isVanished()) return;

        if (main.getApi().getModuleManager().isEnabled("Login") && main.config.isSpawnBeforeLogin()) {
            unit.teleport(user);
            return;
        }

        unit.execute(user);
        map.put(Type.JOIN, uuid);
        map.put(Type.BETWEEN, uuid);
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        SIRUser user = main.getApi().getUserManager().getUser(player);
        if (!user.isLogged()) return;

        user.getImmuneData().giveImmunity(0);
        if (!main.isEnabled()) return;

        MessageUnit unit = main.messages.get(user, false);
        if (unit == null) return;

        if (main.config.isJoinDisabled()) event.setQuitMessage(null);

        UUID uuid = player.getUniqueId();
        if (map.isStillOnCooldown(Type.QUIT, uuid) ||
                map.isStillOnCooldown(Type.BETWEEN, uuid)) return;

        if (user.isVanished()) return;

        user.setLogged(false);
        unit.execute(user);

        map.put(Type.QUIT, uuid);
    }

    enum Type {
        JOIN,
        BETWEEN,
        QUIT
    }

    class CooldownMap {

        private final Map<UUID, Long> join = new HashMap<>(),
                quit = new HashMap<>(),
                between = new HashMap<>();

        Map<UUID, Long> getCooldowns(Type type) {
            switch (type) {
                case JOIN: return join;
                case BETWEEN: return between;
                case QUIT: return quit;
                default: return new HashMap<>();
            }
        }

        int getConfigValue(Type type) {
            switch (type) {
                case JOIN: return main.config.getJoinCooldown();
                case BETWEEN: return main.config.getBetweenCooldown();
                case QUIT: return main.config.getQuitCooldown();
                default: return 0;
            }
        }

        void put(Type type, UUID uuid) {
            if (getConfigValue(type) > 0) getCooldowns(type).put(uuid, System.currentTimeMillis());
        }

        boolean isStillOnCooldown(Type type, UUID uuid) {
            int cooldown = getConfigValue(type);
            return cooldown > 0 && getCooldowns(type).containsKey(uuid) &&
                    System.currentTimeMillis() - getCooldowns(type).get(uuid) < cooldown * 1000L;
        }
    }
}
