package me.croabeast.sir.plugin.module;

import lombok.*;
import lombok.experimental.UtilityClass;
import me.croabeast.lib.CollectionBuilder;
import me.croabeast.lib.applier.StringApplier;
import me.croabeast.lib.file.Configurable;
import me.croabeast.lib.util.ReplaceUtils;
import me.croabeast.lib.util.TextUtils;
import me.croabeast.prismatic.PrismaticAPI;
import me.croabeast.sir.plugin.misc.ChatChannel;
import me.croabeast.sir.plugin.aspect.AspectKey;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.file.FileData;
import me.croabeast.sir.plugin.misc.SIRUser;
import me.croabeast.sir.plugin.manager.SIRUserManager;
import me.croabeast.takion.message.chat.ChatClick;
import me.croabeast.takion.misc.StringAligner;
import org.apache.commons.lang.StringUtils;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

@UtilityClass
class ChannelUtils {

    private final String DEF_FORMAT = " &7{player}: {message}";
    ChatChannel defaults = null;

    final String[] CHAT_KEYS = {
            "{prefix}", "{sir-prefix}", "{suffix}", "{sir-suffix}",
            "{color}", "{message}"
    };

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    @Getter
    static abstract class BaseChannel implements ChatChannel {

        private final SIRPlugin plugin = SIRPlugin.getInstance();

        @NotNull
        private final ConfigurationSection section;
        private final boolean global;

        @Nullable
        private final ChatChannel parent;

        @NotNull
        private final String permission;
        private final int priority;

        @Nullable
        private final String prefix, suffix;
        private final String color;

        @Getter(AccessLevel.NONE)
        @NotNull
        private final ColorChecker checker;

        private final int radius;
        private final List<String> worldsNames;

        private final Access localAccess;
        private final Click clickAction;

        @Nullable
        private final List<String> hoverList;

        @Setter @NotNull
        private String chatFormat;
        @NotNull
        private final String logFormat;

        BaseChannel(ConfigurationSection section, @Nullable ChatChannel parent) {
            this.section = section;
            this.parent = parent;

            global = section.getBoolean("global", true);

            permission = fromParent("permission", ChatChannel::getPermission, "DEFAULT");
            priority = fromParent(
                    "priority", ChatChannel::getPriority,
                    permission.matches("(?i)DEFAULT") ? 0 : 1
            );

            prefix = fromParent("prefix", ChatChannel::getPrefix, null);
            suffix = fromParent("suffix", ChatChannel::getSuffix, null);

            color = fromParent("color", ChatChannel::getColor, null);

            checker = new ColorChecker(
                    fromBoolean("color-options.normal"), fromBoolean("color-options.special"),
                    fromBoolean("color-options.rgb")
            );

            radius = fromParent("radius", ChatChannel::getRadius, global ? -1 : 100);
            worldsNames = fromList("worlds", ChatChannel::getWorldsNames);

            ConfigurationSection s = section.getConfigurationSection("access");
            Access access = s != null ? new AccessImpl(s) : null;

            localAccess = useParents("access") ? parent.getLocalAccess() : access;

            Object previous = section.get("click-action");
            previous = previous != null ? previous : section.get("click");

            Click click = null;
            if (previous instanceof ConfigurationSection) {
                click = new ClickImpl((ConfigurationSection) previous);
            }
            if (previous instanceof String) {
                click = new ClickImpl((String) previous);
            }

            clickAction = ((!section.isSet("click-action") || !section.isSet("click"))
                    && parent != null) ?
                    parent.getClickAction() : click;

            hoverList = fromList("hover", ChatChannel::getHoverList);
            chatFormat = fromParent("format", ChatChannel::getChatFormat, DEF_FORMAT);

            Configurable config = FileData.Module.Chat.getMain();
            String logPath = "simple-logger.";

            this.logFormat = TextUtils.STRIP_FIRST_SPACES.apply(
                    !config.get(logPath + "enabled", false) ?
                            chatFormat :
                            config.get(logPath + "format", DEF_FORMAT)
            );
        }

        private boolean useParents(String path) {
            return !section.isSet(path) && parent != null;
        }

        private boolean fromBoolean(String path) {
            return (useParents(path) ? parent.getSection() : section).getBoolean(path);
        }

        private <T> T fromParent(String path, Function<ChatChannel, T> f, T def) {
            return useParents(path) ? f.apply(parent) : (T) section.get(path, def);
        }

        private List<String> fromList(String p, Function<ChatChannel, List<String>> list) {
            return useParents(p) ? list.apply(parent) : Configurable.toStringList(section, p, null);
        }

        @NotNull
        public Set<SIRUser> getRecipients(SIRUser user) {
            Set<SIRUser> previous = plugin.getUserManager().getOnlineUsers();
            if (user == null) return previous;

            CollectionBuilder<SIRUser> users = CollectionBuilder
                    .of(previous)
                    .filter(u -> {
                        World world = u.getPlayer().getWorld();
                        return getWorlds().contains(world);
                    });

            final int radius = getRadius();
            if (radius > 0) {
                Set<SIRUser> e = user.getNearbyUsers(radius);
                if (!e.isEmpty()) users.filter(e::contains);
            }

            if (isLocal()) {
                if (StringUtils.isNotBlank(getGroup()))
                    users.filter(u -> isInGroup(u.getPlayer()));

                users.filter(u -> u.hasPerm(getPermission()));
                users.filter(u -> u.isLocalChannelToggled(getName()));
            }

            users.filter(u -> !u.isIgnoring(user, true));
            return users.toSet();
        }

        @Override
        public @NotNull Set<SIRUser> getRecipients(Player player) {
            return getRecipients(plugin.getUserManager().getUser(player));
        }

        @NotNull
        public String[] getChatKeys() {
            return CHAT_KEYS;
        }

        @NotNull
        public String[] getChatValues(String message) {
            return new String[] {getPrefix(), getPrefix(), getSuffix(), getSuffix(), getColor(), message};
        }

        @NotNull
        <T> PlayerFormatter<T> fromKey(AspectKey key) {
            PlayerFormatter<T> formatter = plugin.getModuleManager().getFormatter(key);
            return formatter != null ? formatter : (player, string, o) -> string;
        }

        @NotNull
        public String formatString(Player target, Player parser, String string, boolean chat) {
            final String format = chat ? chatFormat : logFormat;

            final StringApplier applier = StringApplier.simplified(format)
                    .apply(s -> SIRPlugin.getLib().getPlaceholderManager().replace(parser, s))
                    .apply(s -> {
                        String[] values = getChatValues(checker.check(parser, string));
                        return ReplaceUtils.replaceEach(CHAT_KEYS, values, s);
                    })
                    .apply(s -> fromKey(SIRModule.Key.EMOJIS).format(parser, s))
                    .apply(s -> fromKey(SIRModule.Key.TAGS).format(parser, s))
                    .apply(s -> fromKey(SIRModule.Key.MENTIONS).format(parser, s, this));

            if (chat) applier.apply(SIRPlugin.getLib().getSmallCapsAction()::act);

            if (isDefault() && !TextUtils.IS_JSON.test(applier.toString()))
                return applier
                        .apply(s -> SIRPlugin.getLib().colorize(target, parser, s))
                        .apply(StringAligner::align).toString();

            return isChatEventless() ? applier.toString() :
                    applier.apply(TextUtils.STRIP_JSON).toString();
        }
    }

    static Configurable config() {
        return FileData.Module.Chat.CHANNELS.getFile();
    }

    static ChatChannel loadDefaults() {
        ConfigurationSection def = config().getSection("default-channel");

        return def == null ? null : (defaults = new BaseChannel(def, null) {

            public boolean isGlobal() {
                return true;
            }

            @Nullable
            public ChatChannel getSubChannel() {
                return null;
            }
        });
    }

    static ChatChannel getDefaults() {
        if (!config().get("default-channel.enabled", true))
            return null;

        try {
            return defaults == null ? loadDefaults() : defaults;
        } catch (Exception e) {
            return null;
        }
    }

    static ChatChannel of(ConfigurationSection section) {
        return new ChannelImpl(section);
    }

    @Getter
    static class ChannelImpl extends BaseChannel {

        private final ChatChannel subChannel;

        ChannelImpl(ConfigurationSection section) {
            super(section, getDefaults());

            ConfigurationSection l = getSection().getConfigurationSection("local");
            subChannel = (l == null || isLocal()) ? null : new BaseChannel(l, this) {

                public boolean isGlobal() {
                    return false;
                }

                @Nullable
                public ChatChannel getSubChannel() {
                    return null;
                }
            };
        }
    }

    @Getter
    static class ClickImpl implements ChatChannel.Click {

        private final ChatClick.Action action;
        private final String input;

        private ClickImpl(ConfigurationSection section) {
            action = ChatClick.Action.from(section.getString("action"));
            input = section.getString("input");
        }

        private ClickImpl(String string) {
            String[] array = string
                    .replace("\"", "").split(":", 2);

            action = ChatClick.Action.from(array[0]);
            input = array[1];
        }
    }

    @Getter
    static class AccessImpl implements ChatChannel.Access {

        private final String prefix;
        private final List<String> commands;

        private AccessImpl(ConfigurationSection section) {
            this.prefix = section.getString("prefix");
            this.commands = Configurable.toStringList(section, "commands");
        }
    }

    @RequiredArgsConstructor
    static class ColorChecker {

        static final String PERM = "sir.color.chat.";
        final boolean normal, special, rgb;

        boolean notColor(Player player, String perm) {
            boolean b;
            switch (perm) {
                case "special":
                    b = special;
                    break;
                case "rgb":
                    b = rgb;
                    break;
                case "normal": default:
                    b = normal;
                    break;
            }
            return !SIRUserManager.hasPerm(player, PERM + perm) && !b;
        }

        String check(Player player, String string) {
            StringApplier applier = StringApplier.simplified(string);

            if (notColor(player, "normal"))
                applier.apply(PrismaticAPI::stripBukkit);
            if (notColor(player, "rgb"))
                applier.apply(PrismaticAPI::stripRGB);
            if (notColor(player, "special"))
                applier.apply(PrismaticAPI::stripSpecial);

            return applier.toString();
        }
    }
}
