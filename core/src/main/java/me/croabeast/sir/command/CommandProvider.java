package me.croabeast.sir.command;

import me.croabeast.sir.SIRExtension;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface CommandProvider extends SIRExtension {

    @NotNull
    Set<SIRCommand> getCommands();
}
