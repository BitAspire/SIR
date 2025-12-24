package me.croabeast.sir.module.vanish;

import lombok.Getter;
import lombok.SneakyThrows;
import me.croabeast.common.util.ArrayUtils;
import me.croabeast.sir.ExtensionFile;

import java.util.List;

@Getter
final class Config {

    private final boolean chatEnabled;
    private String chatKey = "?";
    private final boolean regex, prefix;
    private List<String> notAllowed = ArrayUtils.toList("<P> &cYou are not allowed to chat when you are vanished.");

    @SneakyThrows
    Config(Vanish main) {
        ExtensionFile file = new ExtensionFile(main, "config", true);

        chatEnabled = file.get("vanish-chat.enabled", false);
        chatKey = file.get("vanish-chat.key", chatKey);
        regex = file.get("vanish-chat.regex", false);
        prefix = file.get("vanish-chat.prefix", true);

        notAllowed = file.toStringList("vanish-chat.not-allowed-messages", notAllowed);
    }
}
