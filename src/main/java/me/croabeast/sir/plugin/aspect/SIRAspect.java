package me.croabeast.sir.plugin.aspect;

import me.croabeast.lib.Registrable;
import me.croabeast.lib.file.ConfigurableFile;
import org.jetbrains.annotations.NotNull;

public interface SIRAspect extends Registrable {

    @NotNull
    AspectKey getAspectKey();

    @NotNull
    default String getName() {
        return getAspectKey().getName();
    }

    @NotNull
    AspectButton getButton();

    @NotNull
    ConfigurableFile getMainFile();
}
