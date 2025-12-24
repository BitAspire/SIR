package me.croabeast.sir.module.channel;

import lombok.Getter;
import lombok.SneakyThrows;
import me.croabeast.sir.ChatChannel;
import me.croabeast.sir.ExtensionFile;
import me.croabeast.sir.PermissibleUnit;
import me.croabeast.sir.user.SIRUser;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
final class Data {

    private ChatChannel defaultChannel = null;

    private final Map<String, ChatChannel> locals = new LinkedHashMap<>();
    private final Map<String, ChatChannel> globals = new LinkedHashMap<>();

    @SneakyThrows
    Data(Channels main) {
        ExtensionFile file = new ExtensionFile(main, "channels", true);

        ConfigurationSection section = file.getSection("default-channel");
        if (section != null && section.getBoolean("enabled", true))
            defaultChannel = new Factory.BaseChannel(main, section, null) {

                @Override
                public boolean isGlobal() {
                    return true;
                }

                @Nullable
                public ChatChannel getSubChannel() {
                    return null;
                }
            };

        PermissibleUnit
                .loadUnits(file.getSection("channels"), s -> new ChannelImpl(main, s))
                .forEach(c -> (c.isLocal() ? locals : globals).put(c.getName(), c));
    }

    ChatChannel getLocal(SIRUser user, String string, boolean usePrefix) {
        for (ChatChannel channel : locals.values()) {
            if (!channel.hasPermission(user)) continue;

            if (usePrefix ?
                    channel.isAccessibleByPrefix(string) :
                    channel.isAccessibleByCommand(string))
                return channel;
        }

        return null;
    }

    ChatChannel getGlobal(SIRUser user) {
        for (ChatChannel channel : globals.values())
            if (channel.hasPermission(user)) return channel;

        return defaultChannel;
    }

    @Getter
    final class ChannelImpl extends Factory.BaseChannel {

        private ChatChannel subChannel = null;

        ChannelImpl(Channels main, ConfigurationSection section) {
            super(main, section, defaultChannel);

            ConfigurationSection local = section.getConfigurationSection("local");
            if (local == null) return;

            subChannel = new Factory.BaseChannel(main, local, null) {

                @Override
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
}
