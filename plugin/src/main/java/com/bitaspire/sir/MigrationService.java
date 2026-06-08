package com.bitaspire.sir;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

final class MigrationService {

    private static final long SECONDS_THRESHOLD = 10_000_000_000L;
    private final SIRPlugin plugin;

    MigrationService(SIRPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isPluginActive(String name) {
        Plugin pluginRef = plugin.getServer().getPluginManager().getPlugin(name);
        return pluginRef != null && pluginRef.isEnabled();
    }

    Result migrateEssentialsX() throws IOException {
        Result result = new Result();

        File essentialsFolder = findPluginFolder("Essentials", "EssentialsX");
        if (essentialsFolder == null) {
            result.path = resolveExpectedPath("Essentials", "userdata");
            return result;
        }

        result.ok = true;
        result.path = essentialsFolder.getPath();

        migrateEssentialsXConfigs(essentialsFolder, result);
        if (isPluginActive("EssentialsChat")) {
            migrateEssentialsChat(essentialsFolder, result);
        }
        if (isPluginActive("EssentialsSpawn")) {
            migrateEssentialsSpawn(essentialsFolder, result);
        }
        if (isPluginActive("EssentialsDiscord")) {
            migrateEssentialsDiscord(result);
        }
        migrateEssentialsLang(essentialsFolder, result);
        migrateEssentialsXCommandStates(essentialsFolder, result);

        File userdataFolder = new File(essentialsFolder, "userdata");
        if (!userdataFolder.isDirectory()) {
            result.backupPath = backupPluginFolderSnapshot(essentialsFolder, essentialsFolder.getName());
            return result;
        }

        File usersFolder = new File(plugin.getDataFolder(), "users");
        if (!usersFolder.exists() && !usersFolder.mkdirs())
            throw new IOException("Could not create users folder.");

        File ignoreFile = new File(usersFolder, "ignore.yml");
        File muteFile = new File(usersFolder, "mute.yml");
        File nickFile = new File(usersFolder, "nick.yml");

        YamlConfiguration ignoreConfig = YamlConfiguration.loadConfiguration(ignoreFile);
        YamlConfiguration muteConfig = YamlConfiguration.loadConfiguration(muteFile);
        YamlConfiguration nickConfig = YamlConfiguration.loadConfiguration(nickFile);

        boolean ignoreChanged = false, muteChanged = false, nickChanged = false;

        File[] files = userdataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return result;

        for (File file : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            UUID uuid = resolveUuid(file, config);
            if (uuid == null) {
                result.invalidUsers++;
                continue;
            }

            result.users++;
            String uuidKey = uuid.toString();

            String nick = resolveEssentialsNick(config);
            if (isSomething(nick)) {
                nickConfig.set(uuidKey, translateEssentialsFormatting(translateEssentialsText(nick)));
                nickChanged = true;
                result.nickUsers++;
            }

            List<String> ignored = new ArrayList<>(config.getStringList("ignored"));
            if (ignored.isEmpty()) ignored.addAll(config.getStringList("ignore"));

            Set<String> ignoredIds = parseUuids(ignored);
            if (!ignoredIds.isEmpty()) {
                ignoreConfig.set(uuidKey + ".chat.single", new ArrayList<>(ignoredIds));
                ignoreConfig.set(uuidKey + ".msg.single", new ArrayList<>(ignoredIds));
                ignoreConfig.set(uuidKey + ".chat.all", false);
                ignoreConfig.set(uuidKey + ".msg.all", false);
                ignoreChanged = true;
                result.ignoreUsers++;
                result.ignoredEntries += ignoredIds.size();
            }

            if (!config.getBoolean("muted", false)) continue;

            long expiresAt = resolveMuteExpiry(config);
            if (expiresAt > 0 && System.currentTimeMillis() > expiresAt) {
                result.expiredMutes++;
                continue;
            }

            String admin = resolveFirstString(config, "mutedby", "mutedBy", "muted-by");
            String reason = resolveFirstString(config, "mutereason", "muteReason", "mute-reason");

            muteConfig.set(uuidKey + ".muted", true);
            muteConfig.set(uuidKey + ".expiresAt", expiresAt > 0 ? expiresAt : -1L);
            muteConfig.set(uuidKey + ".remaining", expiresAt > 0 ? expiresAt - System.currentTimeMillis() : -1L);
            muteConfig.set(uuidKey + ".admin", admin == null ? "Unknown" : admin);
            muteConfig.set(uuidKey + ".reason", reason == null ? "Not following server rules." : reason);
            muteChanged = true;
            result.mutedUsers++;
        }

        if (ignoreChanged) ignoreConfig.save(ignoreFile);
        if (muteChanged) muteConfig.save(muteFile);
        if (nickChanged) nickConfig.save(nickFile);

        result.backupPath = backupPluginFolderSnapshot(essentialsFolder, essentialsFolder.getName());
        return result;
    }

    Result migrateSir() throws IOException {
        Result result = new Result();

        File sirFolder = findPluginFolder("SIR");
        if (sirFolder == null) {
            result.path = resolveExpectedPath("SIR", null);
            return result;
        }

        result.path = sirFolder.getPath();
        if (!isLegacySirFolder(sirFolder))
            return result;

        result.ok = true;

        migrateSirUsers(sirFolder, result);
        migrateSirSharedFiles(sirFolder, result);
        migrateSirModules(sirFolder, result);
        migrateSirModuleStates(sirFolder, result);
        migrateSirCommandStates(sirFolder, result);

        result.backupPath = isSameFile(sirFolder, plugin.getDataFolder())
                ? backupLegacySirArtifacts(sirFolder)
                : backupPluginFolder(sirFolder, sirFolder.getName());
        return result;
    }

    private void migrateSirUsers(File sirFolder, Result result) throws IOException {
        File commandsFolder = new File(sirFolder, "commands");
        if (!commandsFolder.isDirectory()) return;

        File usersFolder = new File(plugin.getDataFolder(), "users");
        if (!usersFolder.exists() && !usersFolder.mkdirs())
            throw new IOException("Could not create users folder.");

        YamlConfiguration ignoreTarget = YamlConfiguration.loadConfiguration(new File(usersFolder, "ignore.yml"));
        YamlConfiguration muteTarget = YamlConfiguration.loadConfiguration(new File(usersFolder, "mute.yml"));
        YamlConfiguration chatViewTarget = YamlConfiguration.loadConfiguration(new File(usersFolder, "chat-view.yml"));
        YamlConfiguration chatColorTarget = YamlConfiguration.loadConfiguration(new File(usersFolder, "chat-color.yml"));

        Set<String> migratedUsers = new HashSet<>();

        boolean ignoreChanged = mergeIgnoreData(new File(commandsFolder, "ignore" + File.separator + "data.yml"),
                ignoreTarget, result, migratedUsers);
        boolean muteChanged = mergeMuteData(new File(commandsFolder, "mute" + File.separator + "data.yml"),
                muteTarget, result, migratedUsers);
        boolean viewChanged = mergeSimpleUsers(new File(commandsFolder, "chat_view" + File.separator + "data.yml"),
                chatViewTarget, migratedUsers);
        boolean colorChanged = mergeSimpleUsers(new File(commandsFolder, "chat_color" + File.separator + "data.yml"),
                chatColorTarget, migratedUsers);

        result.users = migratedUsers.size();

        if (ignoreChanged) ignoreTarget.save(new File(usersFolder, "ignore.yml"));
        if (muteChanged) muteTarget.save(new File(usersFolder, "mute.yml"));
        if (viewChanged) chatViewTarget.save(new File(usersFolder, "chat-view.yml"));
        if (colorChanged) chatColorTarget.save(new File(usersFolder, "chat-color.yml"));
    }

    private File moduleFolder(String moduleKey) {
        return moduleFolder(new File(plugin.getDataFolder(), "modules"), moduleKey);
    }

    private File moduleFolder(File modulesFolder, String moduleKey) {
        return new File(modulesFolder, toModuleName(moduleKey));
    }

    private File moduleFile(String moduleKey, String fileName) {
        return moduleFile(new File(plugin.getDataFolder(), "modules"), moduleKey, fileName);
    }

    private File moduleFile(File modulesFolder, String moduleKey, String fileName) {
        return new File(moduleFolder(modulesFolder, moduleKey), fileName);
    }

    private void migrateSirModules(File sirFolder, Result result) throws IOException {
        File modulesFolder = new File(sirFolder, "modules");
        if (!modulesFolder.isDirectory()) return;

        File targetModulesFolder = new File(plugin.getDataFolder(), "modules");
        if (!targetModulesFolder.exists() && !targetModulesFolder.mkdirs())
            throw new IOException("Could not create modules folder.");

        copyIfPresent(new File(modulesFolder, "announcements" + File.separator + "config.yml"),
                moduleFile(targetModulesFolder, "announcements", "config.yml"),
                result);
        migrateSirAnnouncements(new File(modulesFolder, "announcements" + File.separator + "announces.yml"),
                moduleFile(targetModulesFolder, "announcements", "announcements.yml"),
                result);

        migrateSirJoinQuitConfig(new File(modulesFolder, "join_quit" + File.separator + "config.yml"),
                moduleFile(targetModulesFolder, "join-quit", "config.yml"),
                result);
        migrateSirJoinQuitMessages(new File(modulesFolder, "join_quit" + File.separator + "messages.yml"),
                moduleFile(targetModulesFolder, "join-quit", "messages.yml"),
                result);

        migrateSirAdvancements(new File(modulesFolder, "advancements" + File.separator + "config.yml"),
                moduleFile(targetModulesFolder, "advancements", "config.yml"),
                result);

        migrateSirMotd(new File(modulesFolder, "motd" + File.separator + "config.yml"),
                moduleFile(targetModulesFolder, "motd", "config.yml"),
                result);

        migrateLegacySirChannels(new File(modulesFolder, "chat" + File.separator + "channels.yml"),
                moduleFile(targetModulesFolder, "channels", "channels.yml"),
                result);
        copyIfPresent(new File(modulesFolder, "chat" + File.separator + "config.yml"),
                moduleFile(targetModulesFolder, "channels", "config.yml"),
                result);
        copyIfPresent(new File(modulesFolder, "chat" + File.separator + "tags.yml"),
                moduleFile(targetModulesFolder, "tags", "tags.yml"),
                result);
        copyIfPresent(new File(modulesFolder, "chat" + File.separator + "moderation.yml"),
                moduleFile(targetModulesFolder, "moderation", "moderation.yml"),
                result);
        copyIfPresent(new File(modulesFolder, "chat" + File.separator + "emojis.yml"),
                moduleFile(targetModulesFolder, "emojis", "emojis.yml"),
                result);
        copyIfPresent(new File(modulesFolder, "chat" + File.separator + "cooldowns.yml"),
                moduleFile(targetModulesFolder, "cooldowns", "cooldowns.yml"),
                result);
        copyIfPresent(new File(modulesFolder, "chat" + File.separator + "mentions.yml"),
                moduleFile(targetModulesFolder, "mentions", "mentions.yml"),
                result);

        migrateLegacySirDiscordConfig(new File(modulesFolder, "hook" + File.separator + "discord.yml"),
                moduleFile(targetModulesFolder, "discord", "config.yml"),
                result);
        migrateLegacySirLoginConfig(new File(modulesFolder, "hook" + File.separator + "login.yml"),
                moduleFile(targetModulesFolder, "join-quit", "config.yml"),
                result);
        migrateLegacySirVanishConfig(new File(modulesFolder, "hook" + File.separator + "vanish.yml"),
                moduleFile(targetModulesFolder, "vanish", "config.yml"),
                result);
    }

    private void migrateSirSharedFiles(File sirFolder, Result result) throws IOException {
        copyIfPresent(new File(sirFolder, "bossbars.yml"),
                new File(plugin.getDataFolder(), "bossbars.yml"),
                result);
        copyIfPresent(new File(sirFolder, "webhooks.yml"),
                new File(plugin.getDataFolder(), "webhooks.yml"),
                result);
    }

    private void migrateLegacySirChannels(File sourceFile, File targetFile, Result result) throws IOException {
        if (!sourceFile.isFile()) return;

        YamlConfiguration source = YamlConfiguration.loadConfiguration(sourceFile);
        YamlConfiguration target = new YamlConfiguration();
        target.set("version", 2);

        migrateLegacySirChannelSection(
                source.getConfigurationSection("default-channel"),
                target.createSection("default-channel"),
                true,
                false
        );

        ConfigurationSection sourceChannels = source.getConfigurationSection("channels");
        ConfigurationSection targetChannels = target.createSection("channels");

        if (sourceChannels != null)
            for (String key : sourceChannels.getKeys(false)) {
                ConfigurationSection sourceChannel = sourceChannels.getConfigurationSection(key);
                if (sourceChannel == null) continue;

                ConfigurationSection targetChannel = targetChannels.createSection(key);
                migrateLegacySirChannelSection(sourceChannel, targetChannel, false, false);

                ConfigurationSection sourceLocal = sourceChannel.getConfigurationSection("local");
                if (sourceLocal == null) continue;

                String localKey = resolveLegacySirLocalKey(targetChannels, key, sourceLocal.getString("name"));
                ConfigurationSection targetLocal = targetChannels.createSection(localKey);
                targetLocal.set("inherits", key);

                migrateLegacySirChannelSection(sourceLocal, targetLocal, false, true);
            }

        ensureParent(targetFile);
        target.save(targetFile);
        result.configs++;
    }

    private void migrateLegacySirChannelSection(
            ConfigurationSection source,
            ConfigurationSection target,
            boolean defaults,
            boolean forcedLocal
    ) {
        target.set("enabled", source == null || source.getBoolean("enabled", true));

        if (defaults) {
            target.set("access.default", true);
            target.set("access.strip-prefix", true);
            target.set("audience.radius", 0);
            target.set("audience.same-world", false);
            target.set("audience.include-sender", true);
            target.set("logging.enabled", false);
        }

        if (source == null) return;

        if (source.isSet("description"))
            target.set("description", source.getString("description"));
        if (source.isSet("permission"))
            target.set("permission", source.getString("permission"));
        if (source.isSet("group"))
            target.set("group", source.getString("group"));
        if (source.isSet("priority"))
            target.set("priority", source.getInt("priority"));

        boolean hasExplicitAccess = hasLegacySirExplicitAccess(source);
        boolean defaultAccess = defaults || (!forcedLocal && !hasExplicitAccess && resolveLegacySirGlobal(source, false));

        target.set("access.default", defaultAccess);
        if (defaults || forcedLocal || hasExplicitAccess || source.isSet("access.strip-prefix"))
            target.set("access.strip-prefix", source.getBoolean("access.strip-prefix", true));

        List<String> prefixes = resolveLegacySirAccessPrefixes(source);
        if (!prefixes.isEmpty())
            target.set("access.prefixes", prefixes);

        List<String> commands = resolveLegacySirAccessCommands(source);
        if (!commands.isEmpty())
            target.set("access.commands", commands);

        if (defaults || source.isSet("radius"))
            target.set("audience.radius", Math.max(0, source.getInt("radius", 0)));
        if (defaults || source.isSet("same-world"))
            target.set("audience.same-world", source.getBoolean("same-world", false));

        List<String> worlds = source.getStringList("worlds");
        if (!worlds.isEmpty())
            target.set("audience.worlds", worlds);

        String recipientPermission = firstNonBlank(
                source.getString("recipient-permission"),
                source.getString("permission")
        );
        if (hasText(recipientPermission))
            target.set("audience.permission", recipientPermission);

        String recipientGroup = firstNonBlank(
                source.getString("recipient-group"),
                source.getString("group")
        );
        if (hasText(recipientGroup))
            target.set("audience.group", recipientGroup);

        if (defaults || source.isSet("include-sender"))
            target.set("audience.include-sender", source.getBoolean("include-sender", true));

        if (source.isSet("tag"))
            target.set("style.tag", source.getString("tag"));
        if (source.isSet("prefix"))
            target.set("style.prefix", source.getString("prefix"));
        if (source.isSet("suffix"))
            target.set("style.suffix", source.getString("suffix"));

        if (hasText(source.getString("color")))
            target.set("style.colors.default", source.getString("color"));

        if (defaults || source.isSet("color-options.normal"))
            target.set("style.colors.normal", source.getBoolean("color-options.normal", false));
        if (defaults || source.isSet("color-options.special"))
            target.set("style.colors.special", source.getBoolean("color-options.special", false));
        if (defaults || source.isSet("color-options.rgb"))
            target.set("style.colors.rgb", source.getBoolean("color-options.rgb", false));

        List<String> hover = source.getStringList("hover");
        if (!hover.isEmpty())
            target.set("style.hover", hover);

        if (source.isSet("format"))
            target.set("style.format", source.getString("format"));
        migrateLegacySirClick(source, target);

        String loggingFormat = firstNonBlank(
                source.getString("logging.format"),
                source.getString("log-format")
        );
        boolean loggingEnabled = source.getBoolean("logging.enabled", hasText(loggingFormat));

        if (defaults || source.isSet("logging.enabled") || hasText(loggingFormat))
            target.set("logging.enabled", loggingEnabled);
        if (hasText(loggingFormat))
            target.set("logging.format", loggingFormat);

        boolean desiredGlobal = resolveLegacySirGlobal(source, forcedLocal);
        boolean derivedGlobal = !target.isSet("audience.radius") || target.getInt("audience.radius", 0) <= 0;

        if (desiredGlobal != derivedGlobal)
            target.set("global", desiredGlobal);
    }

    private void migrateLegacySirClick(ConfigurationSection source, ConfigurationSection target) {
        Object click = source.get("click-action");
        if (click == null) click = source.get("click");
        if (click == null) return;

        if (click instanceof String) {
            String[] split = ((String) click).replace("\"", "").split(":", 2);
            if (split.length == 0 || !hasText(split[0])) return;

            target.set("style.click.action", split[0]);
            if (split.length > 1 && hasText(split[1]))
                target.set("style.click.value", split[1]);
            return;
        }

        if (!(click instanceof ConfigurationSection)) return;

        ConfigurationSection section = (ConfigurationSection) click;
        String action = firstNonBlank(section.getString("action"), section.getString("type"));
        String value = firstNonBlank(
                section.getString("input"),
                section.getString("value"),
                section.getString("url")
        );

        if (hasText(action))
            target.set("style.click.action", action);
        if (hasText(value))
            target.set("style.click.value", value);
    }

    private boolean hasLegacySirExplicitAccess(ConfigurationSection section) {
        if (section == null) return false;

        ConfigurationSection access = section.getConfigurationSection("access");
        if (access == null) return false;

        return hasText(access.getString("prefix"))
                || !access.getStringList("prefixes").isEmpty()
                || !access.getStringList("commands").isEmpty();
    }

    private List<String> resolveLegacySirAccessPrefixes(ConfigurationSection section) {
        if (section == null) return Collections.emptyList();

        ConfigurationSection access = section.getConfigurationSection("access");
        if (access == null) return Collections.emptyList();

        LinkedHashSet<String> prefixes = new LinkedHashSet<>();
        String prefix = access.getString("prefix");
        if (hasText(prefix)) prefixes.add(prefix);
        prefixes.addAll(access.getStringList("prefixes"));

        List<String> result = new ArrayList<>();
        for (String value : prefixes)
            if (hasText(value)) result.add(value);

        return result;
    }

    private List<String> resolveLegacySirAccessCommands(ConfigurationSection section) {
        if (section == null) return Collections.emptyList();

        ConfigurationSection access = section.getConfigurationSection("access");
        if (access == null) return Collections.emptyList();

        List<String> commands = new ArrayList<>();
        for (String value : access.getStringList("commands"))
            if (hasText(value)) commands.add(value);

        return commands;
    }

    private boolean resolveLegacySirGlobal(ConfigurationSection section, boolean forcedLocal) {
        if (forcedLocal) return false;
        if (section == null) return true;
        if (section.isSet("global")) return section.getBoolean("global");
        if (section.isSet("radius")) return section.getInt("radius", 0) <= 0;
        return true;
    }

    private String resolveLegacySirLocalKey(ConfigurationSection targetChannels, String parentKey, String preferred) {
        String base = hasText(preferred) ? preferred.trim() : parentKey + "-local";
        String candidate = base;
        int index = 2;

        while (targetChannels.isSet(candidate)) {
            candidate = base + "-" + index;
            index++;
        }

        return candidate;
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;

        for (String value : values)
            if (hasText(value)) return value;

        return null;
    }

    private void migrateEssentialsXConfigs(File essentialsFolder, Result result) throws IOException {
        File configFile = new File(essentialsFolder, "config.yml");
        if (!configFile.isFile()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        migrateEssentialsJoinQuit(config, result);
        migrateEssentialsMotd(essentialsFolder, result);
    }

    private void migrateEssentialsChat(File essentialsFolder, Result result) throws IOException {
        File configFile = new File(essentialsFolder, "config.yml");
        if (!configFile.isFile()) return;

        YamlConfiguration essentialsConfig = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection chat = essentialsConfig.getConfigurationSection("chat");
        if (chat == null) return;

        String format = resolveEssentialsChatFormat(chat);
        int radius = chat.getInt("radius", 0);
        if (format == null && radius == 0) return;

        File channelsFile = moduleFile("channels", "channels.yml");
        YamlConfiguration target = channelsFile.isFile()
                ? YamlConfiguration.loadConfiguration(channelsFile)
                : new YamlConfiguration();

        if (format != null) {
            target.set("default-channel.format", translateEssentialsChatFormat(format));
            target.set("default-channel.style.format", translateEssentialsChatFormat(format));
        }

        target.set("default-channel.radius", radius);
        target.set("default-channel.audience.radius", radius);

        ensureParent(channelsFile);
        target.save(channelsFile);
        result.configs++;
    }

    private void migrateEssentialsSpawn(File essentialsFolder, Result result) throws IOException {
        File spawnFile = new File(essentialsFolder, "spawn.yml");
        if (!spawnFile.isFile()) return;

        YamlConfiguration spawnConfig = YamlConfiguration.loadConfiguration(spawnFile);
        ConfigurationSection spawns = spawnConfig.getConfigurationSection("spawns");
        if (spawns == null) return;

        File essentialsConfigFile = new File(essentialsFolder, "config.yml");
        YamlConfiguration essentialsConfig = essentialsConfigFile.isFile()
                ? YamlConfiguration.loadConfiguration(essentialsConfigFile)
                : new YamlConfiguration();

        String spawnGroup = essentialsConfig.getString("newbies.spawnpoint", "default");
        if (spawnGroup.equalsIgnoreCase("none"))
            spawnGroup = essentialsConfig.getString("spawnpoint", "default");

        boolean spawnOnJoin = essentialsConfig.getBoolean("spawn-on-join", false);

        ConfigurationSection spawnSection = spawns.getConfigurationSection(spawnGroup.toLowerCase(Locale.ENGLISH));
        if (spawnSection == null) spawnSection = spawns.getConfigurationSection("default");
        if (spawnSection == null) return;

        String world = spawnSection.getString("world");
        Double x = getDouble(spawnSection, "x");
        Double y = getDouble(spawnSection, "y");
        Double z = getDouble(spawnSection, "z");
        Double yaw = getDouble(spawnSection, "yaw");
        Double pitch = getDouble(spawnSection, "pitch");

        if (world == null || x == null || y == null || z == null) return;

        File messagesFile = moduleFile("join-quit", "messages.yml");
        YamlConfiguration target = messagesFile.isFile()
                ? YamlConfiguration.loadConfiguration(messagesFile)
                : new YamlConfiguration();

        String coordinates = formatCoordinates(x, y, z);
        String rotation = formatRotation(yaw == null ? 0.0 : yaw, pitch == null ? 0.0 : pitch);

        if (spawnOnJoin) {
            target.set("join.default.spawn.enabled", true);
            target.set("join.default.spawn.world", world);
            target.set("join.default.spawn.coordinates", coordinates);
            target.set("join.default.spawn.rotation", rotation);
        }

        if (!spawnOnJoin) {
            target.set("first-join.default.spawn.enabled", true);
            target.set("first-join.default.spawn.world", world);
            target.set("first-join.default.spawn.coordinates", coordinates);
            target.set("first-join.default.spawn.rotation", rotation);
        }

        ensureParent(messagesFile);
        target.save(messagesFile);
        result.configs++;
    }

    private void migrateEssentialsDiscord(Result result) throws IOException {
        File discordFolder = findPluginFolder("EssentialsDiscord");
        if (discordFolder == null) return;

        File configFile = new File(discordFolder, "config.yml");
        if (!configFile.isFile()) return;

        YamlConfiguration discordConfig = YamlConfiguration.loadConfiguration(configFile);

        File targetFile = moduleFile("discord", "config.yml");
        YamlConfiguration target = targetFile.isFile()
                ? YamlConfiguration.loadConfiguration(targetFile)
                : new YamlConfiguration();

        boolean changed = false;
        String guild = discordConfig.getString("guild");
        if (guild != null) {
            target.set("default-server", guild);
            changed = true;
        }

        ConfigurationSection channels = discordConfig.getConfigurationSection("channels");
        ConfigurationSection messageTypes = discordConfig.getConfigurationSection("message-types");

        changed |= updateDiscordChannel(target, "first-join", resolveDiscordChannelId(channels, messageTypes, "first-join"));
        changed |= updateDiscordChannel(target, "join", resolveDiscordChannelId(channels, messageTypes, "join"));
        changed |= updateDiscordChannel(target, "quit", resolveDiscordChannelId(channels, messageTypes, "leave"));
        changed |= updateDiscordChannel(target, "global-chat", resolveDiscordChannelId(channels, messageTypes, "chat"));
        changed |= updateDiscordChannel(target, "advancements", resolveDiscordChannelId(channels, messageTypes, "advancement"));

        ConfigurationSection messages = discordConfig.getConfigurationSection("messages");
        if (messages != null) {
            changed |= updateDiscordMessage(target, "messages.first-join.text", messages.getString("first-join"));
            changed |= updateDiscordMessage(target, "messages.join.text", messages.getString("join"));
            changed |= updateDiscordMessage(target, "messages.quit.text", messages.getString("quit"));
            changed |= updateDiscordMessage(target, "messages.global-chat.text", messages.getString("mc-to-discord"));
            changed |= updateDiscordMessage(target, "messages.advancements.text", messages.getString("advancement"));
        }

        if (changed) {
            ensureParent(targetFile);
            target.save(targetFile);
            result.configs++;
        }

        String backup = backupPluginFolderSnapshot(discordFolder, discordFolder.getName());
        if (backup != null) result.extraBackups.add(backup);
    }

    private void migrateEssentialsLang(File essentialsFolder, Result result) throws IOException {
        File configFile = new File(essentialsFolder, "config.yml");
        if (!configFile.isFile()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        String locale = resolveEssentialsLocale(config);

        String normalizedLocale = locale.trim().replace('-', '_');
        Properties messages = loadEssentialsMessages(essentialsFolder, normalizedLocale);
        if (messages == null || messages.isEmpty()) return;

        File targetFile = new File(plugin.getDataFolder(), "commands" + File.separator + "lang.yml");
        YamlConfiguration target = targetFile.isFile()
                ? YamlConfiguration.loadConfiguration(targetFile)
                : new YamlConfiguration();

        boolean changed = false;

        String noPerm = resolveProperty(messages, "noPerm");
        if (noPerm != null) {
            String value = translateEssentialsLangMessage(noPerm, Collections.singletonMap("{0}", "{perm}"));
            target.set("lang.no-permission", value);
            changed = true;
        }

        String playerNotFound = resolveProperty(messages, "playerNotFound");
        if (playerNotFound != null) {
            String value = translateEssentialsLangMessage(playerNotFound, Collections.singletonMap("{0}", "{target}"));
            target.set("lang.not-player", value);
            changed = true;
        }

        if (changed) {
            ensureParent(targetFile);
            target.save(targetFile);
            result.configs++;
        }

        migrateEssentialsMessageLang(messages, result);
        migrateEssentialsIgnoreLang(messages, result);
        migrateEssentialsMuteLang(messages, result);
        migrateEssentialsNickLang(messages, result);
        migrateEssentialsChannelsLang(messages, result);
    }

    private void migrateEssentialsMessageLang(Properties messages, Result result) throws IOException {
        File targetFile = new File(plugin.getDataFolder(), "commands" + File.separator + "message" + File.separator + "lang.yml");
        YamlConfiguration target = targetFile.isFile()
                ? YamlConfiguration.loadConfiguration(targetFile)
                : new YamlConfiguration();

        boolean changed = false;

        String consoleName = resolveProperty(messages, "consoleName");
        if (consoleName != null) {
            target.set("lang.console-formatting.name", translateEssentialsInlineValue(consoleName, null));
            changed = true;
        }

        String msgFormat = resolveProperty(messages, "msgFormat");
        if (msgFormat != null) {
            Map<String, String> placeholders = placeholders(
                    "{0}", "{sender}",
                    "{1}", "{receiver}",
                    "{2}", "{message}"
            );
            String value = translateEssentialsInlineValue(msgFormat, placeholders);

            target.set("lang.for-sender.message", value);
            target.set("lang.for-receiver.message", value);
            target.set("lang.console-formatting.format", value);
            changed = true;
        }

        if (!changed) return;

        ensureParent(targetFile);
        target.save(targetFile);
        result.configs++;
    }

    private void migrateEssentialsIgnoreLang(Properties messages, Result result) throws IOException {
        File targetFile = new File(plugin.getDataFolder(), "commands" + File.separator + "ignore" + File.separator + "lang.yml");
        YamlConfiguration target = targetFile.isFile()
                ? YamlConfiguration.loadConfiguration(targetFile)
                : new YamlConfiguration();

        boolean changed = false;

        String ignoreYourself = resolveProperty(messages, "ignoreYourself");
        if (ignoreYourself != null) {
            target.set("lang.not-yourself", translateEssentialsLangMessage(ignoreYourself, null));
            changed = true;
        }

        String ignorePlayer = resolveProperty(messages, "ignorePlayer");
        if (ignorePlayer != null) {
            target.set("lang.success.player", translateEssentialsLangMessage(ignorePlayer,
                    Collections.singletonMap("{0}", "{target}")));
            changed = true;
        }

        String unignorePlayer = resolveProperty(messages, "unignorePlayer");
        if (unignorePlayer != null) {
            target.set("lang.remove.player", translateEssentialsLangMessage(unignorePlayer,
                    Collections.singletonMap("{0}", "{target}")));
            changed = true;
        }

        if (!changed) return;

        ensureParent(targetFile);
        target.save(targetFile);
        result.configs++;
    }

    private void migrateEssentialsMuteLang(Properties messages, Result result) throws IOException {
        File targetFile = new File(plugin.getDataFolder(), "commands" + File.separator + "mute" + File.separator + "lang.yml");
        YamlConfiguration target = targetFile.isFile()
                ? YamlConfiguration.loadConfiguration(targetFile)
                : new YamlConfiguration();

        boolean changed = false;

        String permMute = firstNonBlank(
                resolveProperty(messages, "mutedPlayerReason"),
                resolveProperty(messages, "mutedPlayer")
        );
        if (permMute != null) {
            target.set("lang.action.perm", translateEssentialsLangMessage(permMute, placeholders(
                    "{0}", "{target}",
                    "{1}", "{reason}"
            )));
            changed = true;
        }

        String tempMute = firstNonBlank(
                resolveProperty(messages, "mutedPlayerForReason"),
                resolveProperty(messages, "mutedPlayerFor")
        );
        if (tempMute != null) {
            target.set("lang.action.temp", translateEssentialsLangMessage(tempMute, placeholders(
                    "{0}", "{target}",
                    "{1}", "{time}",
                    "{2}", "{reason}"
            )));
            changed = true;
        }

        String unmutedPlayer = resolveProperty(messages, "unmutedPlayer");
        if (unmutedPlayer != null) {
            target.set("lang.action.unmute", translateEssentialsLangMessage(unmutedPlayer,
                    Collections.singletonMap("{0}", "{player}")));
            changed = true;
        }

        if (!changed) return;

        ensureParent(targetFile);
        target.save(targetFile);
        result.configs++;
    }

    private void migrateEssentialsNickLang(Properties messages, Result result) throws IOException {
        File targetFile = new File(plugin.getDataFolder(), "commands" + File.separator + "nick" + File.separator + "lang.yml");
        YamlConfiguration target = targetFile.isFile()
                ? YamlConfiguration.loadConfiguration(targetFile)
                : new YamlConfiguration();

        boolean changed = false;

        String nickSet = resolveProperty(messages, "nickSet");
        if (nickSet != null) {
            target.set("lang.success.self-set", translateEssentialsLangMessage(nickSet,
                    Collections.singletonMap("{0}", "{nick}")));
            changed = true;
        }

        String nickNoMore = resolveProperty(messages, "nickNoMore");
        if (nickNoMore != null) {
            target.set("lang.success.self-reset", translateEssentialsLangMessage(nickNoMore, null));
            changed = true;
        }

        if (!changed) return;

        ensureParent(targetFile);
        target.save(targetFile);
        result.configs++;
    }

    private void migrateEssentialsChannelsLang(Properties messages, Result result) throws IOException {
        File targetFile = moduleFile("channels", "config.yml");
        YamlConfiguration target = targetFile.isFile()
                ? YamlConfiguration.loadConfiguration(targetFile)
                : new YamlConfiguration();

        String voiceSilenced = resolveProperty(messages, "voiceSilenced");
        if (voiceSilenced == null) return;

        target.set("user-muted", translateEssentialsLangMessage(voiceSilenced, null));

        ensureParent(targetFile);
        target.save(targetFile);
        result.configs++;
    }

    private String resolveEssentialsLocale(YamlConfiguration config) {
        String locale = resolveFirstString(config, "locale", "language", "lang");
        return locale == null || locale.trim().isEmpty() ? "en" : locale;
    }

    private void migrateEssentialsJoinQuit(YamlConfiguration config, Result result) throws IOException {
        String joinMessage = config.getString("custom-join-message", "none");
        String quitMessage = config.getString("custom-quit-message", "none");

        boolean hasJoinMessage = isSomething(joinMessage);
        boolean hasQuitMessage = isSomething(quitMessage);
        if (!hasJoinMessage && !hasQuitMessage) return;

        File joinQuitFolder = moduleFolder("join-quit");
        File configFile = new File(joinQuitFolder, "config.yml");
        File messagesFile = new File(joinQuitFolder, "messages.yml");

        YamlConfiguration targetConfig = configFile.isFile()
                ? YamlConfiguration.loadConfiguration(configFile)
                : new YamlConfiguration();
        YamlConfiguration targetMessages = messagesFile.isFile()
                ? YamlConfiguration.loadConfiguration(messagesFile)
                : new YamlConfiguration();

        if (hasJoinMessage)
            targetMessages.set("join.default.public", translateEssentialsText(joinMessage));
        if (hasQuitMessage)
            targetMessages.set("quit.default.public", translateEssentialsText(quitMessage));

        if (hasJoinMessage)
            targetConfig.set("disable-vanilla-messages.join", true);
        if (hasQuitMessage)
            targetConfig.set("disable-vanilla-messages.quit", true);

        ensureParent(configFile);
        targetConfig.save(configFile);
        result.configs++;

        ensureParent(messagesFile);
        targetMessages.save(messagesFile);
        result.configs++;
    }

    private void migrateEssentialsMotd(File essentialsFolder, Result result) throws IOException {
        File motdFile = new File(essentialsFolder, "motd.txt");
        if (!motdFile.isFile()) return;

        File targetFile = moduleFile("motd", "motd.yml");
        List<String> lines = Files.readAllLines(motdFile.toPath());
        if (lines.isEmpty()) return;

        YamlConfiguration target = new YamlConfiguration();
        int index = 1;
        for (String line : lines) {
            target.set("motd.first." + index, translateEssentialsText(line));
            index++;
        }

        ensureParent(targetFile);
        target.save(targetFile);
        result.configs++;
    }

    private void migrateEssentialsXCommandStates(File essentialsFolder, Result result) throws IOException {
        File configFile = new File(essentialsFolder, "config.yml");
        if (!configFile.isFile()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        List<String> disabled = config.getStringList("disabled-commands");
        if (disabled.isEmpty()) return;

        File targetDir = new File(plugin.getDataFolder(), "commands");
        if (!targetDir.exists() && !targetDir.mkdirs())
            throw new IOException("Could not create commands folder.");

        File targetFile = new File(targetDir, "states.yml");
        YamlConfiguration target = targetFile.isFile()
                ? YamlConfiguration.loadConfiguration(targetFile)
                : new YamlConfiguration();

        for (String command : disabled) {
            String mapped = mapEssentialsCommand(command);
            if (mapped == null) continue;

            String provider = resolveProvider(mapped);
            if (provider == null) continue;

            target.set("providers." + provider + ".commands." + mapped.toLowerCase(Locale.ENGLISH), false);
            result.commandStates++;
        }

        target.save(targetFile);
    }

    private String mapEssentialsCommand(String command) {
        if (command == null) return null;

        String normalized = command.toLowerCase(Locale.ENGLISH).replace("/", "");
        switch (normalized) {
            case "msg":
            case "m":
            case "w":
            case "t":
            case "pm":
            case "tell":
            case "whisper":
            case "message":
                return "message";
            case "r":
            case "reply":
                return "reply";
            case "ignore":
            case "unignore":
            case "delignore":
            case "remignore":
            case "rmignore":
                return "ignore";
            case "mute":
            case "silence":
                return "mute";
            case "tempmute":
                return "tempmute";
            case "unmute":
                return "unmute";
            case "nick":
            case "nickname":
                return "nick";
            default:
                return null;
        }
    }

    private void migrateSirModuleStates(File sirFolder, Result result) throws IOException {
        File moduleStatesFile = new File(sirFolder, "modules" + File.separator + "modules.yml");
        if (!moduleStatesFile.isFile()) return;

        YamlConfiguration source = YamlConfiguration.loadConfiguration(moduleStatesFile);
        ConfigurationSection modulesSection = source.getConfigurationSection("modules");
        if (modulesSection == null) return;

        File targetDir = new File(plugin.getDataFolder(), "modules");
        if (!targetDir.exists() && !targetDir.mkdirs())
            throw new IOException("Could not create modules folder.");

        File targetFile = new File(targetDir, "states.yml");
        YamlConfiguration target = targetFile.isFile()
                ? YamlConfiguration.loadConfiguration(targetFile)
                : new YamlConfiguration();

        Map<String, Boolean> mapping = new LinkedHashMap<>();
        mapping.put("join-quit", modulesSection.getBoolean("join-quit", true));
        mapping.put("announcements", modulesSection.getBoolean("announcements", true));
        mapping.put("motd", modulesSection.getBoolean("motd", true));
        mapping.put("advancements", modulesSection.getBoolean("advancements", true));

        ConfigurationSection chatSection = modulesSection.getConfigurationSection("chat");
        if (chatSection != null) {
            mapping.put("channels", chatSection.getBoolean("channels", true));
            mapping.put("tags", chatSection.getBoolean("tags", true));
            mapping.put("moderation", chatSection.getBoolean("moderation", true));
            mapping.put("emojis", chatSection.getBoolean("emojis", true));
            mapping.put("cooldowns", chatSection.getBoolean("cooldowns", true));
            mapping.put("mentions", chatSection.getBoolean("mentions", true));
        }

        ConfigurationSection hookSection = modulesSection.getConfigurationSection("hook");
        if (hookSection != null) {
            mapping.put("discord", hookSection.getBoolean("discord", true));
            mapping.put("login", hookSection.getBoolean("login", true));
            mapping.put("vanish", hookSection.getBoolean("vanish", true));
        }

        for (Map.Entry<String, Boolean> entry : mapping.entrySet()) {
            target.set("modules." + toModuleName(entry.getKey()) + ".enabled", entry.getValue());
            result.moduleStates++;
        }

        target.save(targetFile);
    }

    private void migrateSirCommandStates(File sirFolder, Result result) throws IOException {
        File commandsFile = new File(sirFolder, "commands" + File.separator + "commands.yml");
        if (!commandsFile.isFile()) return;

        YamlConfiguration source = YamlConfiguration.loadConfiguration(commandsFile);
        ConfigurationSection commandsSection = source.getConfigurationSection("commands");
        if (commandsSection == null) return;

        File targetDir = new File(plugin.getDataFolder(), "commands");
        if (!targetDir.exists() && !targetDir.mkdirs())
            throw new IOException("Could not create commands folder.");

        File targetFile = new File(targetDir, "states.yml");
        YamlConfiguration target = targetFile.isFile()
                ? YamlConfiguration.loadConfiguration(targetFile)
                : new YamlConfiguration();

        Map<String, String> commandMapping = new LinkedHashMap<>();
        commandMapping.put("print", "print");
        commandMapping.put("clearchat", "clear-chat");
        commandMapping.put("ignore", "ignore");
        commandMapping.put("chatcolor", "chat-color");
        commandMapping.put("msg", "message");
        commandMapping.put("reply", "reply");
        commandMapping.put("mute", "mute");
        commandMapping.put("tempmute", "tempmute");

        for (Map.Entry<String, String> entry : commandMapping.entrySet()) {
            ConfigurationSection section = commandsSection.getConfigurationSection(entry.getKey());
            if (section == null) continue;

            Boolean enabled = section.contains("enabled") ? section.getBoolean("enabled") : null;
            Boolean override = section.contains("override-existing")
                    ? section.getBoolean("override-existing")
                    : null;

            String provider = resolveProvider(entry.getValue());
            if (provider == null) continue;

            if (enabled != null) {
                target.set("providers." + provider + ".enabled", enabled);
                result.commandStates++;
            }

            if (override != null) {
                String commandKey = entry.getValue().toLowerCase(Locale.ENGLISH);
                target.set("providers." + provider + ".commands." + commandKey, override);
                result.commandStates++;
            }
        }

        target.save(targetFile);
    }

    private String resolveProvider(String commandKey) {
        switch (commandKey.toLowerCase(Locale.ENGLISH)) {
            case "clear-chat":
                return "ClearProvider";
            case "chat-color":
                return "ColorProvider";
            case "ignore":
                return "IgnoreProvider";
            case "message":
            case "reply":
                return "MessageProvider";
            case "mute":
            case "tempmute":
            case "unmute":
                return "MuteProvider";
            case "nick":
                return "NickProvider";
            case "print":
                return "PrintProvider";
            default:
                return null;
        }
    }

    private String toModuleName(String moduleKey) {
        switch (moduleKey.toLowerCase(Locale.ENGLISH)) {
            case "join-quit":
                return "JoinQuit";
            case "announcements":
                return "Announcements";
            case "motd":
                return "MOTD";
            case "advancements":
                return "Advancements";
            case "channels":
                return "Channels";
            case "tags":
                return "Tags";
            case "moderation":
                return "Moderation";
            case "emojis":
                return "Emojis";
            case "cooldowns":
                return "Cooldowns";
            case "mentions":
                return "Mentions";
            case "discord":
                return "Discord";
            case "login":
                return "Login";
            case "vanish":
                return "Vanish";
            default:
                return moduleKey;
        }
    }


    private boolean mergeIgnoreData(File sourceFile, YamlConfiguration target, Result result, Set<String> migratedUsers) {
        if (!sourceFile.isFile()) return false;

        YamlConfiguration source = YamlConfiguration.loadConfiguration(sourceFile);
        boolean changed = false;

        for (String uuid : source.getKeys(false)) {
            List<String> chatList = source.getStringList(uuid + ".chat.single");
            List<String> msgList = source.getStringList(uuid + ".msg.single");

            target.set(uuid + ".chat.single", chatList);
            target.set(uuid + ".msg.single", msgList);
            target.set(uuid + ".chat.all", source.getBoolean(uuid + ".chat.all", false));
            target.set(uuid + ".msg.all", source.getBoolean(uuid + ".msg.all", false));
            changed = true;

            result.ignoreUsers++;
            result.ignoredEntries += chatList.size() + msgList.size();
            migratedUsers.add(uuid);
        }

        return changed;
    }

    private boolean mergeMuteData(File sourceFile, YamlConfiguration target, Result result, Set<String> migratedUsers) {
        if (!sourceFile.isFile()) return false;

        YamlConfiguration source = YamlConfiguration.loadConfiguration(sourceFile);
        boolean changed = false;

        for (String uuid : source.getKeys(false)) {
            target.set(uuid, source.get(uuid));
            changed = true;
            result.mutedUsers++;
            migratedUsers.add(uuid);
        }

        return changed;
    }

    private boolean mergeSimpleUsers(File sourceFile, YamlConfiguration target, Set<String> migratedUsers) {
        if (!sourceFile.isFile()) return false;

        YamlConfiguration source = YamlConfiguration.loadConfiguration(sourceFile);
        boolean changed = false;

        for (String uuid : source.getKeys(false)) {
            target.set(uuid, source.get(uuid));
            changed = true;
            migratedUsers.add(uuid);
        }

        return changed;
    }

    private void migrateSirJoinQuitConfig(File sourceFile, File targetFile, Result result) throws IOException {
        if (!sourceFile.isFile()) return;

        YamlConfiguration source = YamlConfiguration.loadConfiguration(sourceFile);
        YamlConfiguration target = targetFile.isFile()
                ? YamlConfiguration.loadConfiguration(targetFile)
                : new YamlConfiguration();

        Boolean disableJoin = source.getBoolean("default-messages.disable-join", false);
        Boolean disableQuit = source.getBoolean("default-messages.disable-quit", false);

        target.set("disable-vanilla-messages.join", disableJoin);
        target.set("disable-vanilla-messages.quit", disableQuit);
        target.set("cooldown.join", source.getInt("cooldown.join", 0));
        target.set("cooldown.between", source.getInt("cooldown.between", 0));
        target.set("cooldown.quit", source.getInt("cooldown.quit", 0));

        ensureParent(targetFile);
        target.save(targetFile);
        result.configs++;
    }

    private void migrateSirJoinQuitMessages(File sourceFile, File targetFile, Result result) throws IOException {
        if (!sourceFile.isFile()) return;

        YamlConfiguration source = YamlConfiguration.loadConfiguration(sourceFile);
        YamlConfiguration target = new YamlConfiguration();

        migrateJoinQuitGroup(source, target, "first-join", "first-join");
        migrateJoinQuitGroup(source, target, "join", "join");
        migrateJoinQuitGroup(source, target, "quit", "quit");

        ensureParent(targetFile);
        target.save(targetFile);
        result.configs++;
    }

    private void migrateJoinQuitGroup(YamlConfiguration source, YamlConfiguration target, String sourcePath, String targetPath) {
        ConfigurationSection group = source.getConfigurationSection(sourcePath);
        if (group == null) return;

        for (String key : group.getKeys(false)) {
            ConfigurationSection entry = group.getConfigurationSection(key);
            if (entry == null) continue;

            String base = targetPath + "." + key;

            copyString(entry, target, base + ".permission", "permission");
            copyInt(entry, target, base + ".priority", "priority");
            copyInt(entry, target, base + ".invulnerable", "invulnerable");

            if (entry.contains("public"))
                target.set(base + ".public", entry.get("public"));
            if (entry.contains("private"))
                target.set(base + ".private", entry.get("private"));
            if (entry.contains("commands"))
                target.set(base + ".commands", entry.get("commands"));

            if (entry.contains("sound")) {
                Object soundValue = entry.get("sound");
                if (soundValue instanceof ConfigurationSection) {
                    ConfigurationSection soundSection = (ConfigurationSection) soundValue;
                    if (soundSection.contains("type")) {
                        target.set(base + ".sound.enabled", soundSection.getBoolean("enabled", true));
                        target.set(base + ".sound.type", soundSection.getString("type"));
                        target.set(base + ".sound.volume", soundSection.getDouble("volume", 1.0));
                        target.set(base + ".sound.pitch", soundSection.getDouble("pitch", 1.0));
                    }
                } else if (soundValue instanceof String) {
                    target.set(base + ".sound.enabled", true);
                    target.set(base + ".sound.type", soundValue);
                    target.set(base + ".sound.volume", 1.0);
                    target.set(base + ".sound.pitch", 1.0);
                }
            }

            ConfigurationSection spawn = entry.getConfigurationSection("spawn");
            if (spawn == null) spawn = entry.getConfigurationSection("spawn-location");

            if (spawn != null) {
                target.set(base + ".spawn.enabled", spawn.getBoolean("enabled", false));
                copyString(spawn, target, base + ".spawn.world", "world");
                copyString(spawn, target, base + ".spawn.coordinates", "coordinates");
                copyString(spawn, target, base + ".spawn.rotation", "rotation");
            }
        }
    }

    private void migrateSirAnnouncements(File sourceFile, File targetFile, Result result) throws IOException {
        if (!sourceFile.isFile()) return;

        YamlConfiguration source = YamlConfiguration.loadConfiguration(sourceFile);
        YamlConfiguration target = new YamlConfiguration();

        ConfigurationSection section = source.getConfigurationSection("announces");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) continue;

            String base = "announcements." + key;
            if (entry.contains("sound")) {
                Object sound = entry.get("sound");
                if (sound instanceof String) {
                    target.set(base + ".sound.enabled", true);
                    target.set(base + ".sound.type", sound);
                    target.set(base + ".sound.volume", 1.0);
                    target.set(base + ".sound.pitch", 1.0);
                } else if (sound instanceof ConfigurationSection) {
                    ConfigurationSection soundSection = (ConfigurationSection) sound;
                    target.set(base + ".sound.enabled", soundSection.getBoolean("enabled", true));
                    target.set(base + ".sound.type", soundSection.getString("type"));
                    target.set(base + ".sound.volume", soundSection.getDouble("volume", 1.0));
                    target.set(base + ".sound.pitch", soundSection.getDouble("pitch", 1.0));
                }
            }

            if (entry.contains("lines"))
                target.set(base + ".lines", entry.get("lines"));
            if (entry.contains("commands"))
                target.set(base + ".commands", entry.get("commands"));
        }

        ensureParent(targetFile);
        target.save(targetFile);
        result.configs++;
    }

    private void migrateSirAdvancements(File sourceFile, File targetFile, Result result) throws IOException {
        if (!sourceFile.isFile()) return;

        YamlConfiguration source = YamlConfiguration.loadConfiguration(sourceFile);
        YamlConfiguration target = targetFile.isFile()
                ? YamlConfiguration.loadConfiguration(targetFile)
                : new YamlConfiguration();

        target.set("worlds.enabled", true);
        target.set("worlds.whitelist", false);
        target.set("worlds.list", source.getStringList("disabled-worlds"));
        target.set("game-modes.enabled", true);
        target.set("game-modes.whitelist", false);
        target.set("game-modes.list", source.getStringList("disabled-modes"));
        target.set("advancements.enabled", true);
        target.set("advancements.whitelist", false);
        target.set("advancements.list", source.getStringList("disabled-advs"));

        ensureParent(targetFile);
        target.save(targetFile);
        result.configs++;
    }

    private void migrateSirMotd(File sourceFile, File targetFile, Result result) throws IOException {
        if (!sourceFile.isFile()) return;

        YamlConfiguration source = YamlConfiguration.loadConfiguration(sourceFile);
        YamlConfiguration target = targetFile.isFile()
                ? YamlConfiguration.loadConfiguration(targetFile)
                : new YamlConfiguration();

        target.set("max-players.type", source.getString("max-players.type", "DEFAULT"));
        target.set("max-players.count", source.getInt("max-players.count", 0));
        target.set("server-icon.usage", source.getString("server-icon.usage", "SINGLE"));
        target.set("server-icon.image", source.getString("server-icon.image", "server-icon.png"));
        target.set("random-motd", source.getBoolean("random-motds", false));

        ensureParent(targetFile);
        target.save(targetFile);
        result.configs++;
    }

    private boolean copyIfPresent(File sourceFile, File targetFile, Result result) throws IOException {
        if (!sourceFile.isFile()) return false;
        if (isSameFile(sourceFile, targetFile)) return false;

        ensureParent(targetFile);
        Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        result.configs++;
        return true;
    }

    private void ensureParent(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent == null) return;

        if (!parent.exists() && !parent.mkdirs())
            throw new IOException("Could not create folder " + parent.getPath());
    }

    private void copyString(ConfigurationSection source, YamlConfiguration target, String targetPath, String key) {
        String value = source.getString(key);
        if (value != null) target.set(targetPath, value);
    }

    private void copyInt(ConfigurationSection source, YamlConfiguration target, String targetPath, String key) {
        if (source.contains(key)) target.set(targetPath, source.getInt(key));
    }

    private boolean isLegacySirFolder(File sirFolder) {
        return new File(sirFolder, "commands" + File.separator + "ignore" + File.separator + "data.yml").isFile()
                || new File(sirFolder, "commands" + File.separator + "mute" + File.separator + "data.yml").isFile()
                || new File(sirFolder, "commands" + File.separator + "chat_view" + File.separator + "data.yml").isFile()
                || new File(sirFolder, "commands" + File.separator + "chat_color" + File.separator + "data.yml").isFile()
                || new File(sirFolder, "commands" + File.separator + "commands.yml").isFile()
                || new File(sirFolder, "modules" + File.separator + "modules.yml").isFile()
                || new File(sirFolder, "modules" + File.separator + "chat").isDirectory()
                || new File(sirFolder, "modules" + File.separator + "hook").isDirectory()
                || new File(sirFolder, "modules" + File.separator + "join_quit").isDirectory();
    }

    private void migrateLegacySirDiscordConfig(File sourceFile, File targetFile, Result result) throws IOException {
        if (!sourceFile.isFile()) return;

        YamlConfiguration source = YamlConfiguration.loadConfiguration(sourceFile);
        YamlConfiguration target = targetFile.isFile()
                ? YamlConfiguration.loadConfiguration(targetFile)
                : new YamlConfiguration();

        boolean changed = false;

        if (source.contains("default-server")) {
            target.set("default-server", source.getString("default-server"));
            changed = true;
        }

        ConfigurationSection ids = source.getConfigurationSection("ids");
        if (ids != null) {
            changed |= migrateLegacySirDiscordIds(ids, target, "first-join", "first-join");
            changed |= migrateLegacySirDiscordIds(ids, target, "join", "join");
            changed |= migrateLegacySirDiscordIds(ids, target, "quit", "quit");
            changed |= migrateLegacySirDiscordIds(ids, target, "global-chat", "global-chat");
            changed |= migrateLegacySirDiscordIds(ids, target, "advances", "advancements");
        }

        ConfigurationSection channels = source.getConfigurationSection("channels");
        if (channels != null) {
            changed |= migrateLegacySirDiscordMessage(channels.getConfigurationSection("first-join"), target, "first-join");
            changed |= migrateLegacySirDiscordMessage(channels.getConfigurationSection("join"), target, "join");
            changed |= migrateLegacySirDiscordMessage(channels.getConfigurationSection("quit"), target, "quit");
            changed |= migrateLegacySirDiscordMessage(channels.getConfigurationSection("global-chat"), target, "global-chat");
            changed |= migrateLegacySirDiscordMessage(channels.getConfigurationSection("advances"), target, "advancements");
        }

        if (!changed) return;

        ensureParent(targetFile);
        target.save(targetFile);
        result.configs++;
    }

    private boolean migrateLegacySirDiscordIds(ConfigurationSection ids, YamlConfiguration target, String sourceKey, String targetKey) {
        List<String> values = ids.getStringList(sourceKey);
        if (values.isEmpty()) return false;

        target.set("channel-ids." + targetKey, values);
        return true;
    }

    private boolean migrateLegacySirDiscordMessage(ConfigurationSection source, YamlConfiguration target, String targetKey) {
        if (source == null) return false;

        String base = "messages." + targetKey;
        boolean changed = false;

        if (source.contains("text")) {
            target.set(base + ".text", source.getString("text", ""));
            changed = true;
        }

        target.set(base + ".player-webhook", source.getBoolean("player-webhook", false));
        changed = true;

        ConfigurationSection embed = source.getConfigurationSection("embed");
        if (embed == null) return changed;

        target.set(base + ".embed.enabled",
                hasLegacySirDiscordEmbedContent(embed) || !hasText(source.getString("text")));
        if (embed.contains("color"))
            target.set(base + ".embed.color", embed.get("color"));

        ConfigurationSection author = embed.getConfigurationSection("author");
        if (author != null) {
            target.set(base + ".embed.author.name", author.getString("name", ""));
            target.set(base + ".embed.author.url", author.getString("url"));
            target.set(base + ".embed.author.icon-url",
                    author.getString("icon-url", author.getString("iconURL")));
        }

        if (embed.contains("thumbnail"))
            target.set(base + ".embed.thumbnail", embed.get("thumbnail"));
        if (embed.contains("description"))
            target.set(base + ".embed.description", embed.get("description"));

        Object title = embed.get("title");
        if (title instanceof ConfigurationSection) {
            ConfigurationSection titleSection = (ConfigurationSection) title;
            target.set(base + ".embed.title.text", titleSection.getString("text"));
            target.set(base + ".embed.title.url", titleSection.getString("url"));
        } else if (title != null) {
            target.set(base + ".embed.title.text", String.valueOf(title));
        }

        target.set(base + ".embed.timestamp",
                embed.getBoolean("timestamp", embed.getBoolean("timeStamp", false)));
        return true;
    }

    private boolean hasLegacySirDiscordEmbedContent(ConfigurationSection embed) {
        if (embed == null) return false;
        if (hasText(embed.getString("description")) || hasText(embed.getString("thumbnail")))
            return true;

        Object title = embed.get("title");
        if (title instanceof ConfigurationSection) {
            ConfigurationSection titleSection = (ConfigurationSection) title;
            if (hasText(titleSection.getString("text")) || hasText(titleSection.getString("url")))
                return true;
        } else if (title instanceof String && hasText((String) title)) {
            return true;
        }

        ConfigurationSection author = embed.getConfigurationSection("author");
        return author != null && (hasText(author.getString("name"))
                || hasText(author.getString("url"))
                || hasText(author.getString("iconURL"))
                || hasText(author.getString("icon-url")));
    }

    private void migrateLegacySirLoginConfig(File sourceFile, File targetFile, Result result) throws IOException {
        if (!sourceFile.isFile()) return;

        YamlConfiguration source = YamlConfiguration.loadConfiguration(sourceFile);
        if (!source.contains("spawn-before")) return;

        YamlConfiguration target = targetFile.isFile()
                ? YamlConfiguration.loadConfiguration(targetFile)
                : new YamlConfiguration();

        target.set("spawn-before-login", source.getBoolean("spawn-before", true));

        ensureParent(targetFile);
        target.save(targetFile);
        result.configs++;
    }

    private void migrateLegacySirVanishConfig(File sourceFile, File targetFile, Result result) throws IOException {
        if (!sourceFile.isFile()) return;

        YamlConfiguration source = YamlConfiguration.loadConfiguration(sourceFile);
        ConfigurationSection chatKey = source.getConfigurationSection("chat-key");
        if (chatKey == null) return;

        YamlConfiguration target = targetFile.isFile()
                ? YamlConfiguration.loadConfiguration(targetFile)
                : new YamlConfiguration();

        target.set("vanish-chat.enabled", chatKey.getBoolean("enabled", false));
        target.set("vanish-chat.key", chatKey.getString("key", "?"));
        target.set("vanish-chat.regex", chatKey.getBoolean("regex", false));
        target.set("vanish-chat.prefix",
                !"SUFFIX".equalsIgnoreCase(chatKey.getString("place", "PREFIX")));
        target.set("vanish-chat.not-allowed-messages", chatKey.getStringList("not-allowed"));

        ensureParent(targetFile);
        target.save(targetFile);
        result.configs++;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String resolveEssentialsChatFormat(ConfigurationSection chat) {
        String format = chat.getString("format");
        if (format != null) return format;

        ConfigurationSection formatSection = chat.getConfigurationSection("format");
        return formatSection == null ? null : formatSection.getString("normal");
    }

    private String resolveEssentialsNick(YamlConfiguration config) {
        return resolveFirstString(config, "nickname", "nick");
    }

    private Properties loadEssentialsMessages(File essentialsFolder, String locale) throws IOException {
        for (String resourceName : resolveLangCandidates(locale)) {
            Properties properties = new Properties();
            try (InputStream in = resolveEssentialsLangStream(essentialsFolder, resourceName)) {
                if (in == null) continue;

                properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                return properties;
            }
        }

        return null;
    }

    private List<String> resolveLangCandidates(String locale) {
        List<String> names = new ArrayList<>();
        names.add("messages_" + locale + ".properties");

        if (!"en".equalsIgnoreCase(locale))
            names.add("messages_en.properties");

        names.add("messages.properties");
        return names;
    }

    private InputStream resolveEssentialsLangStream(File essentialsFolder, String resourceName) throws IOException {
        Plugin pluginRef = plugin.getServer().getPluginManager().getPlugin("Essentials");
        if (pluginRef == null)
            pluginRef = plugin.getServer().getPluginManager().getPlugin("EssentialsX");

        if (pluginRef != null) {
            InputStream resource = pluginRef.getResource(resourceName);
            if (resource != null) return resource;
        }

        File jarFile = findEssentialsJar(essentialsFolder);
        if (jarFile == null) return null;

        return readJarEntry(jarFile, resourceName);
    }

    private File findEssentialsJar(File essentialsFolder) {
        File pluginsFolder = essentialsFolder.getParentFile();
        if (pluginsFolder == null || !pluginsFolder.isDirectory()) return null;

        File[] files = pluginsFolder.listFiles((dir, name) -> name.endsWith(".jar")
                && (name.startsWith("Essentials") || name.startsWith("EssentialsX")));
        if (files == null || files.length == 0) return null;

        File fallback = null;
        for (File file : files) {
            if (fallback == null) fallback = file;
            if (isPrimaryEssentialsJar(file)) return file;
        }

        return fallback;
    }

    private boolean isPrimaryEssentialsJar(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry entry = jar.getJarEntry("plugin.yml");
            if (entry == null) return false;

            try (InputStream in = jar.getInputStream(entry);
                 InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                YamlConfiguration pluginConfig = YamlConfiguration.loadConfiguration(reader);
                String name = pluginConfig.getString("name");
                return "Essentials".equalsIgnoreCase(name) || "EssentialsX".equalsIgnoreCase(name);
            }
        } catch (Exception ignored) {
            return false;
        }
    }

    private InputStream readJarEntry(File jarFile, String resourceName) throws IOException {
        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry entry = jar.getJarEntry(resourceName);
            if (entry == null) return null;

            try (InputStream entryStream = jar.getInputStream(entry);
                 ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                byte[] data = new byte[1024];
                int read;
                while ((read = entryStream.read(data)) != -1)
                    buffer.write(data, 0, read);

                return new ByteArrayInputStream(buffer.toByteArray());
            }
        }
    }

    private String resolveProperty(Properties properties, String... keys) {
        for (String key : keys) {
            String value = properties.getProperty(key);
            if (value != null && !value.trim().isEmpty()) return value;
        }
        return null;
    }

    private Map<String, String> placeholders(String... values) {
        if (values.length % 2 != 0)
            throw new IllegalArgumentException("Placeholders must be declared in key/value pairs.");

        Map<String, String> placeholders = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2)
            placeholders.put(values[i], values[i + 1]);

        return placeholders;
    }

    private String backupLegacySirArtifacts(File sirFolder) throws IOException {
        File backupsFolder = new File(plugin.getDataFolder(), "back-ups");
        if (!backupsFolder.exists() && !backupsFolder.mkdirs())
            throw new IOException("Could not create backups folder.");

        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        File target = new File(backupsFolder, "SIR-legacy-" + timestamp);

        boolean moved = false;
        moved |= moveLegacySirArtifact(sirFolder, target, "commands" + File.separator + "ignore");
        moved |= moveLegacySirArtifact(sirFolder, target, "commands" + File.separator + "mute");
        moved |= moveLegacySirArtifact(sirFolder, target, "commands" + File.separator + "chat_view");
        moved |= moveLegacySirArtifact(sirFolder, target, "commands" + File.separator + "chat_color");
        moved |= moveLegacySirArtifact(sirFolder, target, "commands" + File.separator + "commands.yml");
        moved |= moveLegacySirArtifact(sirFolder, target, "modules" + File.separator + "announcements" + File.separator + "announces.yml");
        moved |= moveLegacySirArtifact(sirFolder, target, "modules" + File.separator + "join_quit");
        moved |= moveLegacySirArtifact(sirFolder, target, "modules" + File.separator + "chat");
        moved |= moveLegacySirArtifact(sirFolder, target, "modules" + File.separator + "hook");
        moved |= moveLegacySirArtifact(sirFolder, target, "modules" + File.separator + "modules.yml");

        return moved ? target.getPath() : null;
    }

    private boolean moveLegacySirArtifact(File root, File backupRoot, String relativePath) throws IOException {
        File source = new File(root, relativePath);
        if (!source.exists()) return false;

        File target = new File(backupRoot, relativePath);
        ensureParent(target);
        Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    private boolean updateDiscordChannel(YamlConfiguration target, String key, String channelId) {
        if (channelId == null) return false;

        String path = "channel-ids." + key;
        target.set(path, Collections.singletonList(channelId));
        return true;
    }

    private boolean updateDiscordMessage(YamlConfiguration target, String path, String value) {
        if (value == null)
            return false;

        target.set(path, translateEssentialsText(value));
        return true;
    }

    private String resolveDiscordChannelId(ConfigurationSection channels, ConfigurationSection messageTypes, String messageKey) {
        if (messageTypes == null) return null;

        String raw = messageTypes.getString(messageKey);
        if (raw == null || raw.equalsIgnoreCase("none")) return null;

        if (raw.matches("\\d+")) return raw;
        if (channels == null) return null;

        String channelId = channels.getString(raw);
        if (channelId != null && channelId.matches("\\d+")) return channelId;

        return null;
    }

    private Double getDouble(ConfigurationSection section, String key) {
        return section.contains(key) ? section.getDouble(key) : null;
    }

    private String formatCoordinates(double x, double y, double z) {
        return stripTrailingZeros(x) + "," + stripTrailingZeros(y) + "," + stripTrailingZeros(z);
    }

    private String formatRotation(double yaw, double pitch) {
        return stripTrailingZeros(yaw) + "," + stripTrailingZeros(pitch);
    }

    private String stripTrailingZeros(double value) {
        return value == (long) value ? String.valueOf((long) value) : String.valueOf(value);
    }

    private boolean isSomething(String value) {
        return value != null && !value.trim().isEmpty() && !value.equalsIgnoreCase("none");
    }

    private String translateEssentialsText(String value) {
        return value == null ? null : value
                .replaceAll("(?i)\\{username}", "{player}")
                .replaceAll("(?i)\\{player}", "{player}")
                .replaceAll("(?i)\\{displayname}", "{player}")
                .replaceAll("(?i)\\{message}", "{message}")
                .replaceAll("(?i)\\{world}", "{world}")
                .replaceAll("(?i)\\{online}", "{online}")
                .replaceAll("(?i)\\{unique}", "{unique}");
    }

    private String translateEssentialsLangMessage(String value, Map<String, String> placeholders) {
        if (value == null) return null;

        String resolved = translateEssentialsText(value);
        if (placeholders != null)
            for (Map.Entry<String, String> entry : placeholders.entrySet())
                resolved = resolved.replace(entry.getKey(), entry.getValue());

        resolved = translateEssentialsFormatting(resolved);
        return resolved.contains("<P>") ? resolved : "<P> " + resolved;
    }

    private String translateEssentialsInlineValue(String value, Map<String, String> placeholders) {
        if (value == null) return null;

        String resolved = translateEssentialsText(value);
        if (placeholders != null)
            for (Map.Entry<String, String> entry : placeholders.entrySet())
                resolved = resolved.replace(entry.getKey(), entry.getValue());

        return translateEssentialsFormatting(resolved);
    }

    private String translateEssentialsFormatting(String value) {
        if (value == null) return null;

        String formatted = value
                .replace("<dark_red>", "&4")
                .replace("<red>", "&c")
                .replace("<gold>", "&6")
                .replace("<yellow>", "&e")
                .replace("<dark_green>", "&2")
                .replace("<green>", "&a")
                .replace("<aqua>", "&b")
                .replace("<dark_aqua>", "&3")
                .replace("<dark_blue>", "&1")
                .replace("<blue>", "&9")
                .replace("<light_purple>", "&d")
                .replace("<dark_purple>", "&5")
                .replace("<white>", "&f")
                .replace("<gray>", "&7")
                .replace("<dark_gray>", "&8")
                .replace("<black>", "&0")
                .replace("<reset>", "&r")
                .replace("<bold>", "&l")
                .replace("<italic>", "&o")
                .replace("<underlined>", "&n")
                .replace("<strikethrough>", "&m")
                .replace("<obfuscated>", "&k")
                .replace("<primary>", "&e")
                .replace("<secondary>", "&6")
                .replace("<tertiary>", "&7");

        formatted = formatted.replaceAll("<#[0-9A-Fa-f]{6}>", "");
        return formatted.replaceAll("<[^>]+>", "");
    }

    private String translateEssentialsChatFormat(String value) {
        return value == null ? null : value
                .replaceAll("(?i)\\{message}", "{message}")
                .replaceAll("(?i)\\{username}", "{player}")
                .replaceAll("(?i)\\{displayname}", "{player}")
                .replaceAll("(?i)\\{nickname}", "{player}")
                .replaceAll("(?i)\\{prefix}", "{prefix}")
                .replaceAll("(?i)\\{suffix}", "{suffix}")
                .replaceAll("(?i)\\{group}", "{group}")
                .replaceAll("(?i)\\{worldname}", "{world}")
                .replaceAll("(?i)\\{world}", "{world}")
                .replaceAll("(?i)\\{shortworldname}", "{world}")
                .replaceAll("(?i)\\{teamname}", "{team}")
                .replaceAll("(?i)\\{teamprefix}", "{team}")
                .replaceAll("(?i)\\{teamsuffix}", "{team}");
    }

    private File findPluginFolder(String... names) {
        for (String name : names) {
            Plugin pluginRef = plugin.getServer().getPluginManager().getPlugin(name);
            if (pluginRef != null) return pluginRef.getDataFolder();
        }

        File pluginsFolder = plugin.getDataFolder().getParentFile();
        if (pluginsFolder == null) return null;

        for (String name : names) {
            File candidate = new File(pluginsFolder, name);
            if (candidate.exists()) return candidate;
        }

        return null;
    }

    private String resolveExpectedPath(String pluginName, String child) {
        File pluginsFolder = plugin.getDataFolder().getParentFile();
        File expected = pluginsFolder == null ? new File("plugins", pluginName) : new File(pluginsFolder, pluginName);
        return child == null ? expected.getPath() : new File(expected, child).getPath();
    }

    private boolean isSameFile(File first, File second) throws IOException {
        return first != null && second != null
                && first.getCanonicalFile().equals(second.getCanonicalFile());
    }

    private String backupPluginFolderSnapshot(File source, String name) throws IOException {
        if (source == null || !source.exists()) return null;

        File target = createBackupTarget(name);
        copyFolder(source.toPath(), target.toPath());
        return target.getPath();
    }

    private String backupPluginFolder(File source, String name) throws IOException {
        if (source == null || !source.exists()) return null;

        File target = createBackupTarget(name);
        Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return target.getPath();
    }

    private File createBackupTarget(String name) throws IOException {
        File backupsFolder = new File(plugin.getDataFolder(), "back-ups");
        if (!backupsFolder.exists() && !backupsFolder.mkdirs())
            throw new IOException("Could not create backups folder.");

        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        return new File(backupsFolder, name + "-" + timestamp);
    }

    private void copyFolder(Path source, Path target) throws IOException {
        try (Stream<Path> paths = Files.walk(source)) {
            Iterator<Path> iterator = paths.iterator();
            while (iterator.hasNext()) {
                Path current = iterator.next();
                Path destination = target.resolve(source.relativize(current));

                if (Files.isDirectory(current)) {
                    Files.createDirectories(destination);
                    continue;
                }

                Path parent = destination.getParent();
                if (parent != null) Files.createDirectories(parent);
                Files.copy(current, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private UUID resolveUuid(File file, YamlConfiguration config) {
        String name = file.getName();
        if (name.endsWith(".yml")) name = name.substring(0, name.length() - 4);

        UUID uuid = parseUuid(name);
        if (uuid != null) return uuid;

        String stored = config.getString("uuid");
        uuid = parseUuid(stored);
        if (uuid != null) return uuid;

        String username = config.getString("lastAccountName");
        return username != null ?
                Bukkit.getOfflinePlayer(username).getUniqueId() :
                null;
    }

    private UUID parseUuid(String value) {
        if (value == null) return null;
        try {
            return UUID.fromString(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Set<String> parseUuids(Collection<String> values) {
        Set<String> results = new LinkedHashSet<>();
        for (String value : values) {
            UUID uuid = parseUuid(value);
            if (uuid != null)
                results.add(uuid.toString());
        }
        return results;
    }

    private long resolveMuteExpiry(YamlConfiguration config) {
        Object value = resolveFirst(config, "muteTimeout", "muted-until", "mutedUntil", "muted-until-ms");
        if (value instanceof Number) {
            long raw = ((Number) value).longValue();
            return normalizeTime(raw);
        }

        if (value instanceof String)
            try {
                return normalizeTime(Long.parseLong((String) value));
            } catch (Exception ignored) {
                return -1L;
            }

        return -1L;
    }

    private long normalizeTime(long value) {
        return value > 0 ? (value < SECONDS_THRESHOLD ? value * 1000L : value) : -1L;
    }

    private Object resolveFirst(YamlConfiguration config, String... keys) {
        for (String key : keys)
            if (config.contains(key)) return config.get(key);

        return null;
    }

    private String resolveFirstString(YamlConfiguration config, String... keys) {
        for (String key : keys) {
            String value = config.getString(key);
            if (value != null && !value.isEmpty())
                return value;
        }
        return null;
    }

    @Getter
    static final class Result {
        private boolean ok = false;
        private String path;
        private String backupPath;
        private final List<String> extraBackups = new ArrayList<>();
        private int users = 0;
        private int ignoreUsers = 0;
        private int ignoredEntries = 0;
        private int mutedUsers = 0;
        private int nickUsers = 0;
        private final int skipped = 0;
        private int expiredMutes = 0;
        private int invalidUsers = 0;
        private int configs = 0;
        private int moduleStates = 0;
        private int commandStates = 0;
    }
}
