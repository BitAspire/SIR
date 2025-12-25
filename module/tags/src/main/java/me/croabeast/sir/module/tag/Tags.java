package me.croabeast.sir.module.tag;

import me.croabeast.sir.PAPIExpansion;
import me.croabeast.sir.UserFormatter;
import me.croabeast.sir.module.SIRModule;
import me.croabeast.sir.user.SIRUser;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Tags extends SIRModule implements UserFormatter<Object> {

    Data data;
    PAPIExpansion hook;

    @Override
    public boolean register() {
        data = new Data(this);

        return !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") ||
                (hook = new PAPIExpansion("sir_tag") {
                    @Nullable
                    public String onRequest(OfflinePlayer off, @NotNull String params) {
                        Player player = off.getPlayer();
                        if (player == null) return null;

                        SIRUser user = getApi().getUserManager().getUser(player);
                        return user == null ? null : parseTag(user, params);
                    }
                }).register();
    }

    @Override
    public boolean unregister() {
        return hook == null || hook.unregister();
    }

    String parseTag(SIRUser user, String string) {
        if (string.matches("(?i)group:(.+)")) {
            List<Tag> tags = data.fromGroup(user, string.split(":")[1]);
            if (tags.isEmpty()) return null;

            String name = tags.get(0).getTag();
            return StringUtils.isNotBlank(name) ? name : null;
        }

        if (string.matches("(?i)default")) {
            Tag tag = data.getTag(user);
            if (tag == null) return null;

            String name = tag.getTag();
            return StringUtils.isNotBlank(name) ? name : null;
        }

        Tag tag = data.getTags().get(string);
        if (tag == null) return null;

        String name = tag.getTag();
        return StringUtils.isNotBlank(name) ? name : null;
    }

    @NotNull
    public String format(SIRUser user, String string) {
        if (user == null || StringUtils.isBlank(string) || !isEnabled())
            return string;

        Pattern pattern = Pattern.compile("(?i)\\{tag_(.+)}");
        Matcher matcher = pattern.matcher(string);

        while (matcher.find()) {
            String temp = parseTag(user, matcher.group(1));
            if (temp != null)
                string = string.replace(matcher.group(), temp);
        }

        return string;
    }

    @NotNull
    public String format(SIRUser user, String string, Object reference) {
        return format(user, string);
    }
}
