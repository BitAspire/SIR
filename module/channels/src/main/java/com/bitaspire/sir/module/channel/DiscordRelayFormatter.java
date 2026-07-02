package com.bitaspire.sir.module.channel;

import com.bitaspire.sir.channel.ChatChannel;
import com.bitaspire.sir.user.SIRUser;
import lombok.experimental.UtilityClass;
import me.croabeast.common.util.ReplaceUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

@UtilityClass
class DiscordRelayFormatter {

    @NotNull
    UnaryOperator<String> create(@NotNull ChatChannel channel,
                                 @NotNull SIRUser user,
                                 @NotNull String message) {
        String[] keys = channel.getChatKeys();
        String[] values = channel.getChatValues(user, message);

        List<String> relayKeys = new ArrayList<>(keys.length);
        List<String> relayValues = new ArrayList<>(values.length);

        for (int i = 0; i < keys.length && i < values.length; i++) {
            String key = keys[i];
            if (isPlayerChatAffix(key)) continue;

            relayKeys.add(key);
            relayValues.add(values[i]);
        }

        String[] keyArray = relayKeys.toArray(new String[0]);
        String[] valueArray = relayValues.toArray(new String[0]);

        return string -> ReplaceUtils.replaceEach(keyArray, valueArray, string);
    }

    private boolean isPlayerChatAffix(String key) {
        return "{prefix}".equals(key) || "{suffix}".equals(key);
    }
}
