package me.croabeast.sir.plugin.module;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.croabeast.lib.file.Configurable;
import me.croabeast.sir.api.file.ConfigUnit;
import me.croabeast.sir.plugin.FileData;
import me.croabeast.sir.plugin.hook.HookChecker;
import org.apache.commons.lang.StringUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TagsFormatter extends SIRModule implements PlayerFormatter<Object> {

    private final Map<String, ChatTag> tags = new HashMap<>();
    private Object expansion;

    TagsFormatter() {
        super(Key.TAGS);

        if (!HookChecker.PAPI_ENABLED) return;

        expansion = new PlaceholderExpansion() {
            @NotNull
            public String getIdentifier() {
                return "sir_tag";
            }

            @NotNull
            public String getAuthor() {
                return "CroaBeast";
            }

            @NotNull
            public String getVersion() {
                return "1.0";
            }

            @Nullable
            public String onRequest(OfflinePlayer off, @NotNull String params) {
                Player player = off.getPlayer();
                if (player == null) return null;

                if (params.matches("(?i)group:(.+)")) {
                    List<ChatTag> tags = fromGroup(player, params.split(":")[1]);
                    if (tags.isEmpty()) return null;

                    final String name = tags.get(0).getTag();
                    return StringUtils.isNotBlank(name) ? name : null;
                }

                if (params.matches("(?i)perm:(.+)")) {
                    ChatTag tag = fromPerm(player, params.split(":")[1]);
                    if (tag == null) return null;

                    final String name = tag.getTag();
                    return StringUtils.isNotBlank(name) ? name : null;
                }

                ChatTag tag = tags.get(params);
                if (tag == null) return null;

                final String name = tag.getTag();
                return StringUtils.isNotBlank(name) ? name : null;
            }
        };
    }

    @NotNull
    List<ChatTag> fromGroup(Player player, String group) {
        final List<ChatTag> tags = new ArrayList<>();
        if (player == null) return tags;

        for (ChatTag tag : this.tags.values()) {
            if (!Objects.equals(tag.getGroup(), group))
                continue;

            if (tag.isInGroupNonNull(player)) tags.add(tag);
        }

        return tags;
    }

    @Nullable
    ChatTag fromPerm(Player player, String perm) {
        if (player == null) return null;

        Map<Integer, Set<ChatTag>> map = new TreeMap<>(Collections.reverseOrder());

        for (ChatTag tag : tags.values()) {
            if (!Objects.equals(tag.getPermission(), perm)) continue;

            Set<ChatTag> tags = map.getOrDefault(tag.getPriority(), new HashSet<>());
            tags.add(tag);

            map.put(tag.getPriority(), tags);
        }
        if (map.isEmpty()) return null;

        Set<ChatTag> tags = map.values().iterator().next();
        return !tags.isEmpty() ? tags.iterator().next() : null;
    }

    @Override
    public boolean register() {
        if (!isEnabled()) return false;

        tags.clear();
        FileData.Module.Chat.TAGS.getFile()
                .getSections("tags")
                .forEach((key, s) -> tags.put(key, new ChatTag(s)));

        if (!HookChecker.PAPI_ENABLED) return true;

        PlaceholderExpansion ex = (PlaceholderExpansion) expansion;
        return ex.isRegistered() || ex.register();
    }

    @Override
    public boolean unregister() {
        if (!HookChecker.PAPI_ENABLED) return false;

        PlaceholderExpansion ex = (PlaceholderExpansion) expansion;
        return !ex.isRegistered() || ex.unregister();
    }

    @Override
    public String format(Player player, String string, Object reference) {
        return format(player, string);
    }

    @Override
    public String format(Player player, String string) {
        if (player == null || StringUtils.isBlank(string) || !isEnabled())
            return string;

        Pattern pattern = Pattern.compile("(?i)\\{tag_(.+)}");
        Matcher matcher = pattern.matcher(string);

        while (matcher.find()) {
            final String id = matcher.group(1);

            if (id.matches("(?i)group:(.+)")) {
                List<ChatTag> tags = fromGroup(player, id.split(":")[1]);
                if (tags.isEmpty()) continue;

                final String name = tags.get(0).getTag();
                if (StringUtils.isBlank(name)) continue;

                string = string.replace(matcher.group(), name);
                continue;
            }

            if (id.matches("(?i)perm:(.+)")) {
                ChatTag tag = fromPerm(player, id.split(":")[1]);
                if (tag == null) continue;

                final String name = tag.getTag();
                if (StringUtils.isBlank(name)) continue;

                string = string.replace(matcher.group(), name);
                continue;
            }

            ChatTag tag = tags.get(id);
            if (tag == null) continue;

            final String name = tag.getTag();
            if (StringUtils.isBlank(name)) continue;

            string = string.replace(matcher.group(), name);
        }

        return string;
    }

    private static class ChatTag implements ConfigUnit {

        private final ConfigurationSection section;

        private ChatTag(ConfigurationSection section) {
            this.section = section;
        }

        @Override
        public @NotNull ConfigurationSection getSection() {
            return section;
        }

        @Nullable
        public String getTag() {
            return section.getString("tag");
        }

        public List<String> getDescription() {
            return Configurable.toStringList(section, "description");
        }
    }
}
