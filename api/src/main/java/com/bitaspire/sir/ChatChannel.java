package com.bitaspire.sir;

import com.bitaspire.sir.user.SIRUser;
import me.croabeast.common.CollectionBuilder;
import me.croabeast.takion.chat.ChatComponent;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Legacy compatibility bridge for the pre-0.3.2 chat channel API.
 *
 * <p>The runtime now exposes channel contracts from {@code com.bitaspire.sir.channel}.
 * This interface is kept so older addons can still compile while the new API is available.</p>
 */
@ApiStatus.ScheduledForRemoval(inVersion = "0.3.0")
@Deprecated
public interface ChatChannel extends com.bitaspire.sir.channel.ChatChannel {

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

        return CollectionBuilder.of(list)
                .map(Bukkit::getWorld)
                .filter(Objects::nonNull)
                .toList();
    }

    @Nullable
    Click getClickAction();

    @Nullable
    List<String> getHoverList();

    default boolean isChatEventless() {
        List<String> hover = getHoverList();
        Click click = getClickAction();

        return (click == null || click.isEmpty()) && (hover == null || hover.isEmpty());
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
        return StringUtils.isNotBlank(prefix) && message.startsWith(prefix);
    }

    default boolean isAccessibleByCommand(String command) {
        Access access = getLocalAccess();
        if (access == null) return false;

        return access.getCommands().contains(command);
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

    @Override
    @NotNull
    default com.bitaspire.sir.channel.Access getAccess() {
        Access access = getLocalAccess();
        if (access == null) {
            return new com.bitaspire.sir.channel.Access() {
                @Override
                public boolean isDefault() {
                    return false;
                }

                @Override
                public @NotNull List<String> getPrefixes() {
                    return Collections.emptyList();
                }

                @Override
                public @NotNull List<String> getCommands() {
                    return Collections.emptyList();
                }

                @Override
                public boolean shouldStripPrefix() {
                    return true;
                }
            };
        }

        return new com.bitaspire.sir.channel.Access() {
            @Override
            public boolean isDefault() {
                return false;
            }

            @Override
            public @NotNull List<String> getPrefixes() {
                String prefix = access.getPrefix();
                return StringUtils.isBlank(prefix) ?
                        Collections.emptyList() :
                        Collections.singletonList(prefix);
            }

            @Override
            public @NotNull List<String> getCommands() {
                return access.getCommands();
            }

            @Override
            public boolean shouldStripPrefix() {
                return true;
            }
        };
    }

    @Override
    @NotNull
    default com.bitaspire.sir.channel.Audience getAudience() {
        return new com.bitaspire.sir.channel.Audience() {
            @Override
            public int getRadius() {
                return ChatChannel.this.getRadius();
            }

            @Override
            public boolean isSameWorld() {
                return false;
            }

            @Override
            public @Nullable String getPermission() {
                return null;
            }

            @Override
            public @Nullable String getGroup() {
                return null;
            }

            @Override
            public boolean shouldIncludeSender() {
                return true;
            }

            @Override
            public @Nullable List<String> getWorldsNames() {
                return ChatChannel.this.getWorldsNames();
            }
        };
    }

    @Override
    @NotNull
    default com.bitaspire.sir.channel.Style getStyle() {
        return new com.bitaspire.sir.channel.Style() {
            @Override
            public @Nullable String getTag() {
                return null;
            }

            @Override
            public @Nullable String getPrefix() {
                return ChatChannel.this.getPrefix();
            }

            @Override
            public @Nullable String getSuffix() {
                return ChatChannel.this.getSuffix();
            }

            @Override
            public @Nullable String getColor() {
                return ChatChannel.this.getColor();
            }

            @Override
            public boolean allowsNormalColors() {
                return false;
            }

            @Override
            public boolean allowsSpecialColors() {
                return false;
            }

            @Override
            public boolean allowsRgbColors() {
                return false;
            }

            @Override
            public @Nullable com.bitaspire.sir.channel.Click getClick() {
                Click click = getClickAction();
                if (click == null) return null;

                return new com.bitaspire.sir.channel.Click() {
                    @Override
                    public @NotNull ChatComponent.Click getAction() {
                        return click.getAction();
                    }

                    @Override
                    public @Nullable String getInput() {
                        return click.getInput();
                    }
                };
            }

            @Override
            public @Nullable List<String> getHover() {
                return getHoverList();
            }

            @Override
            public @NotNull String getFormat() {
                return getChatFormat();
            }

            @Override
            public void setFormat(@NotNull String format) {
                setChatFormat(format);
            }
        };
    }

    @Override
    @NotNull
    default com.bitaspire.sir.channel.Logging getLogging() {
        return new com.bitaspire.sir.channel.Logging() {
            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public @NotNull String getFormat() {
                return getLogFormat();
            }
        };
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
