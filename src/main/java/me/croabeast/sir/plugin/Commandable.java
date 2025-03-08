package me.croabeast.sir.plugin;

import me.croabeast.sir.plugin.command.SIRCommand;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface Commandable<C extends SIRCommand> {

    @NotNull
    Set<C> getCommands();
}
