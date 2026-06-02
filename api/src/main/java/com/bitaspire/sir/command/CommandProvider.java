package com.bitaspire.sir.command;

import me.croabeast.common.Registrable;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * A source of {@link SIRCommand} instances managed by the {@link CommandManager}.
 *
 * <p> Implementations declare a set of commands and whether they are currently enabled.
 * The manager uses this interface to register or unregister commands as a group.
 */
public interface CommandProvider extends Registrable {

    /**
     * Returns whether this provider (and its commands) is currently enabled.
     *
     * @return {@code true} if enabled.
     */
    boolean isEnabled();

    /**
     * Returns all commands declared by this provider.
     *
     * @return the command set.
     */
    @NotNull
    Set<SIRCommand> getCommands();
}
