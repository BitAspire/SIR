package me.croabeast.sir;

import lombok.SneakyThrows;
import me.croabeast.command.TabBuilder;
import me.croabeast.common.util.ServerInfoUtils;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.takion.message.MessageSender;
import org.apache.commons.lang.SystemUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

final class MainCommand implements TabExecutor {

    private static final String PERMISSION_PREFIX = "sir.admin.";
    private static final String WILD_CARD = PERMISSION_PREFIX + "*";

    private static final List<String> SUB_COMMANDS = Arrays.asList("modules", "about", "reload", "help", "commands", "support");

    private final SIRPlugin main;
    private final ConfigurableFile lang;

    @SneakyThrows
    MainCommand(SIRPlugin main) {
        this.main = main;
        this.lang = new ConfigurableFile(main, "commands" + File.separator + "main", "lang");
        lang.saveDefaults();
    }

    private class CommandDisplayer extends MessageSender {

        private CommandDisplayer(MessageSender sender) {
            super(sender);
        }

        private CommandDisplayer(CommandSender sender) {
            super(main.getLibrary().getLoadedSender());
            setLogger(sender instanceof Player).setTargets(sender);
        }

        @NotNull
        public MessageSender copy() {
            return new CommandDisplayer(this);
        }

        @Override
        public boolean send(String... strings) {
            if (strings.length != 1)
                throw new NullPointerException("Needs only a single path");

            return super.send(lang.toStringList("lang." + strings[0]));
        }
    }

    boolean isProhibited(CommandSender sender, String permission) {
        if (main.getUserManager().hasPermission(sender, permission))
            return false;

        main.getLibrary()
                .getLoadedSender().addPlaceholder("{perm}", permission)
                .addPlaceholder("{permission}", permission)
                .send(main.getCommandLang().toStringList("lang.no-permission"));
        return true;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (isProhibited(sender, "sir.admin") || isProhibited(sender, WILD_CARD))
            return false;

        CommandDisplayer displayer = new CommandDisplayer(sender);
        MessageSender mainSender = main.getLibrary().getLoadedSender();

        String first;
        if (args.length != 1 || !SUB_COMMANDS.contains(first = args[0].toLowerCase(Locale.ENGLISH)))
            return displayer.addPlaceholder("{version}", main.getDescription().getVersion()).send("help");

        if (isProhibited(sender, PERMISSION_PREFIX + first)) return false;

        Player player = sender instanceof Player ? (Player) sender : null;
        switch (first) {
            case "about":
                return mainSender.setTargets(sender).send(
                        "",
                        " &eSIR &7- &f" + main.getDescription().getVersion() + "&7:",
                        "   &8• &7Server Software: &f" + ServerInfoUtils.SERVER_FORK,
                        "   &8• &7Developer: &fCroaBeast",
                        "   &8• &7Java Version: &f" + SystemUtils.JAVA_VERSION,
                        ""
                );

            case "modules":
                if (player == null)
                    return mainSender.send("&cThis command is only for players.");

                if (ServerInfoUtils.SERVER_VERSION < 14.0)
                    return mainSender.setTargets(player).send(
                            "<P> &cModules GUI is not supported on this version.",
                            "<P> &7Enable/disable modules in modules/states.yml file"
                    );

                main.getModuleManager().getMenu().showGui(player);
                return true;

            case "commands":
                if (player == null)
                    return mainSender.send("&cThis command is only for players.");

                if (ServerInfoUtils.SERVER_VERSION < 14.0)
                    return mainSender.setTargets(player).send(
                            "<P> &cCommands GUI is not supported on this version.",
                            "<P> &7Enable/disable commands in commands/states.yml file"
                    );

                main.getCommandManager().getMenu().showGui(player);
                return true;

            case "reload":
                Timer timer = Timer.create();
                lang.reload();
                main.reload();
                return displayer.addPlaceholder("{time}", timer.current()).send("reload");

            case "support":
                return displayer.addPlaceholder("{link}", "https://discord.gg/s9YFGMrjyF").send("support");

            case "help":
            default:
                return displayer.addPlaceholder("{version}", main.getDescription().getVersion()).send("help");
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        TabBuilder builder = new TabBuilder().setPermissionPredicate(main.getUserManager()::hasPermission);

        for (String arg : SUB_COMMANDS) {
            String permission = PERMISSION_PREFIX + arg;
            builder.addArgument(0, (s, a) -> main.getUserManager().hasPermission(s, permission), permission);
        }

        return builder.build(sender, args);
    }
}
