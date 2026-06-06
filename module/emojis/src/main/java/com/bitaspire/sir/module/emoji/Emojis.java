package com.bitaspire.sir.module.emoji;

import com.bitaspire.sir.PluginDependant;
import com.bitaspire.sir.UserFormatter;
import com.bitaspire.sir.module.SIRModule;
import com.bitaspire.sir.user.SIRUser;
import me.croabeast.takion.logger.LogLevel;
import org.jetbrains.annotations.NotNull;

public class Emojis extends SIRModule implements UserFormatter<Object>, PluginDependant {

    private static final String PAPI = "PlaceholderAPI";

    Data data;
    private Object hook;

    @NotNull
    @Override
    public String[] getSoftDependencies() {
        return new String[]{PAPI};
    }

    @Override
    public boolean register() {
        data = new Data(this);

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
}
