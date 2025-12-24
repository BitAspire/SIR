package me.croabeast.sir;

import lombok.SneakyThrows;
import me.croabeast.command.TabBuilder;
import me.croabeast.common.util.ServerInfoUtils;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.sir.command.CommandManager;
import me.croabeast.sir.command.ProviderInformation;
import me.croabeast.sir.module.ModuleManager;
import me.croabeast.sir.module.SIRModule;
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
    private static final List<String> STATE_ARGUMENTS = Arrays.asList("enable", "enabled", "disable", "disabled", "toggle", "on", "off", "true", "false");

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
            setLogger(!(sender instanceof Player)).setTargets(sender);
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

    private Boolean resolveState(String value, boolean current) {
        if (value == null) return !current;

        switch (value.toLowerCase(Locale.ENGLISH)) {
            case "enable":
            case "enabled":
            case "on":
            case "true":
                return true;
            case "disable":
            case "disabled":
            case "off":
            case "false":
                return false;
            case "toggle":
                return !current;
            default:
                return null;
        }
    }

    private boolean handleLegacyModules(CommandSender s, String[] args) {
        ModuleManager moduleManager = main.getModuleManager();

        MessageSender sender = main.getLibrary().getLoadedSender()
                .setTargets(s)
                .setLogger(!(s instanceof Player));

        if (args.length < 2)
            return sender.send(
                    "<P> &7Usage: &f/sir modules <module> [enable|disable|toggle]",
                    "<P> &7Available: &f" + String.join(", ", moduleManager.getModuleNames())
            );

        SIRModule module = moduleManager.getModule(args[1]);
        if (module == null)
            return sender.send("<P> &cModule not found: &f" + args[1]);

        boolean current = moduleManager.isEnabled(module.getName());
        Boolean next = resolveState(args.length > 2 ? args[2] : null, current);
        if (next == null)
            return sender.send("<P> &cInvalid state. Use: enable, disable, toggle.");

        moduleManager.updateModuleEnabled(module.getName(), next);
        moduleManager.saveStates();

        return sender.send(
                "<P> &7Module &f" + module.getName() + " &7is now " + (next ? "&aenabled" : "&cdisabled") + "&7."
        );
    }

    private boolean handleLegacyCommands(CommandSender s, String[] args) {
        CommandManager commandManager = main.getCommandManager();
        MessageSender sender = main.getLibrary().getLoadedSender()
                .setTargets(s)
                .setLogger(!(s instanceof Player));

        if (args.length < 2)
            return sender.send(
                    "<P> &7Usage: &f/sir commands <provider> <enabled|override> [command] [state]",
                    "<P> &7Available: &f" + String.join(", ", commandManager.getProviderNames())
            );

        ProviderInformation info = commandManager.getInformation(args[1]);
        if (info == null)
            return sender.send("<P> &cCommand provider not found: &f" + args[1]);

        if (args.length < 3)
            return sender.send(
                    "<P> &7Usage: &f/sir commands " + info.getName() + " <enabled|override> [command] [state]"
            );

        String mode = args[2].toLowerCase(Locale.ENGLISH);
        if (mode.equals("enabled")) {
            boolean current = commandManager.isProviderEnabled(info.getName());
            Boolean next = resolveState(args.length > 3 ? args[3] : null, current);
            if (next == null)
                return sender.send("<P> &cInvalid state. Use: enable, disable, toggle.");

            if (!commandManager.updateProviderEnabled(info.getName(), next))
                return sender.send("<P> &cFailed to update provider state.");

            commandManager.saveStates();
            return sender.send(
                    "<P> &7Provider &f" + info.getName() + " &7is now " + (next ? "&aenabled" : "&cdisabled") + "&7."
            );
        }

        if (mode.equals("override")) {
            if (args.length < 4)
                return sender.send(
                        "<P> &7Usage: &f/sir commands " + info.getName() + " override <command> [state]"
                );

            String commandKey = args[3];
            Boolean current = commandManager.getCommandOverride(info.getName(), commandKey);
            if (current == null)
                return sender.send("<P> &cCommand not found: &f" + commandKey);

            Boolean next = resolveState(args.length > 4 ? args[4] : null, current);
            if (next == null)
                return sender.send("<P> &cInvalid state. Use: enable, disable, toggle.");

            if (!commandManager.updateCommandOverride(info.getName(), commandKey, next))
                return sender.send("<P> &cFailed to update command override.");

            commandManager.saveStates();
            return sender.send(
                    "<P> &7Command &f" + commandKey + " &7override is now " + (next ? "&aenabled" : "&cdisabled") + "&7."
            );
        }

        return sender.send("<P> &cInvalid mode. Use enabled or override.");
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
                return mainSender.send(
                        "",
                        " &eSIR &7- &f" + main.getDescription().getVersion() + "&7:",
                        "   &8• &7Server Software: &f" + ServerInfoUtils.SERVER_FORK,
                        "   &8• &7Developer: &fCroaBeast",
                        "   &8• &7Java Version: &f" + SystemUtils.JAVA_VERSION,
                        ""
                );

            case "modules":
                if (ServerInfoUtils.SERVER_VERSION < 14.0)
                    return handleLegacyModules(sender, args);

                if (player == null)
                    return mainSender.send("&cThis command is only for players.");

                main.getModuleManager().getMenu().showGui(player);
                return true;

            case "commands":
                if (ServerInfoUtils.SERVER_VERSION < 14.0)
                    return handleLegacyCommands(sender, args);

                if (player == null)
                    return mainSender.send("&cThis command is only for players.");

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

        for (String arg : SUB_COMMANDS)
            builder.addArgument(0, (s, a) -> main.getUserManager().hasPermission(s, PERMISSION_PREFIX + arg), arg);

        if (ServerInfoUtils.SERVER_VERSION < 14.0) {
            ModuleManager moduleManager = main.getModuleManager();
            CommandManager commandManager = main.getCommandManager();

            builder.addArguments(1, (s, a) -> a.length > 0 && a[0].equalsIgnoreCase("modules"), moduleManager.getModuleNames());
            builder.addArguments(2, (s, a) -> a.length > 0 && a[0].equalsIgnoreCase("modules"), STATE_ARGUMENTS);

            builder.addArguments(1, (s, a) -> a.length > 0 && a[0].equalsIgnoreCase("commands"), commandManager.getProviderNames());
            builder.addArguments(2, (s, a) -> a.length > 0 && a[0].equalsIgnoreCase("commands"), Arrays.asList("enabled", "override"));
            builder.addArguments(3, (s, a) -> a.length > 2 && a[0].equalsIgnoreCase("commands") && a[2].equalsIgnoreCase("override"), commandManager.getProviderCommands(args.length > 1 ? args[1] : null));
            builder.addArguments(3, (s, a) -> a.length > 2 && a[0].equalsIgnoreCase("commands") && a[2].equalsIgnoreCase("enabled"), STATE_ARGUMENTS);
            builder.addArguments(4, (s, a) -> a.length > 2 && a[0].equalsIgnoreCase("commands") && a[2].equalsIgnoreCase("override"), STATE_ARGUMENTS);
        }

        return builder.build(sender, args);
    }
}
