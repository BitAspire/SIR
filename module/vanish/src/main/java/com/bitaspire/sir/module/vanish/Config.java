package com.bitaspire.sir.module.vanish;

import lombok.Getter;
import lombok.SneakyThrows;
import me.croabeast.common.util.ArrayUtils;
import com.bitaspire.sir.file.ExtensionFile;

import java.util.List;
import java.util.regex.Pattern;

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
        chatPattern = regex && chatKey != null && !chatKey.isEmpty() ? Pattern.compile(chatKey) : null;

        notAllowed = file.toStringList("vanish-chat.not-allowed-messages", notAllowed);
    }
}
