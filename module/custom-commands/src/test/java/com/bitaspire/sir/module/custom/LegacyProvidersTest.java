package com.bitaspire.sir.module.custom;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LegacyProvidersTest {

    private YamlConfiguration bundled(String name) throws Exception {
        try (Reader reader = new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("commands/" + name + ".yml"), StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        }
    }

    private static YamlConfiguration lang(String... lines) throws Exception {
        YamlConfiguration yml = new YamlConfiguration();
        yml.loadFromString(String.join("\n", lines));
        return yml;
    }

    private static void collect(org.bukkit.configuration.ConfigurationSection section, List<String> into) {
        into.addAll(section.getStringList("messages"));
        org.bukkit.configuration.ConfigurationSection arguments = section.getConfigurationSection("arguments");
        if (arguments == null) return;
        for (String key : arguments.getKeys(false)) collect(arguments.getConfigurationSection(key), into);
    }

    @Test
    void untouchedMessagesKeepTheNewDefaults() throws Exception {
        YamlConfiguration print = bundled("print");
        List<String> before = print.getStringList("messages");

        LegacyProviders.applyPrint(print, lang(
                "help:",
                "  main: [ '&7', ' &6>> &ePrint - Help', '  &7- &b/print&7: Displays this help menu.',",
                "    '  &7- &b/print targets&7: Explains target selectors.', '  &7- &b/print action-bar&7: Action bar usage.',",
                "    '  &7- &b/print chat&7: Chat message usage.', '  &7- &b/print title&7: Title message usage.', '&7' ]",
                "reminder:",
                "  success: '<P> &7Message sent successfully to {target}.'"));

        assertEquals(before, print.getStringList("messages"));
        List<String> lines = new ArrayList<>();
        collect(print, lines);
        assertTrue(lines.contains(LegacyProviders.SENT));
    }

    @Test
    void editedPrintMessagesReplaceEveryCopy() throws Exception {
        YamlConfiguration print = bundled("print");
        LegacyProviders.applyPrint(print, lang(
                "help:",
                "  targets: [ 'My targets' ]",
                "reminder:",
                "  success: '&aDone: {target}'"));

        List<String> lines = new ArrayList<>();
        collect(print, lines);
        assertFalse(lines.contains(LegacyProviders.SENT));
        assertTrue(lines.stream().filter("&aDone: {target}"::equals).count() >= 8, "every channel");
        assertEquals(Arrays.asList("My targets"), print.getStringList("arguments.targets.messages"));
    }

    @Test
    void editedClearChatMessagesMoveToTheirArguments() throws Exception {
        YamlConfiguration clear = bundled("clear-chat");
        LegacyProviders.applyClearChat(clear, lang(
                "help: [ 'Custom help' ]",
                "success:",
                "  all: '<P> &7Chat has been cleared for all players.'",
                "  player: '&a{player} has a clean chat'"));

        assertEquals(Arrays.asList("Custom help"), clear.getStringList("messages"));
        assertEquals(Arrays.asList("Custom help"), clear.getStringList("arguments.help.messages"));
        assertEquals(Arrays.asList("[to:all][repeat:200]", "<P> &7Chat has been cleared for all players."),
                clear.getStringList("arguments.@a.messages"));
        assertEquals(Arrays.asList("[to:target][repeat:200]", "&a{target} has a clean chat"),
                clear.getStringList("arguments.target.messages"));
    }
}
