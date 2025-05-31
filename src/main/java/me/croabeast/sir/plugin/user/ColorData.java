package me.croabeast.sir.plugin.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ColorData {

    @NotNull
    String getColorStart();

    void setColorStart(@NotNull String colorStart);

    @Nullable
    String getColorEnd();

    void setColorEnd(@Nullable String colorEnd);
}
