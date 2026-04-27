package com.bitaspire.sir.command.nick;

import com.bitaspire.sir.ExtensionFile;
import com.bitaspire.sir.command.SIRCommand;
import com.bitaspire.sir.command.StandaloneProvider;
import lombok.Getter;
import lombok.SneakyThrows;

import java.util.HashSet;
import java.util.Set;

@Getter
public final class NickProvider extends StandaloneProvider {

    private final Set<SIRCommand> commands = new HashSet<>();

    ExtensionFile lang;

    @SneakyThrows
    public boolean register() {
        lang = new ExtensionFile(this, "lang", true);
        commands.add(new Command(this));
        return true;
    }

    @Override
    public boolean unregister() {
        return true;
    }
}
