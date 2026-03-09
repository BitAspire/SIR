package com.bitaspire.sir.command.ignore;

import lombok.Getter;
import lombok.SneakyThrows;
import com.bitaspire.sir.command.SIRCommand;
import com.bitaspire.sir.command.StandaloneProvider;

import java.util.HashSet;
import java.util.Set;

@Getter
public final class IgnoreProvider extends StandaloneProvider {

    private final Set<SIRCommand> commands = new HashSet<>();

    @SneakyThrows
    public boolean register() {
        commands.add(new Command(this));
        return true;
    }

    @Override
    public boolean unregister() {
        return true;
    }
}
