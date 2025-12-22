package me.croabeast.sir.command.print;

import lombok.Getter;
import me.croabeast.sir.command.SIRCommand;
import me.croabeast.sir.command.StandaloneProvider;

import java.util.HashSet;
import java.util.Set;

@Getter
public final class PrintProvider extends StandaloneProvider {

    private final Set<SIRCommand> commands = new HashSet<>();

    @Override
    public boolean register() {
        try {
            commands.add(new Command(this));
        } catch (Exception ignored) {}
        return true;
    }

    @Override
    public boolean unregister() {
        return true;
    }
}
