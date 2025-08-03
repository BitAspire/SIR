package me.croabeast.sir.misc;

import me.croabeast.common.CollectionBuilder;
import me.croabeast.sir.PermissibleUnit;
import me.croabeast.sir.user.SIRUser;
import me.croabeast.takion.chat.ChatComponent;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
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

    @Nullable
    String getPrefix();

    @Nullable
    String getSuffix();

    @Nullable
    String getColor();

    int getRadius();

    @Nullable
    List<String> getWorldsNames();

    @NotNull
    default List<World> getWorlds() {
        List<String> list = getWorldsNames();

        if (list == null || list.isEmpty())
            return Bukkit.getWorlds();

        return CollectionBuilder.of(getWorldsNames())
                .map(Bukkit::getWorld)
                .filter(Objects::nonNull).toList();
    }

    @Nullable
    Click getClickAction();

    @Nullable
    List<String> getHoverList();

    default boolean isChatEventless() {
        List<String> h = getHoverList();
        Click c = getClickAction();

        return (c == null || c.isEmpty()) && (h == null || h.isEmpty());
    }

    default boolean isDefault() {
        return isChatEventless() && getRadius() <= 0;
    }

    @Nullable
    Access getLocalAccess();

    default boolean isLocalAccessible() {
        return getLocalAccess() != null;
    }

    default boolean isAccessibleByPrefix(String message) {
        Access access = getLocalAccess();
        if (access == null) return false;

        String prefix = access.getPrefix();
        return StringUtils.isNotBlank(prefix) &&
                message.startsWith(prefix);
    }

    default boolean isAccessibleByCommand(String command) {
        Access access = getLocalAccess();
        if (access == null) return false;

        List<String> list = access.getCommands();
        return list.contains(command);
    }

    @NotNull
    Set<SIRUser> getRecipients(SIRUser user);

    @NotNull
    Set<SIRUser> getRecipients(Player player);

    @NotNull
    String getChatFormat();

    void setChatFormat(@NotNull String format);

    @NotNull
    String getLogFormat();

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

    interface Click {

        @NotNull
        ChatComponent.Click getAction();

        @Nullable
        String getInput();

        default boolean isEmpty() {
            return StringUtils.isBlank(getInput());
        }
    }

    interface Access {

        @Nullable
        String getPrefix();

        @NotNull
        List<String> getCommands();

        default boolean isEmpty() {
            return StringUtils.isBlank(getPrefix()) && getCommands().isEmpty();
        }
    }
}
