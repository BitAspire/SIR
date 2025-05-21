package me.croabeast.sir.plugin.module;

import lombok.Getter;
import me.croabeast.file.Configurable;
import me.croabeast.common.util.ReplaceUtils;
import me.croabeast.prismatic.PrismaticAPI;
import me.croabeast.sir.api.file.PermissibleUnit;
import me.croabeast.sir.plugin.misc.ChatChannel;
import me.croabeast.sir.plugin.FileData;
import me.croabeast.sir.plugin.user.SIRUser;
import me.croabeast.takion.chat.MultiComponent;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MentionFormatter extends SIRModule implements PlayerFormatter<ChatChannel> {

    private final Set<Mention> mentions = new HashSet<>();

    MentionFormatter() {
        super(Key.MENTIONS);
    }

    @Override
    public boolean register() {
        if (!isEnabled()) return false;

        mentions.clear();
        FileData.Module.Chat.MENTIONS.getFile()
                .getSections("mentions")
                .forEach((k, s) -> mentions.add(new Mention(s)));

        return true;
    }

    @Override
    public boolean unregister() {
        return false;
    }

    final String[] keys = {"{prefix}", "{sender}", "{receiver}"};

    @Override
    public String format(Player player, String string, ChatChannel channel) {
        if (player == null ||
                StringUtils.isBlank(string) || !isEnabled())
            return string;

        UnaryOperator<String> operator = null;
        String firstSound = null;
        List<String> firstMessages = null;

        for (Mention mention : mentions) {
            if (!mention.isInGroupAsNull(player)) continue;
            if (!mention.hasPerm(player)) continue;

            final String prefix = mention.prefix;
            if (StringUtils.isBlank(prefix)) continue;

            Pattern pattern = Pattern.compile(
                    Pattern.quote(prefix) + "(.[^ ]+)\\b");

            Matcher matcher = pattern.matcher(string);
            int start = 0, end;

            while (matcher.find()) {
                SIRUser user = plugin.getUserManager().fromClosest(matcher.group(1));
                if (user == null || player == user.getPlayer() ||
                        user.getIgnoreData().isIgnoring(player, true))
                    continue;

                if (channel != null &&
                        !channel.getRecipients(player).contains(user))
                    continue;

                end = matcher.start();

                String finder = string.substring(start, end);
                start = matcher.end();

                String color = PrismaticAPI.getEndColor(finder);
                String[] values =
                        {prefix, player.getName(), user.getName()};

                UnaryOperator<String> op =
                        s -> ReplaceUtils.replaceEach(keys, values, s);
                if (operator == null) operator = op;

                plugin.getLibrary().getLoadedSender()
                        .setTargets(user.getPlayer())
                        .setLogger(false)
                        .addFunctions(op)
                        .send(mention.messages.receiver);

                if (firstMessages == null)
                    firstMessages = mention.messages.sender;

                final Entry e = mention.sound;
                if (!e.receiver.isEmpty())
                    user.playSound(e.receiver.get(0));

                if (firstSound == null) firstSound = e.sender.get(0);

                List<String> hover = mention.hover;
                hover.replaceAll(op);

                String click = op.apply(mention.click);
                String[] c = click.split(":", 2);

                String result = op.apply(mention.value);

                result = MultiComponent.fromString(plugin.getLibrary(), result)
                        .setHover(hover)
                        .setClick(click)
                        .toFormattedString();

                String replace = plugin.getLibrary().colorize(player, result);
                if (color != null) replace += color;

                string = string.replace(matcher.group(), replace);
            }
        }

        if (firstMessages != null && !firstMessages.isEmpty())
            plugin.getLibrary().getLoadedSender()
                    .addFunctions(operator)
                    .setLogger(false).setTargets(player)
                    .send(firstMessages);

        plugin.getUserManager().getUser(player).playSound(firstSound);
        return string;
    }

    @Override
    public String format(Player player, String string) {
        throw new UnsupportedOperationException("A channel should be used");
    }

    static class Mention implements PermissibleUnit {

        @Getter
        ConfigurationSection section;
        String prefix, value;

        String click;
        List<String> hover;

        Entry sound = Entry.empty(), messages = Entry.empty();

        Mention(ConfigurationSection section) {
            this.section = section;

            prefix = section.getString("prefix", "");
            value = section.getString("value", "");

            click = section.getString("click", "");
            hover = Configurable.toStringList(section, "hover");

            try {
                sound = new Entry(section, "sound");
            } catch (Exception ignored) {}
            try {
                messages = new Entry(section, "messages");
            } catch (Exception ignored) {}
        }

        @Override
        public String toString() {
            return "Mention{" +
                    "prefix='" + prefix + '\'' +
                    ", value='" + value + '\'' +
                    ", click='" + click + '\'' +
                    ", hover=" + hover +
                    ", sound=" + sound +
                    ", messages=" + messages +
                    '}';
        }
    }

    static class Entry {

        private List<String> sender = new ArrayList<>(),
                receiver = new ArrayList<>();

        Entry() {}

        Entry(ConfigurationSection s, String path) {
            Objects.requireNonNull(s);

            s = s.getConfigurationSection(path);
            Objects.requireNonNull(s);

            sender = Configurable.toStringList(s, "sender");
            receiver = Configurable.toStringList(s, "receiver");
        }

        static Entry empty() {
            return new Entry();
        }

        @Override
        public String toString() {
            return "Entry{sender=" + sender + ", receiver=" + receiver + '}';
        }
    }
}
