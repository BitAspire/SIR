package com.bitaspire.sir.module.vanish;

import lombok.Getter;
import lombok.SneakyThrows;
import me.croabeast.common.util.ArrayUtils;
import com.bitaspire.sir.file.ExtensionFile;
import me.croabeast.takion.logger.LogLevel;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Getter
final class Config {

    private final boolean chatEnabled;
    private final boolean fakeJoinQuit;
    private String chatKey = "?";
    private final boolean regex, prefix;
    private final Pattern chatPattern;
    private List<String> notAllowed = ArrayUtils.toList("<P> &cYou are not allowed to chat when you are vanished.");

    @SneakyThrows
    Config(Vanish main) {
        ExtensionFile file = new ExtensionFile(main, "config", true);

        chatEnabled = file.get("vanish-chat.enabled", false);
        fakeJoinQuit = file.get("fake-join-quit", true);
        chatKey = file.get("vanish-chat.key", chatKey);
        regex = file.get("vanish-chat.regex", false);
        prefix = file.get("vanish-chat.prefix", true);
        chatPattern = regex && chatKey != null && !chatKey.isEmpty() ? compile(main, chatKey) : null;

        notAllowed = file.toStringList("vanish-chat.not-allowed-messages", notAllowed);
    }

    private static Pattern compile(Vanish main, String key) {
        try {
            return Pattern.compile(key);
        } catch (PatternSyntaxException e) {
            main.getLogger().log(LogLevel.WARN,
                    "Invalid 'vanish-chat.key' regex, matching it as plain text: " + e.getDescription());
            return Pattern.compile(Pattern.quote(key));
        }
    }
}
