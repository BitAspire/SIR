package com.bitaspire.sir.module.custom;

import lombok.RequiredArgsConstructor;
import me.croabeast.takion.logger.LogLevel;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

// Clear Chat and Print were command providers before they became commands/clear-chat.yml and print.yml.
// Once per provider, their enabled state, override-existing and the messages the admin edited are copied
// into those files, and the old folder is renamed to '<provider>-migrated'.
@RequiredArgsConstructor
final class LegacyProviders {

    private static final List<String> CLEAR_HELP = Arrays.asList(
            "&7",
            " &6>> &eClear Chat - Help",
            "  &7- &b/clear-chat help&7: Displays this help menu.",
            "  &7- &b/clear-chat @a&7: Clears the chat for all online players.",
            "  &7- &b/clear-chat <player>&7: Clears the chat for a specific player.",
            "&7");
    private static final String CLEAR_ALL = "<P> &7Chat has been cleared for all players.";
    private static final String CLEAR_PLAYER = "<P> &7Chat has been cleared for &e{player}&7.";

    private static final List<String> PRINT_HELP = Arrays.asList(
            "&7",
            " &6>> &ePrint - Help",
            "  &7- &b/print&7: Displays this help menu.",
            "  &7- &b/print targets&7: Explains target selectors.",
            "  &7- &b/print action-bar&7: Action bar usage.",
            "  &7- &b/print chat&7: Chat message usage.",
            "  &7- &b/print title&7: Title message usage.",
            "&7");
    private static final List<String> PRINT_TARGETS = Arrays.asList(
            "&7",
            " &6>> &ePrint - Targets",
            "  &7- &b<player>&7: Select a specific player.",
            "  &7- &b@a&7: Select all online players.",
            "  &7- &bworld:world&7: Select players in the specified world.",
            "  &7- &bperm:example.permission&7: Select players with a permission.",
            "  &7- &bgroup:member&7: Select players in a permission group (Vault required).",
            "&7");
    private static final String PRINT_SENT = "<P> &7Message sent successfully to {target}.";
    // The confirmation every way of /print ends with in print.yml.
    static final String SENT = "<P> &7Message sent to &e{target}&7.";

    private final CustomCommands main;

    // Adds the providers it migrates, or that need no migration, to 'done', so it runs once each.
    void migrate(List<String> done) {
        if (!done.contains("ClearProvider") && migrate("ClearProvider", "clear-chat")) done.add("ClearProvider");
        if (!done.contains("PrintProvider") && migrate("PrintProvider", "print")) done.add("PrintProvider");
    }

    private boolean migrate(String provider, String command) {
        File sir = main.getApi().getPlugin().getDataFolder();
        File folder = new File(sir, "commands" + File.separator + provider);

        YamlConfiguration states = YamlConfiguration.loadConfiguration(new File(sir, "commands" + File.separator + "states.yml"));
        ConfigurationSection state = states.getConfigurationSection("providers." + provider);

        // Never installed, or the new file was deleted on purpose: nothing to keep.
        File file = main.getFile(command);
        if ((!folder.isDirectory() && state == null) || !file.isFile()) return true;

        try {
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
            if (state != null) {
                if (!state.getBoolean("enabled", true)) yml.set("enabled", false);
                if (state.isSet("commands." + command)) yml.set("override-existing", state.getBoolean("commands." + command));
            }

            ConfigurationSection lang = YamlConfiguration.loadConfiguration(new File(folder, "lang.yml")).getConfigurationSection("lang");
            if (lang != null) {
                if (command.equals("print")) applyPrint(yml, lang);
                else applyClearChat(yml, lang);
            }

            yml.save(file);
            if (folder.isDirectory() && !folder.renameTo(new File(folder.getParentFile(), provider + "-migrated")))
                main.getLogger().log(LogLevel.WARN, "Couldn't rename " + folder.getPath() + " after migrating it.");

            main.getLogger().log(LogLevel.INFO, "Migrated the " + provider + " settings to commands/" + command + ".yml.");
            return true;
        } catch (Exception e) {
            main.getLogger().log(LogLevel.ERROR, "Couldn't migrate " + provider + ": " + e.getMessage());
            return false;
        }
    }

    private static boolean edited(ConfigurationSection lang, String path, Object old) {
        if (!lang.isSet(path)) return false;
        Object value = lang.isList(path) ? lang.getStringList(path) : lang.getString(path);
        return !Objects.equals(value, old);
    }

    // Only the messages changed from the old defaults are copied, so the new ones stay otherwise.
    static void applyClearChat(YamlConfiguration yml, ConfigurationSection lang) {
        if (edited(lang, "help", CLEAR_HELP)) {
            yml.set("messages", lang.getStringList("help"));
            yml.set("arguments.help.messages", lang.getStringList("help"));
        }

        if (edited(lang, "success.all", CLEAR_ALL))
            yml.set("arguments.@a.messages", Arrays.asList("[to:all][repeat:200]", lang.getString("success.all")));

        if (edited(lang, "success.player", CLEAR_PLAYER))
            yml.set("arguments.target.messages", Arrays.asList("[to:target][repeat:200]",
                    lang.getString("success.player", "").replace("{player}", "{target}")));
    }

    static void applyPrint(YamlConfiguration yml, ConfigurationSection lang) {
        if (edited(lang, "help.main", PRINT_HELP)) yml.set("messages", lang.getStringList("help.main"));
        if (edited(lang, "help.targets", PRINT_TARGETS)) yml.set("arguments.targets.messages", lang.getStringList("help.targets"));
        if (edited(lang, "reminder.success", PRINT_SENT)) replaceSent(yml, lang.getString("reminder.success", SENT));
    }

    // Every 'message' argument of print.yml is a copy once the file is loaded, so each one is changed.
    private static void replaceSent(ConfigurationSection section, String sent) {
        List<String> lines = section.getStringList("messages");
        if (lines.contains(SENT)) {
            lines.replaceAll(line -> line.equals(SENT) ? sent : line);
            section.set("messages", lines);
        }

        ConfigurationSection arguments = section.getConfigurationSection("arguments");
        if (arguments == null) return;

        for (String key : arguments.getKeys(false)) {
            ConfigurationSection child = arguments.getConfigurationSection(key);
            if (child != null) replaceSent(child, sent);
        }
    }
}
