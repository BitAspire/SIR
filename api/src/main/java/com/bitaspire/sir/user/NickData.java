package com.bitaspire.sir.user;

import org.jetbrains.annotations.Nullable;

public interface NickData {

    @Nullable
    String getNick();

    default boolean hasNick() {
        String nick = getNick();
        return nick != null && !nick.trim().isEmpty();
    }

    void setNick(@Nullable String nick);

    void resetNick();
}
