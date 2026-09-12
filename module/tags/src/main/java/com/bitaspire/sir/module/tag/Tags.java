package com.bitaspire.sir.module.tag;

import com.bitaspire.sir.PluginDependant;
import com.bitaspire.sir.UserFormatter;
import com.bitaspire.sir.module.SIRModule;
import com.bitaspire.sir.user.SIRUser;
import me.croabeast.takion.logger.LogLevel;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tags extends SIRModule implements UserFormatter<Object>, PluginDependant {

    private static final String PAPI = "PlaceholderAPI";

    private static final Pattern TAG_PATTERN = Pattern.compile("(?i)\\{tag_([^}]+)}");
    private static final Pattern GROUP_PREFIX = Pattern.compile("(?i)group:(.+)");
    private static final Pattern DEFAULT_TAG = Pattern.compile("(?i)default");

    Data data;
    private Object hook;

    @NotNull
    public String[] getSoftDependencies() {
        return new String[]{PAPI};
    }

    @Override
    public boolean register() {
        data = new Data(this);

        if (!isPluginEnabled(PAPI))
            return true;

        try {
            hook = new TagExpansion(this);
            if (!((com.bitaspire.sir.PAPIExpansion) hook).register()) {
                hook = null;
                getLogger().log(LogLevel.WARN,
                        "PlaceholderAPI expansion 'sir_tag' could not be registered; continuing without PAPI placeholders.");
            }
        } catch (LinkageError | RuntimeException e) {
            hook = null;
            getLogger().log(LogLevel.WARN,
                    "PlaceholderAPI expansion 'sir_tag' could not be registered: " + e.getMessage());
        }
        return true;
    }

    @Override
    public boolean unregister() {
        if (hook == null) return true;
        try {
            return ((com.bitaspire.sir.PAPIExpansion) hook).unregister();
        } catch (NoClassDefFoundError e) {
            return true;
        }
    }

    String parseTag(SIRUser user, String string) {
        if (GROUP_PREFIX.matcher(string).matches()) {
            List<Tag> tags = data.fromGroup(user, string.split(":")[1]);
            if (tags.isEmpty()) return null;

            String name = tags.get(0).getTag();
            return StringUtils.isNotBlank(name) ? name : null;
        }

        if (DEFAULT_TAG.matcher(string).matches()) {
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

        Matcher matcher = TAG_PATTERN.matcher(string);
        if (!matcher.find()) return string;

        StringBuffer buffer = new StringBuffer(string.length());
        do {
            String temp = parseTag(user, matcher.group(1));
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(
                    temp != null ? temp : matcher.group()));
        } while (matcher.find());

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    @NotNull
    public String format(SIRUser user, String string, Object reference) {
        return format(user, string);
    }
}
