package me.croabeast.sir.module.channel;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import me.croabeast.common.util.ArrayUtils;
import me.croabeast.sir.ExtensionFile;

import java.util.List;

@Getter
final class Config {

    @Accessors(fluent = true)
    private final boolean useBukkitFormat, allowsEmpty, useSimpleLogger;

    private List<String> mutedMessages = ArrayUtils.toList("<P> &cYou are muted, you can not send messages.");
    private List<String> allowEmptyMessages = ArrayUtils.toList("<P> &cYou can not send an empty message.");

    private String simpleLoggerFormat = " &7{player}: {message}";

    @SneakyThrows
    Config(Channels main) {
        ExtensionFile file = new ExtensionFile(main, "config", true);

        useBukkitFormat = file.get("default-bukkit-format", false);
        allowsEmpty = file.get("allow-empty.enabled", false);
        useSimpleLogger = file.get("simple-logger.enabled", false);

        mutedMessages = file.toStringList("user-muted", mutedMessages);
        allowEmptyMessages = file.toStringList("allow-empty.message", allowEmptyMessages);
        simpleLoggerFormat = file.get("simple-logger.format", simpleLoggerFormat);
    }
}
