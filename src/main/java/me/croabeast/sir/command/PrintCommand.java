package me.croabeast.sir.command;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import me.croabeast.common.CollectionBuilder;
import me.croabeast.command.TabBuilder;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.sir.module.PlayerFormatter;
import me.croabeast.sir.FileData;
import me.croabeast.sir.manager.ModuleManager;
import me.croabeast.sir.module.SIRModule;
import me.croabeast.sir.user.SIRUser;
import me.croabeast.sir.manager.UserManager;
import me.croabeast.sir.LangUtils;
import me.croabeast.takion.TakionLib;
import me.croabeast.takion.channel.Channel;
import me.croabeast.takion.channel.ChannelManager;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

final class PrintCommand extends SIRCommand {

    PrintCommand() {
        super(Key.PRINT, true);

        editSubCommand("targets", (sender, args) -> args.length == 0 ?
                Utils.create(this, sender).send("help.targets") :
                getArgumentCheck().test(sender, args[args.length - 1]));

        editSubCommand("chat", (sender, args) -> {
            TargetCatcher catcher = new TargetCatcher(this, sender, args.length > 0 ? args[0] : null);

            if (args.length == 0)
                return Utils.create(this, sender).send("help.chat");
            if (args.length < 3)
                return Utils.create(this, sender).send("empty-message");

            catcher.sendConfirmation();

            boolean hasArg = args[1].matches("(?i)DEFAULT|CENTERED|MIXED");
            new Printer(catcher, args, hasArg ? 2 : 1).print("");

            return true;
        });

        editSubCommand("action-bar", (sender, args) -> {
            TargetCatcher catcher = new TargetCatcher(this, sender, args.length > 0 ? args[0] : null);

            if (args.length == 0)
                return Utils.create(this, sender).send("help.action-bar");
            if (args.length < 2)
                return Utils.create(this, sender).send("empty-message");

            catcher.sendConfirmation();
            new Printer(catcher, args, 1).print("ACTION-BAR");

            return true;
        });

        editSubCommand("title", (sender, args) -> {
            TargetCatcher catcher = new TargetCatcher(this, sender, args.length > 0 ? args[0] : null);

            if (args.length == 0)
                return Utils.create(this, sender).send("help.title");
            if (args.length < 2)
                return Utils.create(this, sender).send("empty-message");

            catcher.sendConfirmation();
            new Printer(catcher, args, 2).print("TITLE");

            return true;
        });
    }

    @NotNull
    protected ConfigurableFile getLang() {
        return FileData.Command.PRINT.getFile();
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, String[] args) {
        return Utils.create(this, sender).send("help.main");
    }

    @NotNull
    public TabBuilder getCompletionBuilder() {
        TabBuilder builder = Utils.newBuilder()
                .addArgument(0, "sir.print.targets", "targets")
                .addArgument(0, "sir.print.chat", "chat")
                .addArgument(0, "sir.print.action-bar", "action-bar")
                .addArgument(0, "sir.print.title", "title")
                .addArguments(1, "@a", "perm:", "world:")
                .addArguments(1,
                        CollectionBuilder.of(plugin.getUserManager().getOnlineUsers())
                                .filter(SIRUser::isVanished)
                                .map(SIRUser::getName).toList());

        if (plugin.getChat().isEnabled())
            builder.addArgument(1, "group:");

        builder
                .addArgument(2,
                        (s, a) -> a[0].matches("(?i)action-bar"),
                        "<message>"
                )
                .addArguments(2,
                        (s, a) -> a[0].matches("(?i)chat"),
                        "default", "centered", "mixed"
                )
                .addArguments(2,
                        (s, a) -> a[0].matches("(?i)title"),
                        "default", "10,50,10"
                );

        return builder.addArgument(3,
                (s, a) -> a[0].matches("(?i)chat|title"),
                "<message>"
        );
    }

    private class TargetCatcher {

        private final SIRCommand command;
        private final CommandSender sender;
        private final String input;

        private Set<Player> targets = null;

        TargetCatcher(SIRCommand command, CommandSender sender, String input) {
            this.command = command;
            this.sender = sender;
            this.input = input;

            loadAllTargets();
        }

        private Set<Player> loadAllTargets() {
            if (targets != null) return targets;

            if (StringUtils.isBlank(input)) return new HashSet<>();

            Set<Player> targets = new HashSet<>();
            boolean notLoaded = true;

            Player player = Bukkit.getPlayer(input);

            if (player == sender || player != null) {
                targets = Sets.newHashSet(player);
                notLoaded = false;
            }

            if (input.matches("@[Aa]")) {
                targets = new HashSet<>(Bukkit.getOnlinePlayers());
                notLoaded = false;
            }

            String[] array = input.split(":", 2);
            final String id = array[0].toUpperCase(Locale.ENGLISH);

            if (notLoaded) {
                CollectionBuilder<Player> stream =
                        CollectionBuilder.of(Bukkit.getOnlinePlayers()).map(p -> p);

                switch (id) {
                    case "WORLD":
                        World w = Bukkit.getWorld(array[1]);

                        targets = w != null ?
                                new HashSet<>(w.getPlayers()) :
                                new HashSet<>();
                        break;

                    case "PERM":
                        targets = stream
                                .filter(p -> UserManager.hasPermission(p, array[1]))
                                .toSet();
                        break;

                    case "GROUP":
                        targets = stream
                                .filter(p -> {
                                    String group = plugin.getChat().getPrimaryGroup(p);
                                    return group != null && group.matches("(?i)" + array[1]);
                                })
                                .toSet();
                        break;

                    default: break;
                }}

            return this.targets =
                    CollectionBuilder.of(targets).filter(Objects::nonNull).toSet();
        }

        private boolean sendConfirmation() {
            if (targets.isEmpty()) {
                Utils.create(command, sender).addPlaceholder("{target}", input).send("reminder.empty");
                return false;
            }

            if (targets.size() == 1) {
                String target = Lists.newArrayList(targets).get(0).getName();

                return (!(sender instanceof Player) || !targets.contains(sender)) &&
                        Utils.create(command, sender)
                                .addPlaceholder("{target}", target)
                                .send("reminder.success");
            }

            return Utils.create(command, sender).addPlaceholder("{target}", input).send("reminder.success");
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private class Printer {

        private final TargetCatcher catcher;
        private final String[] args;
        private final int index;

        private final TakionLib library = plugin.getLibrary();

        private void print(String key) {
            String message = LangUtils.stringFromArray(args, index);
            String center = library.getCenterPrefix();

            ChannelManager channelManager = library.getChannelManager();
            ModuleManager moduleManager = plugin.getModuleManager();

            PlayerFormatter<?> emojis = moduleManager.getFormatter(SIRModule.Key.EMOJIS);
            PlayerFormatter<?> tags = moduleManager.getFormatter(SIRModule.Key.TAGS);

            for (Player player : catcher.targets) {
                final Channel channel = channelManager.identify(key);
                if (channel == channelManager.identify("chat")) {
                    String[] array = library.splitString(message);

                    for (int i = 0; i < array.length; i++) {
                        final String s = array[i];

                        if (args[2].matches("(?i)CENTERED") &&
                                !s.startsWith(center)) array[i] = center + s;

                        else if (args[2].matches("(?i)DEFAULT") &&
                                s.startsWith(center))
                            array[i] = s.substring(center.length());
                    }

                    for (String s : array) {
                        if (tags != null) s = tags.format(player, s);
                        if (emojis != null) s = emojis.format(player, s);

                        channel.send(player, s);
                    }
                    continue;
                }

                else if (channel == channelManager.identify("title")) {
                    String time = null;
                    try {
                        time = Integer.parseInt(args[2]) + "";
                    } catch (Exception ignored) {}

                    time = time != null ? (":" + time) : "";

                    if (tags != null) message = tags.format(player, message);
                    if (emojis != null) message = emojis.format(player, message);

                    channel.send(player,
                            channelManager.getStartDelimiter() +
                                    channel.getName() + time +
                                    channelManager.getEndDelimiter() +
                                    " " + message
                    );
                    continue;
                }

                if (tags != null) message = tags.format(player, message);
                if (emojis != null) message = emojis.format(player, message);

                channel.send(player, message);
            }
        }
    }
}
