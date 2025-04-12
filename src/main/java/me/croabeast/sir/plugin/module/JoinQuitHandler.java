package me.croabeast.sir.plugin.module;

import lombok.Getter;
import me.croabeast.file.Configurable;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.file.UnitMappable;
import me.croabeast.sir.api.file.PermissibleUnit;
import me.croabeast.sir.plugin.FileData;
import me.croabeast.sir.plugin.misc.SIRUser;
import me.croabeast.sir.plugin.LangUtils;
import me.croabeast.takion.message.MessageSender;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

final class JoinQuitHandler extends ListenerModule {

    final Map<Type, UnitMappable.Set<Unit>> unitMap = new HashMap<>();
    final TimeMap timeMap = new TimeMap();

    final ConfigurableFile file = FileData.Module.JOIN_QUIT.getFile();

    JoinQuitHandler() {
        super(Key.JOIN_QUIT);
    }

    @Override
    public boolean register() {
        unitMap.clear();

        ConfigurableFile messages = FileData.Module.JOIN_QUIT.getFile("messages");
        for (Type type : Type.values())
            unitMap.put(type, messages.asUnitMap(type.name, s -> new Unit(s, type)));

        return super.register();
    }

    Unit get(Type type, SIRUser user) {
        UnitMappable.Set<Unit> loaded = unitMap.get(type);
        if (loaded.isEmpty()) return null;

        for (Set<Unit> maps : loaded.values())
            for (Unit unit : maps)
                if (user.hasPerm(unit.getPermission()))
                    return unit;

        return null;
    }

    void performUnitActions(Unit unit, SIRUser user) {
        final UUID uuid = user.getUuid();
        unit.performAllActions(user);

        if (file.get("cooldown.join", 0) > 0)
            timeMap.put(Time.JOIN, uuid);

        if (file.get("cooldown.between", 0) > 0)
            timeMap.put(Time.BETWEEN, uuid);
    }

    @EventHandler
    private void onJoinConnectionEvent(PlayerJoinEvent event) {
        if (!this.isEnabled()) return;

        final Player player = event.getPlayer();
        Type type = player.hasPlayedBefore() ? Type.JOIN : Type.FIRST;

        SIRUser user = plugin.getUserManager().getUser(player);

        final Unit unit = get(type, user);
        if (unit == null) return;

        if (file.get("default-messages.disable-join", true))
            event.setJoinMessage(null);

        int joinTime = file.get("cooldown.join", 0);
        UUID uuid = player.getUniqueId();

        if (joinTime > 0 && timeMap.contains(Time.JOIN, uuid)) {
            long loaded = timeMap.get(Time.JOIN, uuid);
            long rest = System.currentTimeMillis() - loaded;
            if (rest < joinTime * 1000L) return;
        }

        if (user.isVanished()) return;

        if (Key.LOGIN.isEnabled() && FileData.Module.Hook.LOGIN
                .getFile()
                .get("spawn-before", false)) {
            unit.teleportToSpawn(user);
            return;
        }

        performUnitActions(unit, user);
    }

    @EventHandler
    private void onQuitConnectionEvent(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        SIRUser user = plugin.getUserManager().getUser(player);

        user.giveImmunity(0);
        if (!this.isEnabled()) return;

        Unit unit = get(Type.QUIT, user);
        if (unit == null) return;

        if (file.get("default-messages.disable-quit", true))
            event.setQuitMessage(null);

        UUID uuid = player.getUniqueId();

        int playTime = file.get("cooldown.between", 0);
        int quitTime = file.get("cooldown.quit", 0);

        final long now = System.currentTimeMillis();

        if (quitTime > 0 && timeMap.contains(Time.QUIT, uuid) &&
                now - timeMap.get(Time.QUIT, uuid) < quitTime * 1000L)
            return;

        if (playTime > 0 && timeMap.contains(Time.BETWEEN, uuid) &&
                now - timeMap.get(Time.BETWEEN, uuid) < playTime * 1000L)
            return;

        if (user.isVanished()) return;

        user.setLogged(true);
        unit.performAllActions(user);

        if (quitTime > 0) timeMap.put(Time.QUIT, uuid);
    }

    enum Time {
        JOIN,
        QUIT,
        BETWEEN
    }

    final static class TimeMap {

        private final Map<Time, Map<UUID, Long>> map = new HashMap<>();

        Map<UUID, Long> getMap(Time time) {
            return this.map.computeIfAbsent(time, k -> new HashMap<>());
        }

        boolean contains(Time time, UUID uuid) {
            return getMap(time).containsKey(uuid);
        }

        void put(Time time, UUID uuid) {
            getMap(time).put(uuid, System.currentTimeMillis());
        }

        long get(Time time, UUID uuid) {
            return getMap(time).get(uuid);
        }
    }

    enum Type {
        FIRST("first-join"),
        JOIN("join"),
        QUIT("quit");

        final String name;

        Type(String name) {
            this.name = name;
        }
    }

    class Unit implements PermissibleUnit {

        @Getter
        final ConfigurationSection section;
        private final Type type;

        private final List<String> publicList;
        private final List<String> commandList;

        private List<String> privateList;

        private String sound;
        private int invulnerability;

        private ConfigurationSection spawn;

        Unit(ConfigurationSection section, Type type) {
            this.section = section;
            this.type = type;

            publicList = Configurable.toStringList(section, "public");
            commandList = Configurable.toStringList(section, "commands");
            if (type == Type.QUIT) return;

            privateList = Configurable.toStringList(section, "private");

            sound = section.getString("sound");
            invulnerability = section.getInt("invulnerable");
            spawn = section.getConfigurationSection("spawn-location");
        }

        void performAllActions(SIRUser user) {
            MessageSender sender = plugin.getLibrary().getLoadedSender();
            Player player = user.getPlayer();

            sender.copy().setParser(player)
                    .setTargets(Bukkit.getOnlinePlayers())
                    .send(publicList);

            if (type != Type.QUIT) {
                sender.copy().setTargets(player).send(privateList);

                user.giveImmunity(invulnerability);
                user.playSound(sound);

                teleportToSpawn(user);
            }

            LangUtils.executeCommands(type != Type.QUIT ? player : null, commandList);

            Actionable actor = plugin.getModuleManager().getActionable(Key.DISCORD);
            if (actor != null) actor.act(type.name, player, new String[0], new String[0]);
        }

        void teleportToSpawn(SIRUser user) {
            user.teleport(spawn);
        }

        @Override
        public String toString() {
            return "Unit{type=" + type + ", path='" + getName() + "'}";
        }
    }
}
