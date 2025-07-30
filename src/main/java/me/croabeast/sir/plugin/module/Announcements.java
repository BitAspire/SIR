package me.croabeast.sir.plugin.module;

import lombok.Getter;
import me.croabeast.common.CollectionBuilder;
import me.croabeast.command.BaseCommand;
import me.croabeast.command.TabBuilder;
import me.croabeast.file.Configurable;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.sir.plugin.Commandable;
import me.croabeast.sir.plugin.FileData;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.command.SIRCommand;
import me.croabeast.sir.plugin.misc.FileKey;
import me.croabeast.sir.plugin.user.SIRUser;
import me.croabeast.sir.plugin.LangUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

final class Announcements extends SIRModule implements Actionable, Commandable {

    private final Map<Integer, Announce> announcesMap = new HashMap<>();
    private final FileKey<String> key;

    private boolean running = false;
    private int taskId = -1, order = 0;

    @Getter
    final Set<SIRCommand> commands = new HashSet<>();

    Announcements() {
        super(Key.ANNOUNCEMENTS);
        key = FileData.Module.ANNOUNCEMENT;

        commands.add(new SIRCommand(this, SIRCommand.Key.ANNOUNCER) {
            {
                editSubCommand("help", (sender, args) -> {
                    if (args.length > 0)
                        return isWrongArgument(sender, args[args.length - 1]);

                    return createSender(sender).send("help");
                });

                editSubCommand("preview", (sender, args) -> {
                    if (!(sender instanceof Player)) {
                        plugin.getLibrary().getLogger()
                                .log("&cYou can't preview an announce in console.");
                        return true;
                    }

                    if (args.length == 1) {
                        accept(args[0]);
                        return true;
                    }
                    return createSender(sender).send("select");
                });
            }

            @NotNull
            protected ConfigurableFile getLang() {
                return FileData.Command.ANNOUNCER.getFile();
            }

            @Override
            protected boolean execute(CommandSender sender, String[] args) {
                return createSender(sender).send("help");
            }

            @NotNull
            public TabBuilder getCompletionBuilder() {
                final TabBuilder builder = createBasicTabBuilder();

                for (BaseCommand sub : getSubCommands()) {
                    Deque<String> list = new LinkedList<>(sub.getAliases());
                    list.addFirst(sub.getName());

                    builder.addArguments(0,
                            (s, a) -> sub.isPermitted(s), list);
                }

                ConfigurationSection id = key.getFile("announces").getSection("announces");
                return id != null ?
                        builder.addArguments(1,
                                (s, a) -> a[0].matches("(?i)preview"),
                                id.getKeys(false)
                        ) : builder;
            }
        });
    }

    Set<SIRUser> getUsers(ConfigurationSection c) {
        String perm = c.getString("permission", "DEFAULT");

        List<String> worlds = Configurable.toStringList(c, "worlds");
        Set<SIRUser> users = new HashSet<>();

        for (Player p : Bukkit.getOnlinePlayers()) {
            String world = p.getWorld().getName();
            if (!worlds.isEmpty() && !worlds.contains(world))
                continue;

            SIRUser user = plugin.getUserManager().getUser(p);
            if (user == null) continue;

            if (!user.isVanished() && user.hasPermission(perm))
                users.add(user);
        }

        return users;
    }

    private boolean start() {
        ConfigurationSection section = key.getFile("announces").getSection("announces");
        int delay = key.getFile().get("interval", 0);

        if (!isEnabled() || delay <= 0 || section == null || running)
            return false;

        taskId = SIRPlugin.getScheduler().runTaskTimer(
                () -> {
                    int count = announcesMap.size() - 1;
                    if (order > count) order = 0;

                    Announce a = announcesMap.get(order);
                    a.display(getUsers(a.id));

                    if (key.getFile().get("random", false)) {
                        order = new Random().nextInt(count + 1);
                        return;
                    }

                    order = order < count ? (order + 1) : 0;
                },
                0L, delay
        ).getTaskId();

        return running = true;
    }

    private boolean stop() {
        if (isEnabled() || !running) return false;

        SIRPlugin.getScheduler().cancel(taskId);
        running = false;
        return true;
    }

    @Override
    public boolean register() {
        ConfigurationSection section = key.getFile("announces").getSection("announces");
        if (section == null) return false;

        announcesMap.clear();
        int index = 0;

        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s != null) announcesMap.put(index++, new Announce(s));
        }

        return start();
    }

    @Override
    public boolean unregister() {
        return stop();
    }

    @Override
    public void accept(Object... objects) {
        if (Actionable.failsCheck(objects, String.class))
            return;

        for (Announce a : announcesMap.values()) {
            ConfigurationSection c = a.id;
            if (!c.getName().equals(objects[0]))
                continue;

            a.display(getUsers(c));
            break;
        }
    }

    private class Announce {

        private final ConfigurationSection id;

        private final String sound;
        private final List<String> lines, commands;

        private Announce(ConfigurationSection id) {
            this.id = id;

            sound = id.getString("sound");
            commands = Configurable.toStringList(id, "commands");
            lines = Configurable.toStringList(id, "lines");
        }

        void display(Set<SIRUser> users) {
            if (users.isEmpty()) return;

            lines.replaceAll(plugin.getLibrary().getCharacterManager()::align);

            plugin.getLibrary().getLoadedSender()
                    .setTargets(CollectionBuilder
                            .of(users)
                            .map(SIRUser::getPlayer).toSet())
                    .send(lines);

            users.forEach(u -> u.playSound(sound));
            LangUtils.executeCommands(null, commands);
        }
    }
}
