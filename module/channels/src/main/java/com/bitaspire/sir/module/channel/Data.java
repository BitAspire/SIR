package com.bitaspire.sir.module.channel;

import com.bitaspire.sir.module.channel.channel.Layout;
import com.bitaspire.sir.module.channel.channel.Loader;
import lombok.Getter;
import lombok.SneakyThrows;
import com.bitaspire.sir.PermissibleUnit;
import com.bitaspire.sir.channel.ChatChannel;
import com.bitaspire.sir.user.SIRUser;
import org.bukkit.configuration.ConfigurationSection;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
final class Data {

    private ChatChannel defaultChannel = null;

    private final Map<String, ChatChannel> locals = new LinkedHashMap<>();
    private final Map<String, ChatChannel> globals = new LinkedHashMap<>();
    private final List<ChatChannel> routed = new ArrayList<>();
    private final List<ChatChannel> fallbacks = new ArrayList<>();

    private boolean hasExplicitAccess(ChatChannel channel) {
        return channel != null
                && channel.isLocalAccessible()
                && (!channel.getAccess().getPrefixes().isEmpty()
                || !channel.getAccess().getCommands().isEmpty());
    }

    @SneakyThrows
    Data(Channels main) {
        Layout layout = new Loader(main).load();

        ConfigurationSection section = layout.getDefaults();
        if (section != null && section.getBoolean("enabled", true))
            defaultChannel = new Factory.BaseChannel(main, section, null) {

                @Nullable
                public ChatChannel getSubChannel() {
                    return null;
                }
            };

        List<ChatChannel> channels = new ArrayList<>();
        PermissibleUnit
                .loadUnits(layout.getChannels(), s -> new ChannelImpl(main, s))
                .forEach(c -> {
                    channels.add(c);

                    ChatChannel sub = c.getSubChannel();
                    if (sub != null) channels.add(sub);
                });

        channels.sort(PermissibleUnit.getDefaultOrder());
        for (ChatChannel channel : channels) {
            (channel.isLocal() ? locals : globals).put(channel.getName(), channel);

            if (hasExplicitAccess(channel))
                routed.add(channel);

            if (channel.getAccess().isDefault())
                fallbacks.add(channel);
        }
    }

    private boolean matches(SIRUser user, ChatChannel channel) {
        return user != null && channel != null
                && (channel.isDefaultPermission() || channel.hasPermission(user))
                && (StringUtils.isBlank(channel.getGroup()) || channel.isInGroup(user));
    }

    ChatChannel getAccessible(SIRUser user, String string, boolean usePrefix) {
        for (ChatChannel channel : routed) {
            if (!matches(user, channel)) continue;

            if (usePrefix ?
                    channel.isAccessibleByPrefix(string) :
                    channel.isAccessibleByCommand(string))
                return channel;
        }

        return null;
    }

    ChatChannel getFallback(SIRUser user) {
        for (ChatChannel channel : fallbacks)
            if (matches(user, channel)) return channel;

        return matches(user, defaultChannel) ? defaultChannel : null;
    }

    @Getter
    final class ChannelImpl extends Factory.BaseChannel {

        private ChatChannel subChannel = null;

        ChannelImpl(Channels main, ConfigurationSection section) {
            super(main, section, defaultChannel);

            ConfigurationSection local = section.getConfigurationSection("local");
            if (local == null) return;

            subChannel = new Factory.BaseChannel(
                    main, local, this,
                    local.getString("name", getName() + "-local")
            ) {

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
