package me.croabeast.sir.plugin.command;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import me.croabeast.lib.CollectionBuilder;
import me.croabeast.lib.command.TabBuilder;
import me.croabeast.lib.file.ConfigurableFile;
import me.croabeast.sir.plugin.module.PlayerFormatter;
import me.croabeast.sir.plugin.FileData;
import me.croabeast.sir.plugin.hook.HookChecker;
import me.croabeast.sir.plugin.manager.ModuleManager;
import me.croabeast.sir.plugin.module.SIRModule;
import me.croabeast.sir.plugin.misc.SIRUser;
import me.croabeast.sir.plugin.manager.UserManager;
import me.croabeast.sir.plugin.LangUtils;
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
                createSender(sender).send("help.targets") :
                isWrongArgument(sender, args[args.length - 1]));

        editSubCommand("chat", (sender, args) -> {
            TargetCatcher catcher = new TargetCatcher(sender, args.length > 0 ? args[0] : null);

            if (args.length == 0)
                return createSender(sender).send("help.chat");
            if (args.length < 3)
                return createSender(sender).send("empty-message");

            catcher.sendConfirmation();

            boolean hasArg = args[1].matches("(?i)DEFAULT|CENTERED|MIXED");
            new Printer(catcher, args, hasArg ? 2 : 1).print("");

            return true;
        });

        editSubCommand("action-bar", (sender, args) -> {
            TargetCatcher catcher = new TargetCatcher(sender, args.length > 0 ? args[0] : null);

            if (args.length == 0)
                return createSender(sender).send("help.action-bar");
            if (args.length < 2)
                return createSender(sender).send("empty-message");

            catcher.sendConfirmation();
            new Printer(catcher, args, 1).print("ACTION-BAR");

            return true;
        });

        editSubCommand("title", (sender, args) -> {
            TargetCatcher catcher = new TargetCatcher(sender, args.length > 0 ? args[0] : null);

            if (args.length == 0)
                return createSender(sender).send("help.title");
            if (args.length < 2)
                return createSender(sender).send("empty-message");

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
    protected boolean execute(CommandSender sender, String[] args) {
        return createSender(sender).send("help.main");
    }

    @NotNull
    public TabBuilder getCompletionBuilder() {
        TabBuilder builder = createBasicTabBuilder()
                .addArgument(0, "sir.print.targets", "targets")
                .addArgument(0, "sir.print.chat", "chat")
                .addArgument(0, "sir.print.action-bar", "action-bar")
                .addArgument(0, "sir.print.title", "title")
                .addArguments(1, "@a", "perm:", "world:")
                .addArguments(1,
                        CollectionBuilder.of(plugin.getUserManager().getOnlineUsers())
                                .filter(SIRUser::isVanished)
                                .map(SIRUser::getName).toList());

        if (HookChecker.VAULT_ENABLED)
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

        private final CommandSender sender;
        private final String input;

        private Set<Player> targets = null;

        TargetCatcher(CommandSender sender, String input) {
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
                                .filter(p -> UserManager.hasPerm(p, array[1]))
                                .toSet();
                        break;

                    case "GROUP":
                        targets = stream
                                .filter(p -> {
                                    String group = plugin.getVaultHolder().getPrimaryGroup(p);
                                    return group != null && group.matches("(?i)" + array[1]);
                                })
                                .toSet();
                        break;

                    default:
                }}

            return this.targets =
                    CollectionBuilder.of(targets).filter(Objects::nonNull).toSet();
        }

        private boolean sendConfirmation() {
            if (targets.isEmpty()) {
                createSender(sender).addPlaceholder("{target}", input).send("reminder.empty");
                return false;
            }

            if (targets.size() == 1) {
                String target = Lists.newArrayList(targets).get(0).getName();

                return (!(sender instanceof Player) || !targets.contains(sender)) &&
                        createSender(sender)
                                .addPlaceholder("{target}", target)
                                .send("reminder.success");
            }

            return createSender(sender).addPlaceholder("{target}", input).send("reminder.success");
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private class Printer {

        private final TargetCatcher catcher;
        private final String[] args;
        private final int index;

        private final TakionLib lib = plugin.getLibrary();

        private void print(String key) {
            String message = LangUtils.stringFromArray(args, index);
            String center = lib.getCenterPrefix();

            ChannelManager channelMr = lib.getChannelManager();
            ModuleManager moduleMr = plugin.getModuleManager();

            PlayerFormatter<?> emojis = moduleMr.getFormatter(SIRModule.Key.EMOJIS);
            PlayerFormatter<?> tags = moduleMr.getFormatter(SIRModule.Key.TAGS);

            for (Player player : catcher.targets) {
                final Channel c = channelMr.identify(key);
                if (c == channelMr.identify("chat")) {
                    String[] a = lib.splitString(message);

                    for (int i = 0; i < a.length; i++) {
                        final String s = a[i];

                        if (args[2].matches("(?i)CENTERED") &&
                                !s.startsWith(center)) a[i] = center + s;

                        else if (args[2].matches("(?i)DEFAULT") &&
                                s.startsWith(center))
                            a[i] = s.substring(center.length());
                    }

                    for (String s : a) {
                        if (tags != null) s = tags.format(player, s);
                        if (emojis != null) s = emojis.format(player, s);

                        c.send(player, s);
                    }
                    continue;
                }

                else if (c == channelMr.identify("title")) {
                    String time = null;
                    try {
                        time = Integer.parseInt(args[2]) + "";
                    } catch (Exception ignored) {}

                    time = time != null ? (":" + time) : "";

                    if (tags != null) message = tags.format(player, message);
                    if (emojis != null) message = emojis.format(player, message);

                    c.send(player,
                            channelMr.getStartDelimiter() +
                                    c.getName() + time +
                                    channelMr.getEndDelimiter() +
                                    " " + message
                    );
                    continue;
                }

                if (tags != null) message = tags.format(player, message);
                if (emojis != null) message = emojis.format(player, message);

                c.send(player, message);
            }
        }
    }
}
