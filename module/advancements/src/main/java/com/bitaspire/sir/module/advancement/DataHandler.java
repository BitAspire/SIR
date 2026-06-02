package com.bitaspire.sir.module.advancement;

import lombok.SneakyThrows;
import me.croabeast.advancement.AdvancementInfo;
import me.croabeast.common.CollectionBuilder;
import me.croabeast.common.Loadable;
import me.croabeast.scheduler.GlobalTask;
import com.bitaspire.sir.DelayLogger;
import com.bitaspire.sir.Timer;
import com.bitaspire.sir.file.ExtensionFile;
import me.croabeast.vnc.VNC;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

final class DataHandler implements Loadable {

    private final Advancements main;
    final Map<Advancement, AdvancementInfo> information = new HashMap<>();
    final Map<Advancement, ConfigurationSection> sections = new HashMap<>();

    private final ExtensionFile data;
    private GlobalTask task;

    private int tasks = 0, challenges = 0, goals = 0, custom = 0;
    private final Set<World> processedWorlds = new HashSet<>();

    @SneakyThrows
    DataHandler(Advancements main) {
        this.main = main;

        CollectionBuilder.of(Bukkit.advancementIterator())
                .filter(advancement -> {
                    String key = advancement.getKey().toString().toLowerCase(Locale.ROOT);
                    return !key.contains("recipes") && !key.contains("root");
                })
                .forEach(advancement -> {
                    final AdvancementInfo info;
                    try {
                        if ((info = AdvancementInfo.create(advancement)) == null)
                            return;
                    } catch (Exception e) {
                        return;
                    }

                    switch (info.getFrame()) {
                        case TASK:
                            tasks++;
                            break;

                        case CHALLENGE:
                            challenges++;
                            break;

                        case GOAL:
                            goals++;
                            break;

                        default:
                            custom++;
                            break;
                    }
                    this.information.put(advancement, info);
                });

        data = new ExtensionFile(main, "data", true);
    }

    @Override
    public boolean isLoaded() {
        return task != null;
    }

    @Override
    public void load() {
        if (isLoaded()) return;

        task = main.getApi().getScheduler().runTask(() -> {
            if (data == null) return;

            Timer timer = Timer.create();
            final List<String> keys = data.getKeys("data");
            int count = 0;

            for (final AdvancementInfo info : information.values()) {
                Advancement advancement = info.getBukkit();

                String key = advancement.getKey().toString().replaceAll("[/:]", ".");
                if (keys.contains(key)) continue;

                key = "data." + key;
                String type = info.getFrame().toString().toLowerCase(Locale.ENGLISH);

                data.set(key + ".path", type.equals("unknown") ? "custom" : type);

                data.set(key + ".frame", type);
                data.set(key + ".title", info.getTitle());
                data.set(key + ".description", info.getDescription());

                ItemStack item = info.getIcon();
                data.set(key + ".item", item == null ? null : item.getType().toString());

                ConfigurationSection section = data.getSection(key);
                if (section != null) sections.put(advancement, section);
                count++;
            }

            if (count > 0) data.save();

            if (isVerboseStartupLog())
                new DelayLogger()
                        .add(true,
                                "===========================",
                                "SIR Advancement Loader",
                                "===========================",
                                "&e[Advancements]",
                                "- Tasks: " + tasks, "- Goals: " + goals,
                                "- Challenges: " + challenges, "- Custom: " + custom,
                                "&e[Status]",
                                "- Loaded " + information.size() + " advancements.",
                                "- Completed in " + timer.current() + " ms.",
                                "==========================="
                        ).sendLines();

            if (supportsGameRules())
                GameRuleSupport.disableAnnouncements(main, processedWorlds);
        });
    }

    @Override
    public void unload() {
        if (!isLoaded()) return;

        if (supportsGameRules())
            GameRuleSupport.restoreAnnouncements(main, processedWorlds);

        task = null;
        processedWorlds.clear();
        information.clear();
    }

    private static boolean supportsGameRules() {
        return !VNC.isBefore("1.12") && hasClass("org.bukkit.GameRule");
    }

    private boolean isVerboseStartupLog() {
        if (!(main.getApi().getPlugin() instanceof JavaPlugin)) return false;

        JavaPlugin plugin = (JavaPlugin) main.getApi().getPlugin();
        return "verbose".equalsIgnoreCase(plugin.getConfig().getString("options.startup-logs.console", "summary"));
    }

    private static boolean hasClass(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static final class GameRuleSupport {

        private static void disableAnnouncements(Advancements main, Set<World> processedWorlds) {
            me.croabeast.takion.rule.GameRule<Boolean> rule = me.croabeast.takion.rule.GameRule.ANNOUNCE_ADVANCEMENTS;
            for (World world : Bukkit.getWorlds()) {
                if (main.config.isProhibited(Config.Type.WORLD, world.getName()))
                    continue;

                try {
                    if (!rule.getValue(world)) continue;

                    processedWorlds.add(world);
                    rule.setValue(world, false);
                } catch (Exception ignored) {}
            }
        }

        private static void restoreAnnouncements(Advancements main, Set<World> processedWorlds) {
            me.croabeast.takion.rule.GameRule<Boolean> rule = me.croabeast.takion.rule.GameRule.ANNOUNCE_ADVANCEMENTS;
            for (World world : Bukkit.getWorlds()) {
                if (main.config.isProhibited(Config.Type.WORLD, world.getName()))
                    continue;

                try {
                    if (processedWorlds.contains(world) && !rule.getValue(world))
                        rule.setValue(world, true);
                } catch (Exception ignored) {}
            }
        }
    }
}
