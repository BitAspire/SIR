package com.bitaspire.sir.module;

import com.bitaspire.sir.channel.ChatChannel;
import com.bitaspire.sir.user.SIRUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Marks a module that emits side effects once per chat message, such as alert
 * messages or sounds sent to a mentioned player.
 *
 * <p> A {@link com.bitaspire.sir.UserFormatter UserFormatter} may be invoked once per recipient while a message is
 * being delivered, so it must stay free of side effects. Notifications belong here
 * instead: the dispatcher calls {@link #notify} a single time per message.
 */
public interface ChatNotifier {

    /**
     * Emits the notifications this module owns for a single chat message.
     *
     * @param sender the user that sent the message.
     * @param message the message, already stripped of any channel prefix.
     * @param channel the channel the message was routed through, or {@code null}.
     */
    void notify(@NotNull SIRUser sender, @NotNull String message, @Nullable ChatChannel channel);
}
