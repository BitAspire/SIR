package com.bitaspire.sir.channel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface Style {

    @Nullable
    String getTag();

    @Nullable
    String getPrefix();

    @Nullable
    String getSuffix();

    @Nullable
    String getColor();

    boolean allowsNormalColors();

    boolean allowsSpecialColors();

    boolean allowsRgbColors();

    @Nullable
    Click getClick();

    @Nullable
    List<String> getHover();

    @NotNull
    String getFormat();

    void setFormat(@NotNull String format);

    default boolean isChatEventless() {
        List<String> hover = getHover();
        Click click = getClick();

        return (click == null || click.isEmpty()) && (hover == null || hover.isEmpty());
    }
}
