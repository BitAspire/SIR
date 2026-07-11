package com.bitaspire.sir.module.channel;

import com.bitaspire.sir.channel.ChatChannel;
import com.bitaspire.sir.module.ModuleManager;
import com.bitaspire.sir.user.SIRUser;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.ListenerPriority;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.GameChatMessagePreProcessEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
final class DiscordSrvRelayGuard {

    private final Channels main;
    private boolean registered;

    void register() {
        if (registered) return;

        DiscordSRV.api.subscribe(this);
        registered = true;
    }

    void unregister() {
        if (!registered) return;

        DiscordSRV.api.unsubscribe(this);
        registered = false;
    }

    @Subscribe(priority = ListenerPriority.HIGHEST)
    void onGameChat(GameChatMessagePreProcessEvent event) {
        if (!main.isEnabled()) return;

        ChatChannel channel = resolve(event.getPlayer(), event.getMessage());
        if (channel == null) return;

        if (!channel.relayToDiscord() || sirDiscordWillRelay())
            event.setCancelled(true);
    }

    private ChatChannel resolve(Player player, String message) {
        if (player == null) return null;

        SIRUser user = main.getApi().getUserManager().getUser(player);
        if (user == null) return null;

        ChatChannel channel = main.data.getAccessible(user, message, true);
        return channel != null ? channel : main.data.getFallback(user);
    }

    private boolean sirDiscordWillRelay() {
        ModuleManager manager = main.getApi().getModuleManager();
        return manager.isEnabled("Discord") && manager.getDiscordService() != null;
    }
}
