package me.croabeast.sir.manager;

import me.croabeast.command.Synchronizer;
import me.croabeast.sir.command.SIRCommand;
import org.jetbrains.annotations.NotNull;

public interface CommandManager extends BaseManager<SIRCommand> {

    @NotNull
    Synchronizer getSynchronizer();

    default boolean isEnabled(String name) {
        SIRCommand c = fromName(name);
        return c != null && c.isEnabled();
    }
}
