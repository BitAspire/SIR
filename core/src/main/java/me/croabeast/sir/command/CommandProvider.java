package me.croabeast.sir.command;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface CommandProvider {

    @NotNull
    Set<SIRCommand> getCommands();
}
