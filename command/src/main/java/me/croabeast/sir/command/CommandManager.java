package me.croabeast.sir.command;

import me.croabeast.common.Loadable;
import me.croabeast.common.gui.ChestBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface CommandManager extends Loadable {

    @NotNull
    Set<SIRCommand> getCommands();

    SIRCommand getCommand(String name);

    @NotNull
    ChestBuilder getMenu();

    default boolean isEnabled(String name) {
        SIRCommand command = getCommand(name);
        return command != null && command.isEnabled();
    }
}
