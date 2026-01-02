package me.croabeast.sir.command.print;

import lombok.Getter;
import lombok.SneakyThrows;
import me.croabeast.sir.ChatToggleable;
import me.croabeast.sir.command.SIRCommand;
import me.croabeast.sir.command.StandaloneProvider;

import java.util.HashSet;
import java.util.Set;

@Getter
public final class PrintProvider extends StandaloneProvider implements ChatToggleable {

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
