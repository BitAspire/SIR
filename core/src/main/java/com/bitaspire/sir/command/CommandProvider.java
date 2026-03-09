package com.bitaspire.sir.command;

import com.bitaspire.sir.SIRExtension;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface CommandProvider extends SIRExtension {

    @NotNull
    Set<SIRCommand> getCommands();
}
