package com.bitaspire.sir.channel;

import com.bitaspire.sir.PermissibleUnit;
import com.bitaspire.sir.user.SIRUser;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface ChatChannel extends PermissibleUnit {

    boolean isGlobal();

    default boolean isLocal() {
        return !isGlobal();
    }

    @Nullable
    ChatChannel getParent();

    @Nullable
    ChatChannel getSubChannel();

    @NotNull
    Access getAccess();

    @NotNull
    Audience getAudience();

    @NotNull
    Style getStyle();

    @NotNull
    Logging getLogging();

    default boolean isChatEventless() {
        return getStyle().isChatEventless();
    }

    default boolean isDefault() {
        return isChatEventless() && getAudience().getRadius() <= 0;
    }

    default boolean isLocalAccessible() {
        return getAccess().isConfigured();
    }

    default boolean isAccessibleByPrefix(String message) {
        return getAccess().getMatchingPrefix(message) != null;
    }

    default boolean isAccessibleByCommand(String command) {
        return getAccess().getCommands().stream()
                .anyMatch(s -> s.equalsIgnoreCase(command));
    }

    @NotNull
    Set<SIRUser> getRecipients(SIRUser user);

    @NotNull
    Set<SIRUser> getRecipients(Player player);

    @NotNull
    String[] getChatKeys();

    @NotNull
    String[] getChatValues(String message);

    @NotNull
    String formatString(Player target, Player parser, String string, boolean isChat);

    @NotNull
    default String formatString(Player target, Player parser, String string) {
        return formatString(target, parser, string, true);
    }

    @NotNull
    default String formatString(Player player, String string, boolean isChat) {
        return formatString(player, player, string, isChat);
    }

    @NotNull
    default String formatString(Player player, String string) {
        return formatString(player, player, string, true);
    }
}
