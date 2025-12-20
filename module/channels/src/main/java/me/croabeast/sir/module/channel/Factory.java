package me.croabeast.sir.module.channel;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import me.croabeast.common.CollectionBuilder;
import me.croabeast.common.applier.StringApplier;
import me.croabeast.common.util.ReplaceUtils;
import me.croabeast.file.Configurable;
import me.croabeast.prismatic.PrismaticAPI;
import me.croabeast.sir.ChatChannel;
import me.croabeast.sir.SIRApi;
import me.croabeast.sir.module.emoji.Emojis;
import me.croabeast.sir.module.mention.Mentions;
import me.croabeast.sir.module.tag.Tags;
import me.croabeast.sir.user.ColorData;
import me.croabeast.sir.user.SIRUser;
import me.croabeast.takion.chat.ChatComponent;
import me.croabeast.takion.chat.MultiComponent;
import me.croabeast.takion.format.Format;
import me.croabeast.takion.format.StringFormat;
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
final class Factory {

    private final String DEFAULT_FORMAT = " &7{player}: {message}";
    private final String[] CHAT_KEYS = {"{prefix}", "{sir-prefix}", "{suffix}", "{sir-suffix}", "{color}", "{message}"};

    @Getter
    abstract class BaseChannel implements ChatChannel {

        private final SIRApi api = SIRApi.instance();
        private final Channels main;

        private final ConfigurationSection section;
        private final boolean global;

        private final ChatChannel parent;

        private final String permission;
        private final int priority;

        @Nullable
        private final String prefix, suffix;
        private final String color;

        @Getter(AccessLevel.NONE)
        private final ColorChecker checker;

        private final int radius;
        private final List<String> worldsNames;

        private final Access localAccess;
        private final Click clickAction;

        @Nullable
        private final List<String> hoverList;

        @Setter
        private String chatFormat;
        private final String logFormat;

        BaseChannel(Channels main, ConfigurationSection section, ChatChannel parent) {
            this.main = main;
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
            chatFormat = fromParent("format", ChatChannel::getChatFormat, DEFAULT_FORMAT).trim();

            logFormat = (main.config.useSimpleLogger() ?
                    main.config.getSimpleLoggerFormat() : chatFormat).trim();
        }

        private boolean useParents(String path) {
            return !section.isSet(path) && parent != null;
        }

        private boolean fromBoolean(String path) {
            return (useParents(path) ? parent.getSection() : section).getBoolean(path);
        }

        @SuppressWarnings("unchecked")
        private <T> T fromParent(String path, Function<ChatChannel, T> f, T def) {
            return useParents(path) ? f.apply(parent) : (T) section.get(path, def);
        }

        private List<String> fromList(String p, Function<ChatChannel, List<String>> list) {
            return useParents(p) ? list.apply(parent) : Configurable.toStringList(section, p, null);
        }

        @NotNull
        public Set<SIRUser> getRecipients(SIRUser user) {
            Set<SIRUser> previous = api.getUserManager().getUsers();
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

                users.filter(u -> u.hasPermission(getPermission()));
                users.filter(u -> u.getChannelData().isToggled(getName()));
            }

            users.filter(u -> !u.getIgnoreData().isIgnoring(user, true));
            return users.toSet();
        }

        @NotNull
        public Set<SIRUser> getRecipients(Player player) {
            return getRecipients(api.getUserManager().getUser(player));
        }

        public String[] getChatKeys() {
            return CHAT_KEYS;
        }

        public String[] getChatValues(String message) {
            return new String[] {prefix, prefix, suffix, suffix, color, message};
        }

        @NotNull
        public String formatString(Player target, Player parser, String string, boolean chat) {
            String format = chat ? chatFormat : logFormat;

            StringApplier applier = StringApplier.simplified(format)
                    .apply(s -> api.getLibrary().getPlaceholderManager().replace(parser, s))
                    .apply(s -> {
                        String[] values = getChatValues(checker.check(parser, string));
                        return ReplaceUtils.replaceEach(CHAT_KEYS, values, s);
                    });

            SIRUser user = api.getUserManager().getUser(parser);

            if (api.getModuleManager().isEnabled("Emojis")) {
                Emojis emojis = api.getModuleManager().getModule(Emojis.class);
                if (emojis != null) applier.apply(s -> emojis.parseEmojis(user, s));
            }

            if (api.getModuleManager().isEnabled("Tags")) {
                Tags tags = api.getModuleManager().getModule(Tags.class);
                if (tags != null) applier.apply(s -> tags.parseTags(user, s));
            }

            if (api.getModuleManager().isEnabled("Mentions")) {
                Mentions mentions = api.getModuleManager().getModule(Mentions.class);
                if (mentions != null) applier.apply(s -> mentions.parseMentions(user, s, this));
            }

            if (user != null)
                applier.apply(s -> {
                    String[] keys = {"{color-start}", "{color-end}"};

                    ColorData data = user.getColorData();
                    String[] values = {data.getStart(), data.getEnd()};

                    return ReplaceUtils.replaceEach(keys, values, s);
                });

            if (chat)
                applier.apply(s -> {
                    StringFormat f = api.getLibrary().getFormatManager().get("SMALL_CAPS");
                    return f.accept(s);
                });

            Format<ChatComponent<?>> f = MultiComponent.DEFAULT_FORMAT;
            if (isDefault() && !f.isFormatted(applier.toString()))
                return applier
                        .apply(s -> api.getLibrary().colorize(target, parser, s))
                        .apply(api.getLibrary().getCharacterManager()::align)
                        .toString();

            return (isChatEventless() ? applier : applier.apply(f::removeFormat)).toString();
        }

        @Override
        public String toString() {
            return "BaseChannel{path=" + section.getCurrentPath() +
                    ", permission='" + permission + '\'' +
                    ", priority=" + priority + ", global=" + global +
                    ", format='" + chatFormat + '\'' + '}';
        }
    }

    @Getter
    class ClickImpl implements ChatChannel.Click {

        private final ChatComponent.Click action;
        private final String input;

        private ClickImpl(ConfigurationSection section) {
            action = ChatComponent.Click.fromName(section.getString("action"));
            input = section.getString("input");
        }

        private ClickImpl(String string) {
            String[] array = string
                    .replace("\"", "").split(":", 2);

            action = ChatComponent.Click.fromName(array[0]);
            input = array[1];
        }

        @Override
        public String toString() {
            return "Click{action=" + action + ", input='" + input + '\'' + '}';
        }
    }

    @Getter
    class AccessImpl implements ChatChannel.Access {

        private final String prefix;
        private final List<String> commands;

        private AccessImpl(ConfigurationSection section) {
            this.prefix = section.getString("prefix");
            this.commands = Configurable.toStringList(section, "commands");
        }

        @Override
        public String toString() {
            return "Access{prefix='" + prefix + '\'' + ", commands=" + commands + '}';
        }
    }

    @RequiredArgsConstructor
    class ColorChecker {

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

            return !SIRApi.instance().getUserManager().hasPermission(player, PERM + perm) && !b;
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

        @Override
        public String toString() {
            return "ColorChecker{normal=" + normal + ", special=" + special + ", rgb=" + rgb + '}';
        }
    }
}
