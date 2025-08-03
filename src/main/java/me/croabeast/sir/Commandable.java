package me.croabeast.sir;

import me.croabeast.sir.command.SIRCommand;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface Commandable {

    @NotNull
    Set<SIRCommand> getCommands();
}
