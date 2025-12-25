package me.croabeast.sir.module.channel;

import lombok.RequiredArgsConstructor;
import me.croabeast.common.util.ReplaceUtils;
import me.croabeast.sir.ChatChannel;
import me.croabeast.sir.SIRApi;
import me.croabeast.sir.module.DiscordService;
import me.croabeast.sir.module.ModuleManager;
import me.croabeast.sir.user.SIRUser;
import me.croabeast.takion.TakionLib;
import me.croabeast.takion.channel.Channel;
import me.croabeast.takion.chat.MultiComponent;
import me.croabeast.takion.message.MessageSender;
import org.apache.commons.lang.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@RequiredArgsConstructor
final class Listener extends me.croabeast.sir.Listener {

    private final Channels main;

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled() || !main.isEnabled()) return;

        main.getApi().getLibrary().getLogger().log("1");
        SIRUser user = main.getApi().getUserManager().getUser(event.getPlayer());
        if (user == null) return;

        if (!user.isLogged()) {
            event.setCancelled(true);
            return;
        }

        MessageSender sender = main.getApi().getLibrary()
                .getLoadedSender()
                .setTargets(user.getPlayer());

        if (user.getMuteData().isMuted()) {
            sender.copy().send(main.config.getMutedMessages());
            event.setCancelled(true);
            return;
        }

        String message = event.getMessage();
        if (main.config.allowsEmpty() && StringUtils.isBlank(message)) {
            sender.copy().send(main.config.getAllowEmptyMessages());
            event.setCancelled(true);
            return;
        }

        boolean async = event.isAsynchronous();

        ChatChannel local = main.data.getLocal(user, message, true);
        if (local != null &&
                ((Predicate<ChatChannel>) channel -> {
                    ChatChannel.Access access = local.getLocalAccess();
                    if (access == null) return false;

                    String prefix = access.getPrefix();
                    if (StringUtils.isBlank(prefix)) return false;

                    String msg = message.substring(prefix.length());
                    event.setCancelled(true);

                    new ChatEvent(user, local, msg, async).call();
                    return true;
                }).test(local)) return;

        ChatChannel channel = main.data.getGlobal(user);
        if (channel == null) return;

        String output = channel.formatString(user.getPlayer(), message, true);
        if (main.config.useBukkitFormat()) {
            event.setFormat(MultiComponent.DEFAULT_FORMAT.removeFormat(output).replace("%", "%%"));
            return;
        }

        event.setCancelled(true);

        ChatEvent global = new ChatEvent(user, channel, message, async);
        global.setGlobal(true);
        global.call();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void onCommand(PlayerCommandPreprocessEvent event) {
        if (!main.isEnabled()) return;

        SIRUser user = main.getApi().getUserManager().getUser(event.getPlayer());
        String[] args = event.getMessage().split(" ");

        ChatChannel local = main.data.getLocal(user, args[0], false);
        if (local == null) return;

        event.setCancelled(true);

        String message = SIRApi.joinArray(1, args);
        if (StringUtils.isNotBlank(message))
            new ChatEvent(user, local, message, event.isAsynchronous()).call();
    }

    @EventHandler
    private void onSIRChat(ChatEvent event) {
        if (!main.isEnabled()) return;

        TakionLib lib = main.getApi().getLibrary();

        ChatChannel channel = event.getChannel();
        Player player = event.getPlayer();

        String[] keys = channel.getChatKeys();
        String message = event.getMessage();

        ModuleManager manager = main.getApi().getModuleManager();
        if (manager.isEnabled("Discord")) {
            String m = lib.getPlaceholderManager().replace(player, message);
            String name = event.isGlobal() ? "global-chat" : channel.getName();

            DiscordService discord = manager.getDiscordService();
            if (discord != null)
                discord.sendMessage(
                        discord.isRestricted() ? "restricted" : name, player,
                        s -> ReplaceUtils.replaceEach(keys, channel.getChatValues(m), s)
                );
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

        for (SIRUser user : users) {
            Player p = user.getPlayer();

            String temp = channel.formatString(p, player, message, true);
            temp = chat.formatString(p, player, temp);

            MultiComponent component = MultiComponent.fromString(lib, temp);
            if (hover != null) component.setHoverToAll(hover);
            if (click != null)
                component.setClickToAll(click.getAction(), input);

            p.spigot().sendMessage(component.compile(player));
        }
    }
}
