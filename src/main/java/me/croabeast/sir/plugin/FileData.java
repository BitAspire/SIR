package me.croabeast.sir.plugin;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import me.croabeast.lib.file.ConfigurableFile;
import me.croabeast.lib.file.YAMLFile;
import me.croabeast.lib.util.Exceptions;
import me.croabeast.sir.plugin.misc.FileKey;
import me.croabeast.sir.plugin.misc.DelayLogger;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@UtilityClass
public class FileData {

    final Map<String, ConfigurableFile> FILE_MAP;
    private final List<String> FILE_PATHS;

    static {
        FILE_MAP = new LinkedHashMap<>();

        FILE_PATHS = SIRPlugin.getJarEntries().copy()
                .filter(s -> s.matches("^resources/.*[.]yml$"))
                .apply(s -> s.substring(10)
                        .replace("/", File.separator)
                        .replace(".yml", ""))
                .toList();
    }

    private static class Counter {

        private int loaded = 0;
        private int updated = 0;
        private int failed = 0;

        boolean isModified() {
            return loaded > 0 || updated > 0 || failed > 0;
        }

        void clear() {
            updated = 0;
            loaded = 0;
            failed = 0;
        }
    }

    private final Counter FILES_COUNTER = new Counter();
    private boolean areFilesLoaded = false;

    private ConfigurableFile folderPath(String... args) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < args.length; i++) {
            String temp = args[i];
            if (temp == null) continue;

            builder.append(temp);
            if (i != args.length - 1)
                builder.append(File.separatorChar);
        }

        return FILE_MAP.get(builder.toString());
    }

    private static class SIRFile extends ConfigurableFile {

        SIRFile(String folder, String name) throws IOException {
            super(SIRPlugin.getInstance(), folder, name);
            setResourcePath("resources" + File.separator + getLocation());
        }

        @Override
        public boolean isUpdatable() {
            return get("update", false);
        }
    }

    public DelayLogger loadFiles() {
        DelayLogger logger = new DelayLogger();

        logger.add("&e[Files]", true);
        if (FILES_COUNTER.isModified()) FILES_COUNTER.clear();

        if (areFilesLoaded) {
            for (YAMLFile file : FILE_MAP.values()) {
                file.reload();
                FILES_COUNTER.loaded++;

                if (file.update()) FILES_COUNTER.updated++;
            }
        }
        else {
            for (String path : FILE_PATHS) {
                String separator = Pattern.quote(File.separator);
                String[] parts = path.split(separator);

                final String folder, name;
                int length = parts.length;

                if (length > 1) {
                    StringBuilder builder = new StringBuilder();
                    int last = length - 1;

                    for (int i = 0; i < last; i++) {
                        builder.append(parts[i]);

                        if (i == last - 1) continue;
                        builder.append(File.separator);
                    }

                    folder = builder.toString();
                    name = parts[last];
                } else {
                    folder = null;
                    name = path;
                }

                ConfigurableFile file;
                try {
                    file = new SIRFile(folder, name);
                } catch (Exception e) {
                    FILES_COUNTER.failed++;
                    e.printStackTrace();
                    continue;
                }

                if (file.saveDefaults()) {
                    FILE_MAP.put(path, file);
                    FILES_COUNTER.loaded++;

                    if (file.update()) FILES_COUNTER.updated++;
                    continue;
                }

                FILES_COUNTER.failed++;
            }

            areFilesLoaded = true;
        }

        return logger.add(true,
                "- Loaded: " + FILES_COUNTER.loaded + '/' + FILE_PATHS.size(),
                "- Updated: " + FILES_COUNTER.updated + '/' + FILE_PATHS.size(),
                "- Failed: " + FILES_COUNTER.failed + '/' + FILE_PATHS.size()
        );
    }

    interface BaseKey extends FileKey<Object> {}

    public enum Main implements BaseKey {
        CONFIG,
        BOSSBARS,
        WEBHOOKS;

        @NotNull
        public String getName() {
            return name().toLowerCase(Locale.ENGLISH);
        }

        @NotNull
        public ConfigurableFile getFile() {
            return FILE_MAP.get(getName());
        }
    }

    public enum Module implements FileKey<String> {
        ADVANCEMENT("advancements"),
        ANNOUNCEMENT("announcements"),
        JOIN_QUIT, MOTD;

        private final String folder;

        Module(String folder) {
            this.folder = folder == null ? getName() : folder;
        }

        Module() {
            this(null);
        }

        @NotNull
        public String getName() {
            return name().toLowerCase(Locale.ENGLISH);
        }

        @NotNull
        public ConfigurableFile getFile() {
            return getFile("config");
        }

        @NotNull
        public ConfigurableFile getFile(String name) {
            return folderPath("modules", folder, Exceptions.validate(StringUtils::isNotBlank, name));
        }

        @NotNull
        public static ConfigurableFile getMain() {
            return folderPath("modules", "modules");
        }

        public enum Chat implements BaseKey {
            CHANNELS,
            COOLDOWNS,
            EMOJIS,
            MENTIONS,
            TAGS,
            MODERATION;

            @NotNull
            public String getName() {
                return name().toLowerCase(Locale.ENGLISH);
            }

            @NotNull
            public ConfigurableFile getFile() {
                return folderPath("modules", "chat", getName());
            }

            @NotNull
            public static ConfigurableFile getMain() {
                return folderPath("modules", "chat", "config");
            }
        }

        public enum Hook implements BaseKey {
            DISCORD,
            LOGIN,
            VANISH;

            @NotNull
            public String getName() {
                return name().toLowerCase(Locale.ENGLISH);
            }

            @NotNull
            public ConfigurableFile getFile() {
                return folderPath("modules", "hook", getName());
            }
        }
    }

    @Getter
    public enum Command implements BaseKey {
        ANNOUNCER,
        SIR,
        MSG_REPLY("msg-reply"),
        PRINT;

        private final String name;

        Command(String name) {
            this.name = name;
        }

        Command() {
            this.name = name().toLowerCase(Locale.ENGLISH);
        }

        @NotNull
        public ConfigurableFile getFile() {
            return folderPath("commands", name);
        }

        @NotNull
        public static ConfigurableFile getMain() {
            return folderPath("commands", "commands");
        }

        public enum Multi implements FileKey<Boolean> {
            CHAT_VIEW,
            MUTE,
            IGNORE;

            @NotNull
            public String getName() {
                return name().toLowerCase(Locale.ENGLISH);
            }

            @NotNull
            public ConfigurableFile getFile() {
                return getFile(false);
            }

            @NotNull
            public ConfigurableFile getFile(Boolean isLang) {
                return folderPath("commands", getName(), isLang ? "lang" : "data");
            }
        }
    }
}
