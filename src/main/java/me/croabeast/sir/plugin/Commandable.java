package me.croabeast.sir.plugin;

import me.croabeast.sir.plugin.command.SIRCommand;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface Commandable {

    @NotNull
    Set<SIRCommand> getCommands();
}
