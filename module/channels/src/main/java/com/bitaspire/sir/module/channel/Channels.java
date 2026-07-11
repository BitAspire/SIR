package com.bitaspire.sir.module.channel;

import lombok.Getter;
import com.bitaspire.sir.file.ExtensionFile;
import com.bitaspire.sir.command.CommandProvider;
import com.bitaspire.sir.command.SIRCommand;
import com.bitaspire.sir.module.SIRModule;
import me.croabeast.takion.logger.LogLevel;

import java.util.HashSet;
import java.util.Set;

public final class Channels extends SIRModule implements CommandProvider {

    @Getter
    private final Set<SIRCommand> commands = new HashSet<>();

    Config config;
    Data data;
    Listener listener;
    DiscordSrvRelayGuard discordRelayGuard;

    @Override
    public boolean register() {
        config = new Config(this);
        data = new Data(this);

        try {
            commands.clear();
            commands.add(new Command(this, new ExtensionFile(this, "lang", true)));
        } catch (Exception e) {
            getLogger().log(LogLevel.ERROR, "Command 'chat-view' cannot be loaded due to lang.yml was missing");
            e.printStackTrace();
        }

        (listener = new Listener(this)).register();
        registerDiscordRelayGuard();
        refreshChatListenerAfterStartup();
        return true;
    }

    @Override
    public boolean unregister() {
        if (discordRelayGuard != null) discordRelayGuard.unregister();
        listener.unregister();
        discordRelayGuard = null;
        return true;
    }

    private void registerDiscordRelayGuard() {
        try {
            if (!getApi().getPlugin().getServer().getPluginManager().isPluginEnabled("DiscordSRV"))
                return;

            discordRelayGuard = new DiscordSrvRelayGuard(this);
            discordRelayGuard.register();
        } catch (Throwable ignored) {}
    }

    private void refreshChatListenerAfterStartup() {
        getApi().getScheduler().runTaskLater(() -> {
            if (listener == null) return;

            // Plugins enabled after SIR may register an equal-priority chat formatter.
            // Rebinding after startup gives Channels the same deterministic order as /sir reload.
            listener.unregister();
            listener.register();
        }, 1L);
    }
}
