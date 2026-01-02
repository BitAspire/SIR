package me.croabeast.sir;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;

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
        migrateEssentialsXCommandStates(essentialsFolder, result);

        File userdataFolder = new File(essentialsFolder, "userdata");
        if (!userdataFolder.isDirectory()) {
            result.backupPath = backupPluginFolder(essentialsFolder, essentialsFolder.getName());
            return result;
        }

        File usersFolder = new File(plugin.getDataFolder(), "users");
        if (!usersFolder.exists() && !usersFolder.mkdirs())
            throw new IOException("Could not create users folder.");

        File ignoreFile = new File(usersFolder, "ignore.yml");
        File muteFile = new File(usersFolder, "mute.yml");

        YamlConfiguration ignoreConfig = YamlConfiguration.loadConfiguration(ignoreFile);
        YamlConfiguration muteConfig = YamlConfiguration.loadConfiguration(muteFile);

        boolean ignoreChanged = false, muteChanged = false;

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

        result.backupPath = backupPluginFolder(essentialsFolder, essentialsFolder.getName());
        return result;
    }

    Result migrateSir() throws IOException {
        Result result = new Result();

        File sirFolder = findPluginFolder("SIR");
        if (sirFolder == null) {
            result.path = resolveExpectedPath("SIR", null);
            return result;
        }

        result.ok = true;
        result.path = sirFolder.getPath();

        migrateSirUsers(sirFolder, result);
        migrateSirSharedFiles(sirFolder, result);
        migrateSirModules(sirFolder, result);
        migrateSirModuleStates(sirFolder, result);
        migrateSirCommandStates(sirFolder, result);

        result.backupPath = backupPluginFolder(sirFolder, sirFolder.getName());
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

    private void migrateSirModules(File sirFolder, Result result) throws IOException {
        File modulesFolder = new File(sirFolder, "modules");
        if (!modulesFolder.isDirectory()) return;

        File targetModulesFolder = new File(plugin.getDataFolder(), "modules");
        if (!targetModulesFolder.exists() && !targetModulesFolder.mkdirs())
            throw new IOException("Could not create modules folder.");

        copyIfPresent(new File(modulesFolder, "announcements" + File.separator + "config.yml"),
                new File(targetModulesFolder, "announcements" + File.separator + "config.yml"),
                result);
        migrateSirAnnouncements(new File(modulesFolder, "announcements" + File.separator + "announces.yml"),
                new File(targetModulesFolder, "announcements" + File.separator + "announcements.yml"),
                result);

        migrateSirJoinQuitConfig(new File(modulesFolder, "join_quit" + File.separator + "config.yml"),
                new File(targetModulesFolder, "join-quit" + File.separator + "config.yml"),
                result);
        migrateSirJoinQuitMessages(new File(modulesFolder, "join_quit" + File.separator + "messages.yml"),
                new File(targetModulesFolder, "join-quit" + File.separator + "messages.yml"),
                result);

        migrateSirAdvancements(new File(modulesFolder, "advancements" + File.separator + "config.yml"),
                new File(targetModulesFolder, "advancements" + File.separator + "config.yml"),
                result);

        migrateSirMotd(new File(modulesFolder, "motd" + File.separator + "config.yml"),
                new File(targetModulesFolder, "motd" + File.separator + "config.yml"),
                result);

        copyIfPresent(new File(modulesFolder, "chat" + File.separator + "channels.yml"),
                new File(targetModulesFolder, "channels" + File.separator + "channels.yml"),
                result);
        copyIfPresent(new File(modulesFolder, "chat" + File.separator + "config.yml"),
                new File(targetModulesFolder, "channels" + File.separator + "config.yml"),
                result);
        copyIfPresent(new File(modulesFolder, "chat" + File.separator + "tags.yml"),
                new File(targetModulesFolder, "tags" + File.separator + "tags.yml"),
                result);
        copyIfPresent(new File(modulesFolder, "chat" + File.separator + "moderation.yml"),
                new File(targetModulesFolder, "moderation" + File.separator + "moderation.yml"),
                result);
        copyIfPresent(new File(modulesFolder, "chat" + File.separator + "emojis.yml"),
                new File(targetModulesFolder, "emojis" + File.separator + "emojis.yml"),
                result);
        copyIfPresent(new File(modulesFolder, "chat" + File.separator + "cooldowns.yml"),
                new File(targetModulesFolder, "cooldowns" + File.separator + "cooldowns.yml"),
                result);
        copyIfPresent(new File(modulesFolder, "chat" + File.separator + "mentions.yml"),
                new File(targetModulesFolder, "mentions" + File.separator + "mentions.yml"),
                result);

        copyIfPresent(new File(modulesFolder, "hook" + File.separator + "discord.yml"),
                new File(targetModulesFolder, "discord" + File.separator + "config.yml"),
                result);
        copyIfPresent(new File(modulesFolder, "hook" + File.separator + "login.yml"),
                new File(targetModulesFolder, "login" + File.separator + "config.yml"),
                result);
        copyIfPresent(new File(modulesFolder, "hook" + File.separator + "vanish.yml"),
                new File(targetModulesFolder, "vanish" + File.separator + "config.yml"),
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

        String format = chat.getString("format");
        int radius = chat.getInt("radius", 0);
        if (format == null && radius == 0) return;

        File channelsFile = new File(plugin.getDataFolder(),
                "modules" + File.separator + "channels" + File.separator + "channels.yml");
        YamlConfiguration target = channelsFile.isFile()
                ? YamlConfiguration.loadConfiguration(channelsFile)
                : new YamlConfiguration();

        if (format != null)
            target.set("default-channel.format", translateEssentialsChatFormat(format));

        target.set("default-channel.radius", radius);

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

        File messagesFile = new File(plugin.getDataFolder(),
                "modules" + File.separator + "join-quit" + File.separator + "messages.yml");
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

        File targetFile = new File(plugin.getDataFolder(),
                "modules" + File.separator + "discord" + File.separator + "config.yml");
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

        String backup = backupPluginFolder(discordFolder, discordFolder.getName());
        if (backup != null) result.extraBackups.add(backup);
    }

    private void migrateEssentialsJoinQuit(YamlConfiguration config, Result result) throws IOException {
        String joinMessage = config.getString("custom-join-message", "none");
        String quitMessage = config.getString("custom-quit-message", "none");

        boolean hasJoinMessage = isSomething(joinMessage);
        boolean hasQuitMessage = isSomething(quitMessage);
        if (!hasJoinMessage && !hasQuitMessage) return;

        File joinQuitFolder = new File(plugin.getDataFolder(), "modules" + File.separator + "join-quit");
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

        File targetFile = new File(plugin.getDataFolder(), "modules" + File.separator + "motd" + File.separator + "motd.yml");
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
            case "tell":
            case "message":
                return "message";
            case "r":
            case "reply":
                return "reply";
            case "ignore":
                return "ignore";
            case "mute":
                return "mute";
            case "tempmute":
                return "tempmute";
            case "unmute":
                return "unmute";
            case "clearchat":
            case "clear":
                return "clear-chat";
            case "color":
            case "chatcolor":
                return "chat-color";
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

    private void copyIfPresent(File sourceFile, File targetFile, Result result) throws IOException {
        if (!sourceFile.isFile()) return;

        ensureParent(targetFile);
        Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        result.configs++;
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

    private String backupPluginFolder(File source, String name) throws IOException {
        if (source == null || !source.exists()) return null;

        File backupsFolder = new File(plugin.getDataFolder(), "back-ups");
        if (!backupsFolder.exists() && !backupsFolder.mkdirs())
            throw new IOException("Could not create backups folder.");

        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        File target = new File(backupsFolder, name + "-" + timestamp);

        Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return target.getPath();
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
        private int expiredMutes = 0;
        private int invalidUsers = 0;
        private int configs = 0;
        private int moduleStates = 0;
        private int commandStates = 0;
    }
}
