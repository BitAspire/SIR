package me.croabeast.sir.command.mute;

import lombok.Getter;
import lombok.SneakyThrows;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.sir.ExtensionFile;
import me.croabeast.sir.command.SIRCommand;
import me.croabeast.sir.command.StandaloneProvider;

import java.util.HashSet;
import java.util.Set;

public final class MuteProvider extends StandaloneProvider {

    @Getter
    private final Set<SIRCommand> commands = new HashSet<>();

    ConfigurableFile lang;

    @SneakyThrows
    public boolean register() {
        lang = new ExtensionFile(this, "lang", true);

        commands.add(new MuteCommand(this));
        commands.add(new TempCommand(this));
        commands.add(new UnmuteCommand(this));
        return true;
    }

    @Override
    public boolean unregister() {
        return true;
    }
}
