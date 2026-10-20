package com.bitaspire.sir.module.custom;

import com.bitaspire.sir.ChatToggleable;
import com.bitaspire.sir.chat.ChatProcessor;
import com.bitaspire.sir.command.CommandProvider;
import com.bitaspire.sir.command.SIRCommand;
import com.bitaspire.sir.file.ExtensionFile;
import com.bitaspire.sir.module.SIRModule;
import lombok.Getter;
import lombok.SneakyThrows;
import me.croabeast.takion.logger.LogLevel;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

public final class CustomCommands extends SIRModule implements CommandProvider, ChatProcessor, ChatToggleable {

    private static final List<String> DEFAULTS = Arrays.asList("example", "clear-chat", "print", "dice", "ball", "anon", "coin", "me", "broadcast", "try", "ping", "brand", "daily");

    @Getter
    private final Set<SIRCommand> commands = new HashSet<>();
    final Map<String, CustomCommand> loaded = new TreeMap<>();

    ExtensionFile config, lang, menuFile;
    final CooldownStore cooldowns = new CooldownStore(this);
    final DataStore data = new DataStore(this);
    // "<sender> /<command> <args>" to the time its confirmation ends; kept in memory only.
    final Map<String, Long> confirms = new HashMap<>();
    private final Joiner joiner = new Joiner(this);
    private Expansion expansion;
    private final Hider hider = new Hider(this);
    final Menu menu = new Menu(this);
    final ChatInput chat = new ChatInput(this);

    private File getFolder() {
        return new File(getDataFolder(), "commands");
    }

    private File[] getFiles() {
        File[] files = getFolder().listFiles((dir, name) -> name.endsWith(".yml"));
        return files == null ? new File[0] : files;
    }

    private static String toName(File file) {
        String name = file.getName();
        return name.substring(0, name.length() - 4).toLowerCase(Locale.ENGLISH);
    }

    List<String> getNames() {
        List<String> names = new ArrayList<>();
        for (File file : getFiles()) names.add(toName(file));
        Collections.sort(names);
        return names;
    }

    @SneakyThrows
    public boolean register() {
        config = new ExtensionFile(this, "config", true);
        lang = new ExtensionFile(this, "lang", true);
        menuFile = new ExtensionFile(this, "menu", true);
        cooldowns.load();
        data.load();

        commands.clear();
        commands.add(new AdminCommand(this));

        installDefaults();
        load();
        chat.register();
        joiner.register();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            expansion = new Expansion(this);
            expansion.register();
        }

        try {
            Class.forName("org.bukkit.event.player.PlayerCommandSendEvent");
            hider.register();
        } catch (ClassNotFoundException ignored) {}
        return true;
    }

    // Bundled commands are copied once: a deleted one stays deleted. defaults.yml keeps track of them.
    @SneakyThrows
    private void installDefaults() {
        boolean fresh = !getFolder().exists();
        File file = new File(getDataFolder(), "defaults.yml");

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        List<String> installed = yml.getStringList("installed");

        for (String name : DEFAULTS) {
            if (installed.contains(name)) continue;
            if (fresh || !name.equals("example")) saveResource("commands/" + name + ".yml", false);
            installed.add(name);
        }

        // Clear Chat and Print used to be command providers; their settings move into their files once.
        List<String> migrated = yml.getStringList("migrated");
        new LegacyProviders(this).migrate(migrated);

        yml.set("installed", installed);
        yml.set("migrated", migrated);
        yml.save(file);
    }

    @Override
    public boolean unregister() {
        unload();
        getApi().getCommandManager().getSynchronizer().sync();
        cooldowns.save();
        hider.unregister();
        chat.unregister();
        joiner.unregister();

        if (expansion != null) {
            expansion.unregister();
            expansion = null;
        }
        return true;
    }

    @Override
    public int getPriority() {
        return -10000;
    }

    @Override
    public void process(@NotNull ChatProcessor.Context context) {
        if (chat.handle(context.getPlayer(), context.getMessage())) context.cancel();
    }

    private void unload() {
        loaded.values().forEach(command -> command.unregister(false));
        loaded.clear();
    }

    @Nullable
    private static CommandMap getCommandMap() {
        try {
            return (CommandMap) Bukkit.getServer().getClass().getMethod("getCommandMap").invoke(Bukkit.getServer());
        } catch (Exception e) {
            return null;
        }
    }

    private void load(File file) {
        String name = toName(file);
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        if (!yml.getBoolean("enabled", true)) return;

        if (getApi().getCommandManager().getCommand(name) != null) {
            getLogger().log(LogLevel.WARN, "Custom command '/" + name + "' skipped: SIR already has that command.");
            return;
        }

        CommandMap map = getCommandMap();
        if (!yml.getBoolean("override-existing") && map != null && map.getCommand(name) != null) {
            getLogger().log(LogLevel.WARN, "Custom command '/" + name + "' skipped: another plugin has that command. "
                    + "Set 'override-existing' to true to replace it.");
            return;
        }

        try {
            List<String> warnings = new ArrayList<>();
            CustomCommand command = new CustomCommand(this, name, yml, warnings);
            warnings.forEach(w -> getLogger().log(LogLevel.WARN, "Custom command '/" + name + "': " + w + "."));

            command.register(false);
            loaded.put(name, command);
        } catch (Exception e) {
            getLogger().log(LogLevel.ERROR, "Custom command '" + file.getName() + "' couldn't be loaded: " + e.getMessage());
        }
    }

    int load() {
        unload();
        for (File file : getFiles()) load(file);

        getApi().getCommandManager().getSynchronizer().sync();
        return loaded.size();
    }

    File getFile(String name) {
        for (File file : getFiles())
            if (toName(file).equals(name)) return file;
        return new File(getFolder(), name + ".yml");
    }

    YamlConfiguration read(String name) {
        return YamlConfiguration.loadConfiguration(getFile(name));
    }

    private void reload(String name) {
        CustomCommand old = loaded.remove(name);
        if (old != null) old.unregister(false);

        File file = getFile(name);
        if (file.isFile()) load(file);
        getApi().getCommandManager().getSynchronizer().sync();
    }

    @SneakyThrows
    private void save(String name, YamlConfiguration yml) {
        yml.save(getFile(name));
        reload(name);
    }

    void set(String name, String path, Object value) {
        YamlConfiguration yml = read(name);
        yml.set(path, value);
        save(name, yml);
    }

    private static void put(ConfigurationSection target, String key, Object value) {
        if (!(value instanceof ConfigurationSection)) {
            target.set(key, value);
            return;
        }

        ConfigurationSection source = (ConfigurationSection) value, created = target.createSection(key);
        for (String child : source.getKeys(false)) put(created, child, source.get(child));
    }

    // Rewrites the keys of a section in a new order and with new names, keeping their values.
    // The order matters: typed arguments are matched in the order they are written.
    void rebuild(String name, String path, List<String> from, List<String> to) {
        YamlConfiguration yml = read(name);
        ConfigurationSection section = yml.getConfigurationSection(path);
        if (section == null) return;

        Map<String, Object> values = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) values.put(key, section.get(key));
        for (String key : values.keySet()) section.set(key, null);

        for (int i = 0; i < from.size(); i++) put(section, to.get(i), values.get(from.get(i)));
        save(name, yml);
    }

    boolean toggle(String name) {
        boolean enabled = !read(name).getBoolean("enabled", true);
        set(name, "enabled", enabled);
        return enabled;
    }

    boolean isValidName(String name) {
        return name.matches("[a-z0-9_-]+") && !getNames().contains(name);
    }

    boolean create(String name) {
        if (!isValidName(name)) return false;
        set(name, "enabled", false);
        return true;
    }

    @SneakyThrows
    boolean duplicate(String name, String copy) {
        if (!isValidName(copy)) return false;

        // Disabled so its aliases can be changed before they clash with the original's.
        Files.copy(getFile(name).toPath(), getFile(copy).toPath());
        set(copy, "enabled", false);
        return true;
    }

    boolean rename(String name, String renamed) {
        if (!isValidName(renamed) || !getFile(name).renameTo(getFile(renamed))) return false;

        reload(name);
        reload(renamed);
        return true;
    }

    boolean delete(String name) {
        if (!getFile(name).delete()) return false;
        reload(name);
        return true;
    }
}
