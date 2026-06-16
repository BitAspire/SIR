package com.bitaspire.sir.chat;

import com.bitaspire.sir.user.SIRUser;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface ChatProcessor {

    default int getPriority() {
        return 0;
    }

    void process(@NotNull ChatProcessor.Context context);

    @Setter
    @Getter
    final class Context {

        private final SIRUser user;
        private final boolean asynchronous;

        private String message;
        private boolean cancelled;

        public Context(@NotNull SIRUser user, @NotNull String message, boolean asynchronous) {
            this.user = user;
            this.message = message;
            this.asynchronous = asynchronous;
        }

        @NotNull
        public Player getPlayer() {
            return user.getPlayer();
        }

        public void cancel() {
            setCancelled(true);
        }
    }
}
