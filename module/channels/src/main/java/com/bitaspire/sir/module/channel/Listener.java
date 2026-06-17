package com.bitaspire.sir.module.channel;

import lombok.RequiredArgsConstructor;
import me.croabeast.common.util.ReplaceUtils;
import com.bitaspire.sir.SIRApi;
import com.bitaspire.sir.channel.Access;
import com.bitaspire.sir.channel.ChatChannel;
import com.bitaspire.sir.channel.Click;
import com.bitaspire.sir.module.DiscordService;
import com.bitaspire.sir.module.ModuleManager;
import com.bitaspire.sir.user.SIRUser;
import me.croabeast.prismatic.PrismaticAPI;
import me.croabeast.takion.TakionLib;
import me.croabeast.takion.channel.Channel;
import me.croabeast.prismatic.chat.MultiComponent;
import me.croabeast.takion.message.MessageSender;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@RequiredArgsConstructor
final class Listener extends com.bitaspire.sir.Listener {

    private static final Pattern RGB_HEX_CODE = Pattern.compile("(?i)([&\\u00A7]#|\\{#)[0-9a-f]{6}}?");

    private final Channels main;

    private boolean handleEmptyMessage(MessageSender sender, String message) {
        if (main.config.allowsEmpty() || !isVisibleBlank(message))
            return false;

        sender.copy().send(main.config.getAllowEmptyMessages());
        return true;
    }

    private boolean isVisibleBlank(String message) {
        if (StringUtils.isBlank(message)) return true;

        String stripped = RGB_HEX_CODE.matcher(message).replaceAll("");
        stripped = PrismaticAPI.stripAll(stripped);
        stripped = ChatColor.stripColor(PrismaticAPI.colorize(stripped));

        return StringUtils.isBlank(stripped);
    }

    private String stripPrefix(ChatChannel channel, String message) {
        Access access = channel.getAccess();

        String prefix = access.getMatchingPrefix(message);
        if (StringUtils.isBlank(prefix) || !access.shouldStripPrefix())
            return message;

        return StringUtils.stripStart(message.substring(prefix.length()), null);
    }

    private void dispatch(SIRUser user, ChatChannel channel, String message, boolean async) {
        ChatEvent chat = new ChatEvent(user, channel, message, async);
        chat.setGlobal(channel.isGlobal());
        chat.call();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled() || !main.isEnabled()) return;

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
        if (handleEmptyMessage(sender, message)) {
            event.setCancelled(true);
            return;
        }

        boolean async = event.isAsynchronous();

        ChatChannel routed = main.data.getAccessible(user, message, true);
        if (routed != null) {
            event.setCancelled(true);

            String routedMessage = stripPrefix(routed, message);
            if (StringUtils.isBlank(routedMessage)) {
                handleEmptyMessage(sender, routedMessage);
                return;
            }

            dispatch(user, routed, routedMessage, async);
            return;
        }

        ChatChannel channel = main.data.getFallback(user);
        if (channel == null) return;

        String output = channel.formatString(user.getPlayer(), message, true);
        if (main.config.useBukkitFormat()) {
            event.setFormat(MultiComponent.DEFAULT_FORMAT.removeFormat(output).replace("%", "%%"));
            return;
        }

        event.setCancelled(true);
        dispatch(user, channel, message, async);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void onCommand(PlayerCommandPreprocessEvent event) {
        if (!main.isEnabled()) return;

        SIRUser user = main.getApi().getUserManager().getUser(event.getPlayer());
        String[] args = event.getMessage().split(" ");

        ChatChannel channel = main.data.getAccessible(user, args[0], false);
        if (channel == null) return;

        event.setCancelled(true);

        MessageSender sender = main.getApi().getLibrary()
                .getLoadedSender()
                .setTargets(event.getPlayer());

        String message = SIRApi.joinArray(1, args);
        if (StringUtils.isBlank(message)) {
            handleEmptyMessage(sender, message);
            return;
        }

        dispatch(user, channel, message, event.isAsynchronous());
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
        if (channel.relayToDiscord() && manager.isEnabled("Discord")) {
            String m = lib.getPlaceholderManager().replace(player, message);
            String name = event.isGlobal() ? "global-chat" : channel.getName();

            DiscordService discord = manager.getDiscordService();
            if (discord != null)
                discord.sendMessage(
                        discord.isRestricted() ? "restricted" : name, player,
                        s -> ReplaceUtils.replaceEach(keys, channel.getChatValues(event.getUser(), m), s)
                );
        }

        lib.getLogger().log(channel.formatString(player, message, false));
        String[] values = channel.getChatValues(event.getUser(), message);

        List<String> hover = channel.getStyle().getHover();
        if (hover != null && !hover.isEmpty()) {
            hover = new ArrayList<>(hover);
            hover.replaceAll(s -> ReplaceUtils.replaceEach(keys, values, s));
        }

        Click click = channel.getStyle().getClick();
        String input = click != null ? click.getInput() : null;

        if (StringUtils.isNotBlank(input))
            input = ReplaceUtils.replaceEach(keys, values, input);

        Channel chat = lib.getChannelManager().identify("chat");

        Set<SIRUser> users = event.getRecipients();
        boolean includeSender = channel.getAudience().shouldIncludeSender();

        if (includeSender) users.add(event.getUser());
        else users.remove(event.getUser());

        for (SIRUser user : users) {
            Player p = user.getPlayer();

            String temp = channel.formatString(p, player, message, true);
            temp = chat.formatString(p, player, temp);

            MultiComponent component = MultiComponent.fromString(lib.getChatProcessor(), temp);
            if (hover != null && !hover.isEmpty()) component.setHoverToAll(hover);
            if (click != null)
                component.setClickToAll(click.getAction(), input);

            p.spigot().sendMessage(component.compile(player));
        }
    }
}
