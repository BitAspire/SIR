package com.bitaspire.sir.command.clear;

import lombok.Getter;
import lombok.SneakyThrows;
import com.bitaspire.sir.command.SIRCommand;
import com.bitaspire.sir.command.StandaloneProvider;

import java.util.HashSet;
import java.util.Set;

@Getter
public final class ClearProvider extends StandaloneProvider {

    private final Set<SIRCommand> commands = new HashSet<>();

    @SneakyThrows
    public boolean register() {
        commands.clear();
        commands.add(new Command(this));
        return true;
    }

    @Override
    public boolean unregister() {
        return true;
    }
}
