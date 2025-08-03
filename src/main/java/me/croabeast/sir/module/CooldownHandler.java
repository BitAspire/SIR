package me.croabeast.sir.module;

import lombok.Getter;
import me.croabeast.file.Configurable;
import me.croabeast.file.UnitMappable;
import me.croabeast.sir.PermissibleUnit;
import me.croabeast.sir.FileData;
import me.croabeast.sir.LangUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.*;

final class CooldownHandler extends ListenerModule {

    private final Map<Player, Integer> checkMap = new HashMap<>();
    private final Map<Player, Long> timers = new HashMap<>();

    private UnitMappable.Set<CooldownUnit> units;

    CooldownHandler() {
        super(Key.COOLDOWNS);
    }

    @Override
    public boolean register() {
        units = FileData.Module.Chat.COOLDOWNS
                .getFile()
                .asUnitMap("cooldowns", CooldownUnit::new);
        return super.register();
    }

    private CooldownUnit get(Player player) {
        for (Set<CooldownUnit> set : units.values())
            for (CooldownUnit unit : set)
                if (unit.isInGroupNonNull(player) && unit.hasPermission(player))
                    return unit;

        return null;
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onBukkit(AsyncPlayerChatEvent event) {
        if (event.isCancelled() || !this.isEnabled())
            return;

        Player player = event.getPlayer();
        Long data = timers.get(player);

        CooldownUnit unit = get(player);
        if (unit == null) return;

        final int time = unit.getTime();
        if (time <= 0 || data == null) return;

        long rest = System.currentTimeMillis() - data;
        if (rest >= time * 1000L) return;

        event.setCancelled(true);
        int result = Math.round(rest / 1000F);

        if (unit.canCheck()) {
            int tempCount = checkMap.getOrDefault(player, 0);

            if (rest >= unit.checks.timeLimit * 1000L &&
                    tempCount >= unit.checks.count)
            {
                List<String> commands = unit.checks.commands;
                LangUtils.executeCommands(player, commands);

                checkMap.put(player, ++tempCount);
            }
        }

        final int temp = time - result;

        plugin.getLibrary().getLoadedSender()
                .addPlaceholder("{time}", temp)
                .setLogger(false)
                .setTargets(player)
                .send(unit.getMessages());

        timers.put(player, System.currentTimeMillis());
    }

    private static class Checks {

        private final boolean enabled;
        private final List<String> commands;
        private final int count;
        private final int timeLimit;

        private Checks(ConfigurationSection s) {
            this.enabled = s.getBoolean("enabled");
            this.commands = Configurable.toStringList(s, "commands");
            this.count = s.getInt("count");
            this.timeLimit = s.getInt("time-limit");
        }
    }

    @Getter
    private static class CooldownUnit implements PermissibleUnit {

        private final ConfigurationSection section;

        private Checks checks = null;
        private final int time;
        private final List<String> messages;

        private CooldownUnit(ConfigurationSection section) {
            this.section = section;

            this.messages = Configurable.toStringList(section, "messages");
            this.time = section.getInt("time");

            ConfigurationSection s = section.getConfigurationSection("check");
            if (s != null) this.checks = new Checks(s);
        }

        public boolean canCheck() {
            return checks != null && checks.enabled;
        }
    }
}
