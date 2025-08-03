package me.croabeast.sir.manager;

import me.croabeast.sir.command.SIRCommand;

public interface CommandManager extends BaseManager<SIRCommand> {

    default boolean isEnabled(String name) {
        SIRCommand c = fromName(name);
        return c != null && c.isEnabled();
    }
}
