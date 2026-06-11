package com.bitaspire.sir.module.emoji;

import com.bitaspire.sir.ChatCompletions;
import com.bitaspire.sir.PluginDependant;
import com.bitaspire.sir.UserFormatter;
import com.bitaspire.sir.module.SIRModule;
import com.bitaspire.sir.user.SIRUser;
import me.croabeast.takion.logger.LogLevel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Emojis extends SIRModule implements UserFormatter<Object>, PluginDependant {

    private static final String PAPI = "PlaceholderAPI";

    Data data;
    private Object hook;
    private ChatCompletions completions;

    @NotNull
    public String[] getSoftDependencies() {
        return new String[]{PAPI};
    }

    @Override
    public boolean register() {
        data = new Data(this);
        completions = new ChatCompletions(getApi(), this::getCompletions);
        completions.register();

        if (!isPluginEnabled(PAPI))
            return true;

        try {
            hook = new EmojiExpansion(data);
            if (!((com.bitaspire.sir.PAPIExpansion) hook).register()) {
                hook = null;
                getLogger().log(LogLevel.WARN,
                        "PlaceholderAPI expansion 'sir_emoji' could not be registered; continuing without PAPI placeholders.");
            }
        } catch (LinkageError | RuntimeException e) {
            hook = null;
            getLogger().log(LogLevel.WARN,
                    "PlaceholderAPI expansion 'sir_emoji' could not be registered: " + e.getMessage());
        }
        return true;
    }

    @Override
    public boolean unregister() {
        if (completions != null) {
            completions.unregister();
            completions = null;
        }

        if (hook == null) return true;
        try {
            return ((com.bitaspire.sir.PAPIExpansion) hook).unregister();
        } catch (NoClassDefFoundError e) {
            return true;
        }
    }

    @NotNull
    public String format(SIRUser user, String string) {
        if (!isEnabled() || data.emojis.isEmpty())
            return string;

        for (Emoji emoji : data.emojis.values())
            string = emoji.parse(user, string);

        return string;
    }

    @NotNull
    public String format(SIRUser user, String string, Object reference) {
        return format(user, string);
    }

    private Collection<String> getCompletions(SIRUser user) {
        if (user == null || !user.isOnline() || !isEnabled() || data == null || data.emojis.isEmpty())
            return Collections.emptyList();

        List<String> values = new ArrayList<>();
        for (Emoji emoji : data.emojis.values()) {
            if (emoji.isCompletionEligible() && emoji.canUse(user))
                values.add(emoji.getKey());
        }
        return values;
    }
}
