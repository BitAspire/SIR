package me.croabeast.sir.plugin.module;

import me.croabeast.common.CollectionBuilder;
import me.croabeast.advancement.AdvancementInfo;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.common.util.ServerInfoUtils;
import me.croabeast.sir.plugin.FileData;
import me.croabeast.sir.plugin.misc.FileKey;
import me.croabeast.sir.plugin.misc.DelayLogger;
import me.croabeast.sir.plugin.user.SIRUser;
import me.croabeast.sir.plugin.LangUtils;
import me.croabeast.sir.plugin.misc.Timer;
import me.croabeast.common.WorldRule;
import me.croabeast.takion.format.PlainFormat;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Advancements extends ListenerModule {

    private final FileKey<String> data = FileData.Module.ADVANCEMENT;
    private final Loader loader;

    Advancements() {
        super(Key.ADVANCEMENTS);
        loader = new Loader();
    }

    List<String> fromDisabledList(String path) {
        return data.getFile("config").toStringList("disabled-" + path);
    }

    @Override
    public boolean register() {
        loader.load();
        return super.register();
    }

    @Override
    public boolean unregister() {
        loader.save();
        return super.unregister();
    }

    @EventHandler
    private void onBukkit(PlayerAdvancementDoneEvent event) {
        final Player player = event.getPlayer();
        SIRUser user = plugin.getUserManager().getUser(player);

        if (!this.isEnabled() || user.isVanished() ||
                fromDisabledList("worlds").contains(player.getWorld().getName()))
            return;

        for (String s : fromDisabledList("modes")) {
            String g = s.toUpperCase(Locale.ENGLISH);

            try {
                if (player.getGameMode() == GameMode.valueOf(g))
                    return;
            } catch (Exception ignored) {}
        }

        final Advancement adv = event.getAdvancement();

        String key = adv.getKey().toString();
        if (!loader.infoMap.containsKey(key) || fromDisabledList("advs").contains(key)) return;

        List<String> norms = new ArrayList<>(adv.getCriteria());
        if (norms.isEmpty()) return;

        Date date = player.getAdvancementProgress(adv).getDateAwarded(norms.get(norms.size() - 1));
        if (date != null && date.getTime() < System.currentTimeMillis() - (5 * 1000))
            return;

        ConfigurationSection section = data.getFile("lang").getSection(key.replaceAll("[/:]", "."));
        if (section == null) return;

        String messagePath = section.getString("path");
        if (StringUtils.isBlank(messagePath)) return;

        BaseInfo info = new BaseInfo(section, adv);

        String[] keys = {
                "{adv}", "{description}", "{type}", "{low-type}",
                "{cap-type}", "{item}"
        },
                values = {
                        info.title, info.description, info.frame,
                        info.frame.toLowerCase(Locale.ENGLISH),
                        WordUtils.capitalizeFully(info.frame), info.item
                };

        List<String> mList = new ArrayList<>(), cList = new ArrayList<>();
        List<String> messages = data.getFile("messages").toStringList(messagePath);

        for (String s : messages) {
            Matcher m = Pattern.compile("(?i)^\\[cmd]").matcher(s);

            if (m.find()) {
                cList.add(PlainFormat.TRIM_START_SPACES.accept(s.substring(5)));
                continue;
            }
            mList.add(s);
        }

        Set<Player> players = new HashSet<>(Bukkit.getOnlinePlayers());
        players.add(player);

        plugin.getLibrary().getLoadedSender()
                .setTargets(players)
                .setParser(player)
                .addPlaceholders(keys, values).send(mList);

        LangUtils.executeCommands(player, cList);

        Actionable actor = plugin.getModuleManager().getActionable(Key.DISCORD);
        if (actor != null) actor.accept("advances", player, keys, values);
    }

    static class BaseInfo {

        @NotNull
        private final String title, description, frame;
        private final String item;

        BaseInfo(ConfigurationSection section, Advancement adv) {
            final String key = adv.getKey().toString();

            String temp = key.substring(key.lastIndexOf('/') + 1);
            temp = temp.replace('_', ' ');

            char f = temp.toCharArray()[0];
            String first = (f + "").toUpperCase(Locale.ENGLISH);

            title = section.getString("name", first + temp.substring(1));

            description = section.getString("description", "No description.");
            frame = section.getString("frame", "TASK");

            ItemStack item = section.getItemStack("item");
            this.item = item == null ? null : item.getType().toString();
        }
    }

    class Loader {

        private final Map<String, AdvancementInfo> infoMap;
        private int taskId = -1;

        private int tasks = 0, challenges = 0, goals = 0, unknowns = 0;
        private boolean loaded = false;

        Loader() {
            infoMap = new TreeMap<>(Comparator.naturalOrder());

            CollectionBuilder.of(Bukkit.advancementIterator())
                    .filter(a -> {
                        String k = a.getKey().toString();
                        return !k.contains("recipes") &&
                                !k.contains("root");
                    })
                    .forEach(a -> {
                        String key = a.getKey().toString();
                        AdvancementInfo info = null;

                        try {
                            info = AdvancementInfo.create(a);
                        } catch (Exception ignored) {}

                        if (info == null) return;

                        infoMap.put(key, info);

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
                                unknowns++;
                                break;
                        }
                    });
        }

        void checkAdvancements() {
            if (!isEnabled()) return;

            WorldRule<Boolean> rule = WorldRule.ANNOUNCE_ADVANCEMENTS;
            List<String> list = fromDisabledList("worlds");

            for (World w : Bukkit.getWorlds()) {
                if (list.contains(w.getName())) continue;

                try {
                    if (Boolean.TRUE.equals(rule.getValue(w)))
                        rule.setValue(w, false);
                } catch (Exception ignored) {}
            }
        }

        void load() {
            if (ServerInfoUtils.SERVER_VERSION < 12) return;

            if (plugin.getWorldRuleManager().isLoaded()) checkAdvancements();
            if (loaded || taskId != -1) return;

            taskId = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                loaded = true;
                final DelayLogger logger = new DelayLogger()
                        .add(true,
                                "===========================",
                                "SIR Advancement Loader",
                                "===========================",
                                "&e[Advancements]"
                        );

                final Timer time = Timer.create(true);
                ConfigurableFile advances =
                        FileData.Module.ADVANCEMENT.getFile("lang");

                int count = 0;
                checkAdvancements();

                List<String> keys = advances.getKeys(null, false);
                for (AdvancementInfo info : infoMap.values()) {
                    Advancement adv = info.getBukkit();

                    String k = adv.getKey().toString();
                    String key = k.replaceAll("[/:]", ".");

                    if (keys.contains(key)) continue;

                    String type = (info.getFrame() + "").toLowerCase(Locale.ENGLISH);

                    advances.set(key + ".path", "type." +
                            (type.equals("unknown") ? "custom" : type));

                    advances.set(key + ".frame", type);

                    advances.set(key + ".name", info.getTitle());
                    advances.set(key + ".description", info.getDescription());

                    final ItemStack item = info.getIcon();
                    advances.set(key + ".item",
                            item == null ? null : item.getType().toString());

                    count++;
                }

                if (count > 0) advances.save();

                logger.add(true,
                        "- Tasks: " + tasks, "- Goals: " + goals,
                        "- Challenges: " + challenges
                );

                if (unknowns > 0)
                    logger.add(true,
                            "- Unknowns: " + unknowns,
                            "&o&7Check modules/advancements/lang.yml file"
                    );

                logger.add(true, "&e[Status]",
                        "- Loaded " + infoMap.size() + " advancements.",
                        "- Completed in " + time.result() + " ms.",
                        "==========================="
                ).sendLines();
            });
        }

        void save() {
            if (ServerInfoUtils.SERVER_VERSION < 12 || !isEnabled())
                return;

            List<String> list = fromDisabledList("worlds");
            WorldRule<Boolean> announces = WorldRule.ANNOUNCE_ADVANCEMENTS;

            for (World w : Bukkit.getWorlds()) {
                if (list.contains(w.getName())) continue;

                Boolean b = plugin.getWorldRuleManager().getLoadedValue(w, announces);
                try {
                    boolean v = Boolean.TRUE.equals(announces.getValue(w));
                    if ((b != null && b) && !v) announces.setValue(w, true);
                }
                catch (Exception ignored) {}
            }
        }
    }
}
