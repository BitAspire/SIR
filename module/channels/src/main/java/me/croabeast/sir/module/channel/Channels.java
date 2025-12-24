package me.croabeast.sir.module.channel;

import lombok.Getter;
import me.croabeast.sir.ExtensionFile;
import me.croabeast.sir.command.CommandProvider;
import me.croabeast.sir.command.SIRCommand;
import me.croabeast.sir.module.SIRModule;
import me.croabeast.takion.logger.LogLevel;

import java.util.HashSet;
import java.util.Set;

public final class Channels extends SIRModule implements CommandProvider {

    @Getter
    private final Set<SIRCommand> commands = new HashSet<>();

    Config config;
    Data data;
    Listener listener;

    @Override
    public boolean register() {
        config = new Config(this);
        data = new Data(this);

        try {
            commands.add(new Command(this, new ExtensionFile(this, "lang", true)));
        } catch (Exception e) {
            getLogger().log(LogLevel.ERROR, "Command 'chat-view' cannot be loaded due to lang.yml was missing");
            e.printStackTrace();
        }

        (listener = new Listener(this)).register();
        return true;
    }

    @Override
    public boolean unregister() {
        listener.unregister();
        return true;
    }
}
