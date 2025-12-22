package me.croabeast.sir;

import lombok.SneakyThrows;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.sir.module.ModuleManager;
import me.croabeast.sir.module.emoji.Emojis;
import me.croabeast.sir.module.tag.Tags;
import me.croabeast.takion.TakionLib;
import me.croabeast.takion.logger.TakionLogger;
import me.croabeast.takion.message.MessageSender;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.TreeMap;
import java.util.regex.Pattern;

final class Library extends TakionLib {

    private ConfigurableFile bossbars, webhooks;
    private final SIRPlugin instance;

    @SneakyThrows
    Library(SIRPlugin instance) {
        super(instance);
        this.instance = instance;

        getPlaceholderManager().edit("{playerDisplayName}", "{displayName}");
        getPlaceholderManager().edit("{playerUUID}", "{uuid}");
        getPlaceholderManager().edit("{playerWorld}", "{world}");
        getPlaceholderManager().edit("{playerGameMode}", "{gamemode}");
        getPlaceholderManager().edit("{playerX}", "{x}");
        getPlaceholderManager().edit("{playerY}", "{y}");
        getPlaceholderManager().edit("{playerZ}", "{z}");
        getPlaceholderManager().edit("{playerYaw}", "{yaw}");
        getPlaceholderManager().edit("{playerPitch}", "{pitch}");

        getPlaceholderManager().load("{prefix}", instance.getChat()::getPrefix);
        getPlaceholderManager().load("{suffix}", instance.getChat()::getSuffix);

        getChannelManager().identify("action_bar").addPrefix("action-bar");
    }

    void reload() {
        super.setServerLogger(new TakionLogger(this, false) {
            public boolean isColored() {
                return instance.getConfiguration().isColoredConsole();
            }
            public boolean isStripPrefix() {
                return !instance.getConfiguration().isShowPrefix();
            }
        });

        super.setLogger(new TakionLogger(this) {
            public boolean isColored() {
                return instance.getConfiguration().isColoredConsole();
            }
            public boolean isStripPrefix() {
                return !instance.getConfiguration().isShowPrefix();
            }
        });

        super.setLoadedSender(new MessageSender(this) {
            {
                ModuleManager manager = instance.getModuleManager();

                if (manager.isEnabled("Emojis")) {
                    Emojis emojis = manager.getModule(Emojis.class);
                    if (emojis != null) addFunctions(emojis::parseEmojis);
                }

                if (manager.isEnabled("Tags")) {
                    Tags tags = manager.getModule(Tags.class);
                    if (tags != null) addFunctions(tags::parseTags);
                }

                setSensitive(false);
                setErrorPrefix("&c[X]&7 ");
            }
        });

        try {
            bossbars = new ConfigurableFile(instance, "bossbars");
            bossbars.saveDefaults();
        } catch (Exception ignored) {}

        try {
            webhooks = new ConfigurableFile(instance, "webhooks");
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
        return instance.getConfiguration().getPrefixKey();
    }

    @NotNull
    public String getLangPrefix() {
        return instance.getConfiguration().getPrefix();
    }

    @NotNull
    public String getCenterPrefix() {
        return instance.getConfiguration().getCenterPrefix();
    }

    @NotNull
    public String getLineSeparator() {
        return Pattern.quote(instance.getConfiguration().getLineSeparator());
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
