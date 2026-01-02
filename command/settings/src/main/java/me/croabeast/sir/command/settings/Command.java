package me.croabeast.sir.command.settings;

import me.croabeast.command.TabBuilder;
import me.croabeast.common.util.ServerInfoUtils;
import me.croabeast.sir.ChatToggleable;
import me.croabeast.sir.ExtensionFile;
import me.croabeast.sir.command.SIRCommand;
import me.croabeast.sir.user.SIRUser;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

final class Command extends SIRCommand {

    private static final List<String> STATE_ARGUMENTS = Arrays.asList(
            "enable", "enabled", "disable", "disabled", "toggle", "on", "off", "true", "false"
    );

    private final SettingsProvider main;

    Command(SettingsProvider main) throws IOException {
        super("chat-settings", new ExtensionFile(main, "lang", true));
        this.main = main;
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (!isPermitted(sender)) return true;

        Player player = sender instanceof Player ? (Player) sender : null;
        SIRUser user = main.getApi().getUserManager().getUser(sender);
        if (player == null || user == null) return createSender(sender).setLogger(false).send("player-only");

        if (args.length == 0) {
            if (ServerInfoUtils.SERVER_VERSION >= 14) {
                main.openMainMenu(player);
                return true;
            }

            return createSender(sender).send("help.main");
        }

        SettingsProvider.Category category = SettingsProvider.Category.fromInput(args[0]);
        if (category == null) return isWrongArgument(sender, args[0]);

        if (args.length < 3) {
            String path = category == SettingsProvider.Category.MODULES ? "help.modules" : "help.commands";
            return createSender(sender).send(path);
        }

        String name = args[1];
        ChatToggleState state = ChatToggleState.fromInput(args[2]);
        if (state == null) return isWrongArgument(sender, args[2]);

        return handleToggle(sender, user, category, name, state);
    }

    private boolean handleToggle(CommandSender sender, SIRUser user,
                                 SettingsProvider.Category category,
                                 String name,
                                 ChatToggleState state) {
        ChatToggleable toggleable = main.findToggleable(category, name);
        if (toggleable == null)
            return createSender(sender)
                    .addPlaceholder("{type}", category.getLabel().toLowerCase(Locale.ENGLISH))
                    .addPlaceholder("{name}", name)
                    .send("toggle.not-found");

        boolean current = main.isEnabled(user, category, toggleable.getKey());
        boolean next = state == ChatToggleState.TOGGLE ? !current : state.isEnabled();

        main.setEnabled(user, category, toggleable.getKey(), next);

        String statePath = next ? "toggle.state.enabled" : "toggle.state.disabled";
        return createSender(sender)
                .addPlaceholder("{type}", category.getLabel())
                .addPlaceholder("{name}", toggleable.getKey())
                .addPlaceholder("{state}", getLang().get(statePath, next ? "enabled" : "disabled"))
                .send("toggle.success");
    }

    @Override
    public TabBuilder getCompletionBuilder() {
        TabBuilder builder = createBasicTabBuilder()
                .addArguments(0, "modules", "commands");

        builder.addArguments(1, (s, a) -> a[0].equalsIgnoreCase("modules"),
                main.getToggleableList(SettingsProvider.Category.MODULES).stream()
                        .map(ChatToggleable::getKey)
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.toList())
        );

        builder.addArguments(1, (s, a) -> a[0].equalsIgnoreCase("commands"),
                main.getToggleableList(SettingsProvider.Category.COMMANDS).stream()
                        .map(ChatToggleable::getKey)
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.toList())
        );

        return builder.addArguments(2, (s, a) -> a[0].matches("(?i)modules?|commands?"), STATE_ARGUMENTS);
    }

    enum ChatToggleState {
        ENABLE(true),
        DISABLE(false),
        TOGGLE(null);

        private final Boolean enabled;

        ChatToggleState(Boolean enabled) {
            this.enabled = enabled;
        }

        boolean isEnabled() {
            return Boolean.TRUE.equals(enabled);
        }

        static ChatToggleState fromInput(String input) {
            if (StringUtils.isBlank(input)) return null;

            if (input.matches("(?i)enable|enabled|on|true")) return ENABLE;
            if (input.matches("(?i)disable|disabled|off|false")) return DISABLE;
            if (input.matches("(?i)toggle")) return TOGGLE;
            return null;
        }
    }
}
