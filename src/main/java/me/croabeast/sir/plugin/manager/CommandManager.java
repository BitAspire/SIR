package me.croabeast.sir.plugin.manager;

import me.croabeast.sir.plugin.command.SIRCommand;

public interface CommandManager extends BaseManager<SIRCommand> {

    default boolean isEnabled(String name) {
        SIRCommand c = fromName(name);
        return c != null && c.isEnabled();
    }
}
