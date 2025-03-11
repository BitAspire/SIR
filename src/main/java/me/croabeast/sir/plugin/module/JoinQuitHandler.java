package me.croabeast.sir.plugin.module;

import lombok.Getter;
import me.croabeast.lib.file.Configurable;
import me.croabeast.lib.file.UnitMappable;
import me.croabeast.sir.api.file.ConfigUnit;
import me.croabeast.sir.plugin.FileData;
import me.croabeast.sir.plugin.misc.FileKey;
import me.croabeast.sir.plugin.misc.SIRUser;
import me.croabeast.sir.plugin.LangUtils;
import me.croabeast.takion.message.AnimatedBossbar;
import me.croabeast.takion.message.MessageSender;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

final class JoinQuitHandler extends ListenerModule {

    final Map<UUID, Long> joins, quits, plays;
    final Map<Type, UnitMappable<ConnectionUnit>> unitMap;

    final FileKey<String> key;

    JoinQuitHandler() {
        super(Key.JOIN_QUIT);

        key = FileData.Module.JOIN_QUIT;

        joins = new HashMap<>();
        quits = new HashMap<>();
        plays = new HashMap<>();
        unitMap = new HashMap<>();
    }

    @Override
    public boolean register() {
        unitMap.clear();

        for (Type type : Type.values())
            unitMap.put(type, key.getFile("messages").asUnitMap(
                    type.name,
                    s -> new ConnectionUnit(s, type)
            ));

        return super.register();
    }

    ConnectionUnit get(Type type, SIRUser user) {
        UnitMappable<ConnectionUnit> loaded = unitMap.get(type);
        if (loaded.isEmpty()) return null;

        for (Map.Entry<Integer, Set<ConnectionUnit>> maps : loaded.entrySet())
            for (ConnectionUnit unit : maps.getValue())
                if (user.hasPerm(unit.getPermission())) return unit;

        return null;
    }

    void performConnectionActions(ConnectionUnit unit, SIRUser user) {
        UUID uuid = user.getUuid();

        long data = System.currentTimeMillis();
        unit.performAllActions(user);

        if (key.getFile().get("cooldown.join", 0) > 0)
            joins.put(uuid, data);

        if (key.getFile().get("cooldown.between", 0) > 0)
            plays.put(uuid, data);
    }

    @EventHandler
    private void onJoinConnectionEvent(PlayerJoinEvent event) {
        if (!this.isEnabled()) return;

        final Player player = event.getPlayer();
        Type type = player.hasPlayedBefore() ? Type.JOIN : Type.FIRST;

        SIRUser user = plugin.getUserManager().getUser(player);

        final ConnectionUnit unit = get(type, user);
        if (unit == null) return;

        if (key.getFile().get("default-messages.disable-join", true))
            event.setJoinMessage(null);

        int joinTime = key.getFile().get("cooldown.join", 0);
        UUID uuid = player.getUniqueId();

        if (joinTime > 0 && joins.containsKey(uuid)) {
            long rest = System.currentTimeMillis() - joins.get(uuid);
            if (rest < joinTime * 1000L) return;
        }

        if (user.isVanished()) return;

        if (Key.LOGIN.isEnabled() && FileData.Module.Hook.LOGIN
                .getFile()
                .get("spawn-before", false))
        {
            unit.teleportToSpawn(user);
            return;
        }

        performConnectionActions(unit, user);
    }

    @EventHandler
    private void onQuitConnectionEvent(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        SIRUser user = plugin.getUserManager().getUser(player);

        AnimatedBossbar.unregisterAll();
        user.giveImmunity(0);
        //Conversation.remove(player);

        if (!this.isEnabled()) return;

        ConnectionUnit unit = get(Type.QUIT, user);
        if (unit == null) return;

        if (key.getFile().get("default-messages.disable-quit", true))
            event.setQuitMessage(null);

        UUID uuid = player.getUniqueId();

        int playTime = key.getFile().get("cooldown.between", 0);
        int quitTime = key.getFile().get("cooldown.quit", 0);

        final long now = System.currentTimeMillis();

        if (quitTime > 0 && quits.containsKey(uuid) &&
                now - quits.get(uuid) < quitTime * 1000L) return;

        if (playTime > 0 && plays.containsKey(uuid) &&
                now - plays.get(uuid) < playTime * 1000L) return;

        if (user.isVanished()) return;

        user.setLogged(true);
        unit.performAllActions(user);

        if (quitTime > 0) quits.put(uuid, System.currentTimeMillis());
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

    class ConnectionUnit implements ConfigUnit {

        @Getter
        private final ConfigurationSection section;
        private final Type type;

        private final List<String> publicList;
        private final List<String> commandList;

        private List<String> privateList;

        private String sound;
        private int invulnerability;

        private ConfigurationSection spawn;

        ConnectionUnit(ConfigurationSection section, Type type) {
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

            LangUtils.executeCommands(
                    type != Type.QUIT ? player : null, commandList
            );

            Actionable actor = plugin.getModuleManager().getActionable(Key.DISCORD);
            if (actor != null)
                actor.act(type.name, player, new String[0], new String[0]);
        }

        void teleportToSpawn(SIRUser user) {
            user.teleport(spawn);
        }
    }
}
