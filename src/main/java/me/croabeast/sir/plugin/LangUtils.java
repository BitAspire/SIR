package me.croabeast.sir.plugin;

import me.croabeast.lib.applier.StringApplier;
import me.croabeast.lib.file.Configurable;
import me.croabeast.lib.util.TextUtils;
import me.croabeast.sir.plugin.file.FileData;
import me.croabeast.sir.plugin.manager.ModuleManager;
import me.croabeast.sir.plugin.module.PlayerFormatter;
import me.croabeast.sir.plugin.module.SIRModule;
import me.croabeast.takion.TakionLib;
import me.croabeast.takion.logger.TakionLogger;
import me.croabeast.takion.message.MessageSender;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LangUtils extends TakionLib {

    static Configurable config() {
        return FileData.Main.CONFIG.getFile();
    }

    LangUtils(SIRPlugin instance) {
        super(instance);

        getPlaceholderManager().edit("{playerDisplayName}", "{displayName}");
        getPlaceholderManager().edit("{playerUUID}", "{uuid}");
        getPlaceholderManager().edit("{playerWorld}", "{world}");
        getPlaceholderManager().edit("{playerGameMode}", "{gamemode}");
        getPlaceholderManager().edit("{playerX}", "{x}");
        getPlaceholderManager().edit("{playerY}", "{y}");
        getPlaceholderManager().edit("{playerZ}", "{z}");
        getPlaceholderManager().edit("{playerYaw}", "{yaw}");
        getPlaceholderManager().edit("{playerPitch}", "{pitch}");

        getPlaceholderManager().load("{prefix}", instance.getVaultHolder()::getPrefix);
        getPlaceholderManager().load("{suffix}", instance.getVaultHolder()::getSuffix);

        super.setLogger(new TakionLogger(this) {

            public boolean isColored() {
                return !FileData.Main.CONFIG.getFile().get("options.fix-logger", false);
            }

            public boolean isStripPrefix() {
                return !FileData.Main.CONFIG.getFile().get("options.show-prefix", false);
            }
        });

        super.setLoadedSender(new MessageSender(this) {
            {
                ModuleManager manager = instance.getModuleManager();

                PlayerFormatter<?> tags = manager.getFormatter(SIRModule.Key.TAGS);
                if (tags != null)
                    addFunctions(tags::format);

                PlayerFormatter<?> emojis = manager.getFormatter(SIRModule.Key.EMOJIS);
                if (emojis != null)
                    addFunctions(emojis::format);

                setSensitive(false);
                setErrorPrefix("&c[X]&7 ");
            }

            public boolean isLogger() {
                return config().get("options.send-console", true);
            }
        });
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
        return config().get("values.lang-prefix-key", "<P>");
    }

    @NotNull
    public String getLangPrefix() {
        return config().get("values.lang-prefix", " &e&lSIR &8>");
    }

    @NotNull
    public String getCenterPrefix() {
        return config().get("values.center-prefix", "<C>");
    }

    @NotNull
    public String getLineSeparator() {
        return Pattern.quote(config().get("values.line-separator", "<n>"));
    }

    @NotNull
    public TreeMap<String, ConfigurationSection> getLoadedWebhooks() {
        return loadMapFromConfiguration(FileData.Main.WEBHOOKS.getFile().getSection("webhooks"));
    }

    @NotNull
    public TreeMap<String, ConfigurationSection> getLoadedBossbars() {
        return loadMapFromConfiguration(FileData.Main.BOSSBARS.getFile().getSection("bossbars"));
    }

    public static void executeCommands(Player player, List<String> commands) {
        if (commands == null || commands.isEmpty()) return;

        Pattern cPattern = Pattern.compile("(?i)^\\[(global|console)]");
        Pattern pPattern = Pattern.compile("(?i)^\\[player]");

        for (String c : commands) {
            if (StringUtils.isBlank(c)) continue;

            Matcher pm = pPattern.matcher(c), cm = cPattern.matcher(c);

            StringApplier applier = StringApplier.simplified(c)
                    .apply(s -> getLib().getPlaceholderManager().replace(player, s))
                    .apply(TextUtils.STRIP_FIRST_SPACES);

            if (pm.find() && player != null) {
                String text = applier.toString().replace(pm.group(), "");

                Bukkit.dispatchCommand(player, text);
                continue;
            }

            if (cm.find()) applier.apply(s -> s.replace(cm.group(), ""));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), applier.toString());
        }
    }

    public static String stringFromArray(String[] args, int argumentIndex) {
        if (argumentIndex >= args.length) return null;
        StringBuilder b = new StringBuilder();

        for (int i = argumentIndex; i < args.length; i++)  {
            b.append(args[i]);
            if (i != args.length - 1) b.append(" ");
        }

        return b.toString();
    }
}
