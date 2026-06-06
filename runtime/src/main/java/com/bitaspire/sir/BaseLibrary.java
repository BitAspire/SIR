package com.bitaspire.sir;

import lombok.SneakyThrows;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.takion.TakionLib;
import me.croabeast.takion.channel.Channel;
import me.croabeast.takion.logger.TakionLogger;
import me.croabeast.takion.message.MessageSender;
import me.croabeast.takion.placeholder.PlaceholderManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.TreeMap;
import java.util.regex.Pattern;

class BaseLibrary extends TakionLib {

    private static final String[][] PLACEHOLDER_ALIASES = {
            {"{PLAYER}", "{player}"},
            {"{TARGET}", "{target}"},
            {"{UUID}", "{uuid}"},
            {"{WORLD}", "{world}"},
            {"{MESSAGE}", "{message}"},
            {"{SENDER}", "{sender}"},
            {"{RECEIVER}", "{receiver}"},
            {"{TYPE}", "{type}"},
            {"{TIME}", "{time}"},
            {"{PERM}", "{perm}"},
            {"{PERMISSION}", "{perm}"},
            {"{ADMIN}", "{admin}"},
            {"{REASON}", "{reason}"},
            {"{NICK}", "{nick}"},
            {"{CHANNEL}", "{channel}"},
            {"{STATE}", "{state}"},
            {"{NAME}", "{name}"},
            {"{VERSION}", "{version}"},
            {"{playerDisplayName}", "{display-name}"},
            {"{displayName}", "{display-name}"},
            {"{DISPLAYNAME}", "{display-name}"},
            {"{DISPLAY_NAME}", "{display-name}"},
            {"{playerGameMode}", "{game-mode}"},
            {"{gameMode}", "{game-mode}"},
            {"{GAMEMODE}", "{game-mode}"},
            {"{GAME_MODE}", "{game-mode}"},
            {"{playerUUID}", "{uuid}"},
            {"{playerWorld}", "{world}"},
            {"{playerX}", "{x}"},
            {"{playerY}", "{y}"},
            {"{playerZ}", "{z}"},
            {"{playerYaw}", "{yaw}"},
            {"{playerPitch}", "{pitch}"},
            {"{X}", "{x}"},
            {"{Y}", "{y}"},
            {"{Z}", "{z}"},
            {"{YAW}", "{yaw}"},
            {"{PITCH}", "{pitch}"},
            {"{ignoreUsers}", "{ignore-users}"},
            {"{mutedUsers}", "{muted-users}"},
            {"{nickUsers}", "{nick-users}"},
            {"{moduleStates}", "{module-states}"},
            {"{commandStates}", "{command-states}"}
    };

    private final Plugin plugin;
    private final SIRApi api;

    private ConfigurableFile bossbars;
    private ConfigurableFile webhooks;

    @SneakyThrows
    BaseLibrary(@NotNull SIRApi api) {
        super(api.getPlugin());
        this.api = api;
        this.plugin = api.getPlugin();

        registerPlayerPlaceholders();

        Channel channel = getChannelManager().identify("action_bar");
        channel.addPrefix("actionbar");
        channel.addPrefix("action-bar");
    }

    private void registerPlayerPlaceholders() {
        PlaceholderManager manager = getPlaceholderManager();

        manager.edit("{playerDisplayName}", "{display-name}");
        manager.edit("{playerUUID}", "{uuid}");
        manager.edit("{playerWorld}", "{world}");
        manager.edit("{playerGameMode}", "{game-mode}");
        manager.edit("{playerX}", "{x}");
        manager.edit("{playerY}", "{y}");
        manager.edit("{playerZ}", "{z}");
        manager.edit("{playerYaw}", "{yaw}");
        manager.edit("{playerPitch}", "{pitch}");

        manager.load("{playerDisplayName}", Player::getDisplayName);
        manager.load("{displayName}", Player::getDisplayName);
        manager.load("{DISPLAYNAME}", Player::getDisplayName);
        manager.load("{DISPLAY_NAME}", Player::getDisplayName);
        manager.load("{playerUUID}", player -> player.getUniqueId().toString());
        manager.load("{UUID}", player -> player.getUniqueId().toString());
        manager.load("{playerWorld}", player -> player.getWorld().getName());
        manager.load("{WORLD}", player -> player.getWorld().getName());
        manager.load("{playerGameMode}", player -> player.getGameMode().name());
        manager.load("{gameMode}", player -> player.getGameMode().name());
        manager.load("{GAMEMODE}", player -> player.getGameMode().name());
        manager.load("{GAME_MODE}", player -> player.getGameMode().name());
        manager.load("{playerX}", player -> player.getLocation().getX());
        manager.load("{playerY}", player -> player.getLocation().getY());
        manager.load("{playerZ}", player -> player.getLocation().getZ());
        manager.load("{playerYaw}", player -> player.getLocation().getYaw());
        manager.load("{playerPitch}", player -> player.getLocation().getPitch());
        manager.load("{X}", player -> player.getLocation().getX());
        manager.load("{Y}", player -> player.getLocation().getY());
        manager.load("{Z}", player -> player.getLocation().getZ());
        manager.load("{YAW}", player -> player.getLocation().getYaw());
        manager.load("{PITCH}", player -> player.getLocation().getPitch());

        manager.load("{prefix}", api.getChat()::getPrefix);
        manager.load("{suffix}", api.getChat()::getSuffix);
    }

    protected String normalizePlaceholderAliases(String message) {
        if (message == null || message.indexOf('{') < 0) return message;

        String normalized = message;
        for (String[] alias : PLACEHOLDER_ALIASES)
            normalized = normalized.replace(alias[0], alias[1]);

        return normalized;
    }

    public void reload() {
        super.setServerLogger(new TakionLogger(this, false) {
            public boolean isColored() {
                return api.getConfiguration().isColoredConsole();
            }
            public boolean isStripPrefix() {
                return !api.getConfiguration().isShowPrefix();
            }
        });

        super.setLogger(new TakionLogger(this) {
            public boolean isColored() {
                return api.getConfiguration().isColoredConsole();
            }
            public boolean isStripPrefix() {
                return !api.getConfiguration().isShowPrefix();
            }
        });

        super.setLoadedSender(new MessageSender(this) {
            {
                addFunctions(BaseLibrary.this::normalizePlaceholderAliases);

                UserFormatter<?> emojis = api.getModuleManager().getFormatter("Emojis");
                if (emojis != null) addFunctions(emojis::format);

                UserFormatter<?> tags = api.getModuleManager().getFormatter("Tags");
                if (tags != null) addFunctions(tags::format);

                setSensitive(false);
                setErrorPrefix("&c[X]&7 ");
            }
        });

        try {
            bossbars = new ConfigurableFile(plugin, "bossbars");
            bossbars.saveDefaults();
        } catch (Exception ignored) {}

        try {
            webhooks = new ConfigurableFile(plugin, "webhooks");
            webhooks.saveDefaults();
        } catch (Exception ignored) {}
    }

    @Override
    public void setServerLogger(TakionLogger logger) {
        throw new IllegalStateException("Server TakionLogger can not be set");
    }

    @Override
    public void setLogger(TakionLogger logger) {
        throw new IllegalStateException("TakionLogger can not be set");
    }

    @Override
    public void setLoadedSender(MessageSender loadedSender) {
        throw new IllegalStateException("MessageSender can not be set");
    }

    @NotNull
    public String getLangPrefixKey() {
        return api.getConfiguration().getPrefixKey();
    }

    @NotNull
    public String getLangPrefix() {
        return api.getConfiguration().getPrefix();
    }

    @NotNull
    public String getCenterPrefix() {
        return api.getConfiguration().getCenterPrefix();
    }

    @NotNull
    public String getLineSeparator() {
        return Pattern.quote(api.getConfiguration().getLineSeparator());
    }

    @NotNull
    public TreeMap<String, ConfigurationSection> getLoadedBossbars() {
        return loadMapFromConfiguration(bossbars == null ? null : bossbars.getSection("bossbars"));
    }

    @NotNull
    public TreeMap<String, ConfigurationSection> getLoadedWebhooks() {
        return loadMapFromConfiguration(webhooks == null ? null : webhooks.getSection("webhooks"));
    }
}
