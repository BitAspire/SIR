package me.croabeast.sir.plugin.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface ColorData {

    @NotNull
    Set<String> getFormats();

    void setColorStart(@NotNull String start);

    void setColorEnd(@Nullable String end);

    @NotNull
    String getStart();

    @Nullable
    String getEnd();

    void removeAnyFormats();
}
