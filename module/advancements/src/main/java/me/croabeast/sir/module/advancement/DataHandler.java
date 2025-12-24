package me.croabeast.sir.module.advancement;

import lombok.SneakyThrows;
import me.croabeast.advancement.AdvancementInfo;
import me.croabeast.common.CollectionBuilder;
import me.croabeast.common.Loadable;
import me.croabeast.scheduler.GlobalTask;
import me.croabeast.sir.DelayLogger;
import me.croabeast.sir.Timer;
import me.croabeast.sir.ExtensionFile;
import me.croabeast.takion.rule.GameRule;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

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
            DelayLogger logger = new DelayLogger()
                    .add(true,
                            "===========================",
                            "SIR Advancement Loader",
                            "===========================",
                            "&e[Advancements]"
                    );

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

            logger.add(true,
                    "- Tasks: " + tasks, "- Goals: " + goals,
                    "- Challenges: " + challenges, "- Custom: " + custom,
                    "&e[Status]",
                    "- Loaded " + information.size() + " advancements.",
                    "- Completed in " + timer.current() + " ms.",
                    "==========================="
            ).sendLines();

            GameRule<Boolean> rule = GameRule.ANNOUNCE_ADVANCEMENTS;
            for (World world : Bukkit.getWorlds()) {
                if (main.config.isProhibited(Config.Type.WORLD, world.getName()))
                    continue;

                try {
                    if (!rule.getValue(world)) continue;

                    processedWorlds.add(world);
                    rule.setValue(world, false);
                } catch (Exception ignored) {}
            }
        });
    }

    @Override
    public void unload() {
        if (!isLoaded()) return;

        GameRule<Boolean> rule = GameRule.ANNOUNCE_ADVANCEMENTS;
        for (World world : Bukkit.getWorlds()) {
            if (main.config.isProhibited(Config.Type.WORLD, world.getName()))
                continue;

            try {
                if (processedWorlds.contains(world) && !rule.getValue(world))
                    rule.setValue(world, true);
            } catch (Exception ignored) {}
        }

        task = null;
        processedWorlds.clear();
        information.clear();
    }
}
