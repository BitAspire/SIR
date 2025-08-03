package me.croabeast.sir.command;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.croabeast.common.CollectionBuilder;
import me.croabeast.command.TabBuilder;
import me.croabeast.common.CustomListener;
import me.croabeast.common.Registrable;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.sir.SIRPlugin;
import me.croabeast.sir.aspect.AspectButton;
import me.croabeast.sir.FileData;
import me.croabeast.sir.aspect.AspectKey;
import me.croabeast.sir.aspect.SIRAspect;
import me.croabeast.sir.user.IgnoreData;
import me.croabeast.sir.user.SIRUser;
import me.croabeast.sir.LangUtils;
import me.croabeast.sir.Commandable;
import me.croabeast.takion.message.MessageSender;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;

final class AuditChatHandler implements Commandable, Registrable {

    private final Map<CommandSender, CommandSender> replies = new HashMap<>();
    private final CustomListener listener;

    private final SIRAspect aspect = new MessageAspect();
    private final List<SIRCommand> messageCommands = new ArrayList<>();

    @Getter
    private final Set<SIRCommand> commands = new HashSet<>();

    AuditChatHandler() {
        ConfigurableFile lang = FileData.Command.Multi.IGNORE.getFile(true);
        listener = new CustomListener() {
            @Getter
            private final Status status = new Status();

            @EventHandler
            void onQuit(PlayerQuitEvent event) {
                Player player = event.getPlayer();
                replies.entrySet().removeIf(e ->
                        Objects.equals(e.getKey(), player) ||
                        Objects.equals(e.getValue(), player)
                );
            }
        };

        commands.add(new SIRCommand(SIRCommand.Key.IGNORE, true) {

            private final String[] baseKeys = {"{target}", "{type}"};

            @NotNull
            protected ConfigurableFile getLang() {
                return FileData.Command.Multi.IGNORE.getFile(true);
            }

            @Override
            protected boolean execute(CommandSender sender, String[] args) {
                if (!(sender instanceof Player)) {
                    plugin.getLibrary().getServerLogger().log(
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

                final IgnoreData data = user.getIgnoreData();
                if (args[0].matches("(?i)@a")) {
                    if (data.isIgnoringAll(chat)) {
                        data.unignoreAll(chat);
                    } else {
                        data.ignoreAll(chat);
                    }

                    return createSender(sender)
                            .addPlaceholders(baseKeys, null, channel)
                            .send((data.isIgnoringAll(chat) ?
                                    "success" :
                                    "remove") + ".all");
                }

                SIRUser target = plugin.getUserManager().fromClosest(args[0]);
                if (target == null) return checkPlayer(sender, args[0]);

                if (data.isIgnoring(target, chat)) {
                    data.unignore(target, chat);
                } else {
                    data.ignore(target, chat);
                }

                return createSender(sender)
                        .addPlaceholders(baseKeys, target.getName(), channel)
                        .send((data.isIgnoring(target, chat) ?
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
        });

        messageCommands.add(new BaseCommand("msg") {
            @Override
            protected boolean execute(CommandSender s, String[] args) {
                if (!isPermitted(s)) return true;

                final SIRUser user = plugin.getUserManager().getUser(s);
                if (user != null && user.getMuteData().isMuted())
                    return createSender(s).setLogger(false).send("is-muted");

                if (args.length == 0)
                    return createSender(s).setLogger(false).send("need-player");

                SIRUser target = plugin.getUserManager().fromClosest(args[0]);
                if (target == null) return checkPlayer(s, args[0]);

                if (Objects.equals(target, user))
                    return createSender(s).setLogger(false).send("not-yourself");

                if (target.getIgnoreData().isIgnoring(user, false))
                    return plugin.getLibrary().getLoadedSender()
                            .setLogger(false)
                            .addPlaceholder("{type}", lang.get("lang.channels.msg", ""))
                            .send(lang.toStringList("lang.ignoring"));

                boolean vanished = getLang().get("lang.vanish-messages.enabled", true);
                if (target.isVanished() && vanished)
                    return createSender(s).setLogger(false).send("vanish-messages.message");

                String message = LangUtils.stringFromArray(args, 1);
                if (StringUtils.isBlank(message))
                    return createSender(s).setLogger(false).send("empty-message");

                Values initValues = new Values(plugin, true);
                Values receiveValues = new Values(plugin, false);

                Player player = target.getPlayer();

                MessageSender sender = createSender(null)
                        .setLogger(false)
                        .addPlaceholder("{receiver}", isConsoleValue(player))
                        .addPlaceholder("{message}", message)
                        .addPlaceholder("{sender}", isConsoleValue(s));

                initValues.playSound(s);
                receiveValues.playSound(player);

                sender.copy().setTargets(s).send(initValues.getOutput());
                sender.copy().setTargets(player).send(receiveValues.getOutput());

                replies.put(player, s);
                replies.put(s, player);

                return sender.setErrorPrefix(null)
                        .setLogger(true).send("console-formatting.format");
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
        messageCommands.add(new BaseCommand("reply") {
            @Override
            protected boolean execute(CommandSender s, String[] args) {
                if (!isPermitted(s)) return true;

                final SIRUser receiver = plugin.getUserManager().getUser(s);
                if (receiver != null && receiver.getMuteData().isMuted())
                    return createSender(s).setLogger(false).send("is-muted");

                final CommandSender init = replies.get(s);
                if (init == null)
                    return createSender(s).setLogger(false).send("not-replied");

                SIRUser initiator = plugin.getUserManager().getUser(init);

                if (initiator.getIgnoreData().isIgnoring(receiver, false))
                    return plugin.getLibrary().getLoadedSender()
                            .setLogger(false)
                            .addPlaceholder("{type}", lang.get("lang.channels.msg", ""))
                            .send(lang.toStringList("lang.ignoring"));

                if (getLang().get("lang.vanish-messages.enabled", true) &&
                        initiator.isVanished())
                    return createSender(s).setLogger(false).send("vanish-messages.message");

                String message = LangUtils.stringFromArray(args, 0);
                if (StringUtils.isBlank(message))
                    return createSender(s).setLogger(false).send("empty-message");

                Values initValues = new Values(plugin, true);
                Values receiveValues = new Values(plugin, false);

                initValues.playSound(init);
                receiveValues.playSound(s);

                final MessageSender sender = createSender(null)
                        .setLogger(false)
                        .addPlaceholder("{receiver}", isConsoleValue(init))
                        .addPlaceholder("{message}", message)
                        .addPlaceholder("{sender}", isConsoleValue(s));

                sender.copy().setTargets(s).send(initValues.getOutput());
                sender.copy().setTargets(init).send(receiveValues.getOutput());

                return sender.setErrorPrefix(null)
                        .setLogger(true).send("console-formatting.format");
            }

            @NotNull
            public Supplier<Collection<String>> generateCompletions(CommandSender sender, String[] arguments) {
                return () -> createBasicTabBuilder().addArgument(0, "<message>").build(sender, arguments);
            }
        });

        commands.addAll(messageCommands);
    }

    @Override
    public boolean isRegistered() {
        return listener.isRegistered();
    }

    @Override
    public boolean register() {
        return listener.register(SIRPlugin.getInstance());
    }

    @Override
    public boolean unregister() {
        return listener.unregister();
    }

    @Getter
    private final class MessageAspect implements SIRAspect {

        private final ConfigurableFile file;

        private final AspectKey key;
        private final AspectButton button;

        MessageAspect() {
            file = FileData.Command.getMain();

            SIRCommand.Key key = SIRCommand.Key.MSG_REPLY;
            this.key = key;

            (button = new AspectButton(
                    key,
                    file.get("commands.msg.enabled", true)
            )).setDefaultItems("Messaging Commands");

            button.setOnClick(b -> e -> {
                file.set("commands.mute.enabled", b.isEnabled());
                file.save();

                if (b.isEnabled()) {
                    messageCommands.forEach(c -> c.register(false));
                } else {
                    messageCommands.forEach(c -> c.unregister(false));
                }

                SIRCommand.syncCommands();

                String s = "Messaging commands registered: " + b.isEnabled();
                SIRPlugin.getLib().getLogger().log(s);
            });

            key.setSupplier(button::isEnabled);
        }
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

    abstract class BaseCommand extends SIRCommand {

        protected BaseCommand(String name) {
            super(aspect, name);
            setButton(aspect.getButton());
        }

        protected final String isConsoleValue(CommandSender sender) {
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
    }
}
