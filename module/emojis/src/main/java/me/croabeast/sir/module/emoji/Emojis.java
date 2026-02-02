package me.croabeast.sir.module.emoji;

import me.croabeast.sir.PluginDependant;
import me.croabeast.sir.UserFormatter;
import me.croabeast.sir.module.SIRModule;
import me.croabeast.sir.user.SIRUser;
import org.jetbrains.annotations.NotNull;

public final class Emojis extends SIRModule implements UserFormatter<Object>, PluginDependant {

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
            return ((me.croabeast.sir.PAPIExpansion) hook).register();
        } catch (NoClassDefFoundError e) {
            return true;
        }
    }

    @Override
    public boolean unregister() {
        if (hook == null) return true;
        try {
            return ((me.croabeast.sir.PAPIExpansion) hook).unregister();
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
