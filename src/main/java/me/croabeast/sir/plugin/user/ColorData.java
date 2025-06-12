package me.croabeast.sir.plugin.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public interface ColorData {

    @NotNull
    String getColorStart();

    void setColorStart(@NotNull String start);

    @Nullable
    String getColorEnd();

    void setColorEnd(@Nullable String end);

    void removeAnyFormats();

    @NotNull
    Set<String> getColorFormats();
}
