package com.bitaspire.sir.channel;

import me.croabeast.takion.chat.ChatComponent;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Click {

    @NotNull
    ChatComponent.Click getAction();

    @Nullable
    String getInput();

    default boolean isEmpty() {
        return StringUtils.isBlank(getInput());
    }
}
