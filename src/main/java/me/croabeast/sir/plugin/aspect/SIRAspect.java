package me.croabeast.sir.plugin.aspect;

import me.croabeast.lib.file.ConfigurableFile;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Internal
public interface SIRAspect {

    @NotNull
    AspectKey getKey();

    @NotNull
    default String getName() {
        return getKey().getName();
    }

    @NotNull
    AspectButton getButton();

    default boolean isEnabled() {
        return getButton().isEnabled();
    }

    @NotNull
    ConfigurableFile getFile();
}
