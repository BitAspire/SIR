package me.croabeast.sir.plugin.module;

import lombok.Getter;
import lombok.Setter;
import me.croabeast.lib.CollectionBuilder;
import me.croabeast.lib.command.TabBuilder;
import me.croabeast.lib.file.ConfigurableFile;
import me.croabeast.lib.file.ConfigurableUnit;
import me.croabeast.lib.file.UnitMappable;
import me.croabeast.lib.util.ReplaceUtils;
import me.croabeast.lib.util.TextUtils;
import me.croabeast.sir.plugin.Commandable;
import me.croabeast.sir.plugin.command.SIRCommand;
import me.croabeast.sir.plugin.misc.ChatChannel;
import me.croabeast.sir.plugin.FileData;
import me.croabeast.sir.plugin.misc.FileKey;
import me.croabeast.sir.plugin.misc.SIRUser;
import me.croabeast.sir.plugin.LangUtils;
import me.croabeast.takion.TakionLib;
import me.croabeast.takion.channel.Channel;
import me.croabeast.takion.message.chat.ChatClick;
import me.croabeast.takion.message.chat.ChatComponent;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

final class ChatHandler extends ListenerModule implements Commandable {

    private UnitMappable<ChatChannel> locals = UnitMappable.empty();
    private UnitMappable<ChatChannel> globals = UnitMappable.empty();

    private final ConfigurableFile file;
    @Getter
    final Set<SIRCommand> commands = new HashSet<>();

    ChatHandler() {
        super(Key.CHANNELS);
        file = FileData.Module.Chat.CHANNELS.getFile();

        commands.add(new SIRCommand(this, SIRCommand.Key.CHAT_VIEW) {

            private final FileKey<Boolean> chatFile = FileData.Command.Multi.CHAT_VIEW;

            List<String> keys(SIRUser user) {
                Set<ChatChannel> set = locals.values(HashSet::new);
                return CollectionBuilder.of(set)
                        .filter(c -> !c.hasPerm(user))
                        .map(ConfigurableUnit::getName)
                        .collect(ArrayList::new);
            }

            @Override
            protected boolean execute(CommandSender sender, String[] args) {
                if (!(sender instanceof Player)) {
                    plugin.getLibrary().getLogger().log("&cYou can't toggle a local chat in console.");
                    return true;
                }

                if (!isPermitted(sender)) return true;
                if (args.length == 0) return createSender(sender).send("help");

                if (args.length != 1)
                    return isWrongArgument(sender, args[args.length - 1]);

                final SIRUser user = plugin.getUserManager().getUser(sender);

                String key = null;
                for (String k : keys(user))
                    if (k.matches("(?i)" + args[0])) {
                        key = k;
                        break;
                    }

                if (key == null) return isWrongArgument(sender, args[0]);

                plugin.getUserManager().toggleLocalChannelView(user, key);

                return createSender(sender).addPlaceholder("{channel}", key)
                        .send((user.isLocalChannelToggled(key)) + "");
            }

            @Override
            public TabBuilder getCompletionBuilder() {
                return createBasicTabBuilder().addArguments(0, (s, a) -> keys(plugin.getUserManager().getUser(s)));
            }

            @NotNull
            protected ConfigurableFile getLang() {
                return chatFile.getFile(true);
            }
        });
    }

    @Override
    public boolean register() {
        ChannelUtils.loadDefaults();
        UnitMappable<ChatChannel> channels = file.asUnitMap("channels", ChannelUtils::of);

        locals = channels.copy().filter(ChatChannel::isLocal);
        globals = channels.copy().filter(ChatChannel::isGlobal);

        return super.register();
    }

    ChatChannel localFromPrefix(SIRUser user, String message) {
        for (Set<ChatChannel> set : locals.values())
            for (ChatChannel channel : set)
                if (user.hasPerm(channel.getPermission()) &&
                        channel.isAccessibleByPrefix(message))
                    return channel;

        return null;
    }

    ChatChannel localFromCommand(SIRUser user, String command) {
        for (Set<ChatChannel> set : locals.values())
            for (ChatChannel channel : set)
                if (user.hasPerm(channel.getPermission()) &&
                        channel.isAccessibleByCommand(command))
                    return channel;

        return null;
    }

    ChatChannel getGlobal(SIRUser user) {
        for (Set<ChatChannel> set : globals.values()) {
            for (ChatChannel channel : set) {
                if (user.hasPerm(channel.getPermission()))
                    return channel;
            }
        }

        return ChannelUtils.getDefaults();
    }

    class EmptyChecker {

        private final List<String> messages;
        private final boolean enabled;

        EmptyChecker() {
            messages = file.toStringList("allow-empty.message");
            enabled = file.get("allow-empty.enabled", false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void onChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled() || !isEnabled()) return;

        SIRUser user = plugin.getUserManager().getUser(event.getPlayer());
        if (user.isMuted() || !user.isLogged()) {
            event.setCancelled(true);
            return;
        }

        final String message = event.getMessage();

        EmptyChecker checker = new EmptyChecker();
        if (!checker.enabled && StringUtils.isBlank(message)) {
            plugin.getLibrary().getLoadedSender()
                    .setTargets(user.getPlayer()).send(checker.messages);

            event.setCancelled(true);
            return;
        }

        boolean async = event.isAsynchronous();

        ChatChannel local = localFromPrefix(user, message);
        if (local != null)
            if (((Predicate<ChatChannel>) channel -> {
                ChatChannel.Access access = local.getLocalAccess();
                if (access == null) return false;

                final String prefix = access.getPrefix();
                if (StringUtils.isBlank(prefix)) return false;

                String msg = message.substring(prefix.length());
                event.setCancelled(true);

                new SIRChatEvent(user, local, msg, async).call();
                return true;
            }
            ).test(local)) return;

        ChatChannel channel = getGlobal(user);
        if (channel == null) return;

        SIRChatEvent global = new SIRChatEvent(user, channel, message, async);
        global.setGlobal(true);

        String output = channel.formatString(user.getPlayer(), message, true);

        if (file.get("default-format", false)) {
            event.setFormat(TextUtils.STRIP_JSON.apply(output).replace("%", "%%"));
            return;
        }

        event.setCancelled(true);
        global.call();
    }

    @EventHandler(priority = EventPriority.LOW)
    private void onCommand(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled() || !isEnabled()) return;

        SIRUser user = plugin.getUserManager().getUser(event.getPlayer());
        String[] args = event.getMessage().split(" ");

        ChatChannel local = localFromCommand(user, args[0]);
        if (local == null) return;

        event.setCancelled(true);

        String message = LangUtils.stringFromArray(args, 1);
        if (StringUtils.isNotBlank(message))
            new SIRChatEvent(user, local, message, event.isAsynchronous()).call();
    }

    @EventHandler
    private void onSIRChat(SIRChatEvent event) {
        if (!isEnabled()) return;

        TakionLib lib = plugin.getLibrary();

        ChatChannel channel = event.getChannel();
        Player player = event.getPlayer();

        final String[] keys = channel.getChatKeys();
        String message = event.getMessage();

        if (Key.DISCORD.isEnabled()) {
            String m = lib.getPlaceholderManager().replace(player, message);
            String name = event.isGlobal() ? "global-chat" : channel.getName();

            Actionable act = plugin.getModuleManager().getActionable(Key.DISCORD);
            if (act != null)
                act.act(name, player, keys, channel.getChatValues(m));
        }

        lib.getLogger().log(channel.formatString(player, message, false));
        String[] values = channel.getChatValues(message);

        List<String> hover = channel.getHoverList();
        if (hover != null)
            hover.replaceAll(s -> ReplaceUtils.replaceEach(keys, values, s));

        ChatChannel.Click click = channel.getClickAction();
        String input = click != null ? click.getInput() : null;

        if (StringUtils.isNotBlank(input))
            input = ReplaceUtils.replaceEach(keys, values, input);

        Channel chat = lib.getChannelManager().identify("chat");

        Set<SIRUser> users = event.getRecipients();
        users.add(event.getUser());

        for (final SIRUser user : users) {
            Player p = user.getPlayer();
            String temp = channel.formatString(p, player, message, true);

            ChatComponent component = ChatComponent.create(lib, temp);

            if (click != null)
                component.setClickToAll(new ChatClick(lib, click.getAction(), input));
            if (hover != null) component.setHoverToAll(hover);

            chat.send(p, player, component.toPatternString());
        }
    }

    @Getter @Setter
    final static class SIRChatEvent extends Event {

        @Getter
        private static final HandlerList handlerList = new HandlerList();

        private final SIRUser user;
        private final Player player;

        private ChatChannel channel;
        private String message;

        private boolean global = false;

        public SIRChatEvent(SIRUser user, ChatChannel channel, String message, boolean async) {
            super(async);

            this.user = user;
            this.player = user.getPlayer();

            this.channel = channel;
            this.message = message;
        }

        public Set<SIRUser> getRecipients() {
            return channel.getRecipients(user);
        }

        public void call() {
            Bukkit.getPluginManager().callEvent(this);
        }

        @NotNull
        public HandlerList getHandlers() {
            return handlerList;
        }
    }
}
