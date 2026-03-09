package com.bitaspire.sir.command.mute;

import lombok.Getter;
import lombok.SneakyThrows;
import me.croabeast.file.ConfigurableFile;
import com.bitaspire.sir.ExtensionFile;
import com.bitaspire.sir.command.SIRCommand;
import com.bitaspire.sir.command.StandaloneProvider;

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
