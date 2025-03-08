package me.croabeast.sir.plugin.command;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.croabeast.lib.CollectionBuilder;
import me.croabeast.lib.command.TabBuilder;
import me.croabeast.lib.file.ConfigurableFile;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.aspect.AspectButton;
import me.croabeast.sir.plugin.file.FileData;
import me.croabeast.sir.plugin.misc.SIRUser;
import me.croabeast.sir.plugin.LangUtils;
import me.croabeast.sir.plugin.Commandable;
import me.croabeast.takion.logger.TakionLogger;
import me.croabeast.takion.message.MessageSender;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;

final class AuditChatHandler implements Commandable<SIRCommand> {

    private final Map<CommandSender, CommandSender> replies = new HashMap<>();
    @Getter
    private final Set<SIRCommand> commands = new HashSet<>();

    AuditChatHandler() {
        commands.add(new SIRCommand("ignore") {

            private final String[] baseKeys = {"{target}", "{type}"};

            @NotNull
            protected ConfigurableFile getLang() {
                return FileData.Command.Multi.IGNORE.getFile(true);
            }

            @Override
            protected boolean execute(CommandSender sender, String[] args) {
                if (!(sender instanceof Player)) {
                    TakionLogger.getLogger().log(
                            "&cYou can not ignore players in the console.");
                    return true;
                }

                if (!isPermitted(sender)) return true;
                if (args.length == 0) return createSender(sender).send("help");

                if (args.length > 2)
                    return isWrongArgument(sender, args[args.length - 1]);

                final SIRUser user = plugin.getUserManager().getUser(sender);
                assert user != null;

                boolean chat = args.length == 2 && args[1].matches("(?i)-chat");
                String channel = getLang()
                        .get("lang.channels." + (chat ? "chat" : "msg"), "");

                if (args[0].matches("(?i)@a")) {
                    if (user.isIgnoringAll(chat)) {
                        user.unignoreAll(chat);
                    } else {
                        user.ignoreAll(chat);
                    }

                    return createSender(sender)
                            .addPlaceholders(baseKeys, null, channel)
                            .send((user.isIgnoringAll(chat) ?
                                    "success" :
                                    "remove") + ".all");
                }

                SIRUser target = plugin.getUserManager().fromClosest(args[0]);
                if (target == null)
                    return createSender(sender)
                            .addPlaceholder("{target}", args[0]).send("not-player");

                if (user.isIgnoring(target, chat)) {
                    user.unignore(target, chat);
                } else {
                    user.ignore(target, chat);
                }

                return createSender(sender)
                        .addPlaceholders(baseKeys, target.getName(), channel)
                        .send((user.isIgnoring(target, chat) ?
                                "success" :
                                "remove") + ".player");
            }

            @Override
            public TabBuilder getCompletionBuilder() {
                return createBasicTabBuilder()
                        .addArguments(0, getOnlineNames())
                        .addArgument(0, "@a")
                        .addArgument(1, "-chat");
            }

            @NotNull
            public AspectButton getButton() {
                return null;
            }
        });

        commands.add(new BaseCommand("msg") {
            @Override
            protected boolean execute(CommandSender s, String[] args) {
                if (!isPermitted(s)) return true;

                SIRUser user = plugin.getUserManager().getUser(s);
                if (user != null && user.isMuted())
                    return createSender(s).send("is-muted");

                if (args.length == 0)
                    return createSender(s).send("need-player");

                SIRUser target = plugin.getUserManager().fromClosest(args[0]);
                if (target == null)
                    return createSender(s)
                            .addPlaceholder("{target}", args[0])
                            .send("not-player");

                if (Objects.equals(target, user))
                    return createSender(s).send("not-yourself");

                if (target.isIgnoring(user, false)) {
                    ConfigurableFile lang =
                            FileData.Command.Multi.IGNORE.getFile(false);

                    return plugin.getLibrary().getLoadedSender()
                            .addPlaceholder(
                                    "{type}",
                                    lang.get("lang.channels.msg", "")
                            )
                            .send(lang.toStringList("lang.ignoring"));
                }

                boolean vanished = getLang()
                        .get("lang.vanish-messages.enabled", true);
                if (target.isVanished() && vanished)
                    return createSender(s).send("vanish-messages.message");

                String message = LangUtils.stringFromArray(args, 0);
                if (StringUtils.isBlank(message))
                    return createSender(s).send("empty-message");

                Values initValues = new Values(plugin, true);
                Values receiveValues = new Values(plugin, true);

                MessageSender sender = createSender(s)
                        .addPlaceholder("{message}", message)
                        .setLogger(false);

                initValues.playSound(s);
                receiveValues.playSound(target.getPlayer());

                sender.copy()
                        .addPlaceholder("{receiver}", isConsoleValue(target.getPlayer()))
                        .send(initValues.getOutput());

                sender.copy().setTargets(target.getPlayer())
                        .addPlaceholder("{sender}", isConsoleValue(s))
                        .send(receiveValues.getOutput());

                replies.put(target.getPlayer(), s);

                return createSender(null).setLogger(true)
                        .addPlaceholder("{receiver}", isConsoleValue(target.getPlayer()))
                        .addPlaceholder("{message}", message)
                        .addPlaceholder("{sender}", isConsoleValue(s))
                        .send("console-formatting.format");
            }

            @NotNull
            public Supplier<Collection<String>> generateCompletions(CommandSender sender, String[] arguments) {
                return () -> createBasicTabBuilder()
                        .addArguments(0,
                                CollectionBuilder.of(plugin.getUserManager().getOnlineUsers())
                                        .filter(u -> !u.isVanished())
                                        .map(SIRUser::getName).toList()
                        )
                        .addArgument(1, "<message>").build(sender, arguments);
            }
        });

        commands.add(new BaseCommand("reply") {
            @Override
            protected boolean execute(CommandSender sender, String[] args) {
                if (!isPermitted(sender)) return true;

                SIRUser receiver = plugin.getUserManager().getUser(sender);
                if (receiver != null && receiver.isMuted())
                    return createSender(sender).send("is-muted");

                if (args.length == 0)
                    return createSender(sender).send("empty-message");

                final CommandSender init = replies.get(sender);
                if (init == null)
                    return createSender(sender).send("not-replied");

                SIRUser initiator = plugin.getUserManager().getUser(init);
                if (initiator != null) {
                    if (initiator.isIgnoring(receiver, false)) {
                        ConfigurableFile lang =
                                FileData.Command.Multi.IGNORE.getFile(false);

                        return plugin.getLibrary().getLoadedSender()
                                .addPlaceholder(
                                        "{type}",
                                        lang.get("lang.channels.msg", "")
                                )
                                .send(lang.toStringList("lang.ignoring"));
                    }

                    boolean vanished = getLang()
                            .get("lang.vanish-messages.enabled", true);
                    if (initiator.isVanished() && vanished)
                        return createSender(sender)
                                .send("vanish-messages.message");
                }

                String message = LangUtils.stringFromArray(args, 1);
                if (StringUtils.isBlank(message))
                    return createSender(sender).send("empty-message");

                Values initValues = new Values(plugin, true);
                Values receiveValues = new Values(plugin, true);

                initValues.playSound(init);
                receiveValues.playSound(sender);

                final MessageSender msg = createSender(sender)
                        .addPlaceholder("{message}", message).setLogger(false);

                msg.copy()
                        .addPlaceholder("{receiver}", isConsoleValue(init))
                        .send(initValues.getOutput());

                msg.copy().setTargets(!(init instanceof Player) ?
                                null :
                                (Player) init)
                        .addPlaceholder("{sender}", isConsoleValue(sender))
                        .send(receiveValues.getOutput());

                return createSender(null)
                        .addPlaceholder("{receiver}", isConsoleValue(init))
                        .addPlaceholder("{message}", message)
                        .addPlaceholder("{sender}", isConsoleValue(sender))
                        .send("console-formatting.format");
            }

            @NotNull
            public Supplier<Collection<String>> generateCompletions(CommandSender sender, String[] arguments) {
                return () -> createBasicTabBuilder().addArgument(0, "<message>").build(sender, arguments);
            }
        });
    }

    @RequiredArgsConstructor
    static class Values {

        private final ConfigurableFile lang = FileData.Command.MSG_REPLY.getFile();

        private final SIRPlugin plugin;
        private final boolean sender;

        String getPath() {
            return "lang.for-" + (sender ? "sender" : "receiver") + '.';
        }

        void playSound(CommandSender sender) {
            Player player = sender instanceof Player ? (Player) sender : null;

            SIRUser user = plugin.getUserManager().getUser(player);
            if (user != null) user.playSound(lang.get(getPath() + "sound", ""));
        }

        List<String> getOutput() {
            return lang.toStringList(getPath() + "message");
        }
    }

    static abstract class BaseCommand extends SIRCommand {

        protected BaseCommand(String name) {
            super(name);
        }

        String isConsoleValue(CommandSender sender) {
            return !(sender instanceof Player) ?
                    getLang().get("lang.console-formatting.name", "") :
                    sender.getName();
        }

        @NotNull
        protected ConfigurableFile getLang() {
            return FileData.Command.MSG_REPLY.getFile();
        }

        @NotNull
        public abstract Supplier<Collection<String>> generateCompletions(CommandSender sender, String[] arguments);

        public final TabBuilder getCompletionBuilder() {
            return null;
        }

        @NotNull
        public AspectButton getButton() {
            return null;
        }
    }
}
