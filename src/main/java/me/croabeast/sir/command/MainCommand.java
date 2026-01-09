package me.croabeast.sir.command;

import me.croabeast.command.BaseCommand;
import me.croabeast.command.TabBuilder;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.command.CommandPredicate;
import me.croabeast.common.util.ServerInfoUtils;
import me.croabeast.sir.SIRPlugin;
import me.croabeast.sir.FileData;
import me.croabeast.sir.misc.Timer;
import me.croabeast.takion.message.MessageSender;
import org.apache.commons.lang.SystemUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;

final class MainCommand extends SIRCommand {

    MainCommand() {
        super(Key.SIR, false);

        final MessageSender sender = plugin.getLibrary().getLoadedSender();

        editSubCommand("reload", (s, strings) -> {
            final Timer timer = Timer.create(true);
            FileData.loadFiles();

            plugin.getModuleManager().unload();
            plugin.getCommandManager().unload();

            plugin.getModuleManager().load();
            plugin.getModuleManager().register();

            plugin.getCommandManager().load();
            plugin.getCommandManager().register();

            return senderPredicate("{time}", timer.result(), "reload").test(s, strings);
        });

        editSubCommand("modules", (s, strings) -> {
            final Player player = s instanceof Player ? (Player) s : null;
            if (player == null)
                return sender.copy().send("&cThis command is only for players.");

            if (ServerInfoUtils.SERVER_VERSION < 14.0)
                return sender.copy().setTargets(player).send(
                        "<P> &cModules GUI is not supported on this version.",
                        "<P> &7Enable/disable modules in modules/modules.yml file"
                );

            plugin.getModuleManager().getMenu().showGui(player);
            return true;
        });

        editSubCommand("commands", (s, strings) -> {
            final Player player = s instanceof Player ? (Player) s : null;
            if (player == null)
                return sender.copy().send("&cThis command is only for players.");

            if (ServerInfoUtils.SERVER_VERSION < 14.0)
                return sender.copy().setTargets(player).send(
                        "<P> &cCommands GUI is not supported on this version.",
                        "<P> &7Enable/disable commands in commands/commands.yml file"
                );

            plugin.getCommandManager().getMenu().showGui(player);
            return true;
        });

        editSubCommand("about", (s, strings) -> {
            final Player player = s instanceof Player ? (Player) s : null;

            return sender.copy().setTargets(player).send(
                    "", " &eSIR &7- &f" + SIRPlugin.getVersion() + "&7:",
                    "   &8• &7Server Software: &f" + ServerInfoUtils.SERVER_FORK,
                    "   &8• &7Developer: &f" + SIRPlugin.getAuthor(),
                    "   &8• &7Java Version: &f" + SystemUtils.JAVA_VERSION, ""
            );
        });

        editSubCommand("help", senderPredicate("{version}", SIRPlugin.getVersion(), "help"));
        editSubCommand("support",
                senderPredicate("{link}", "https://discord.gg/s9YFGMrjyF", "support"));
    }

    private CommandPredicate senderPredicate(String key, Object o, String path) {
        return (s, strings) -> createSender(s).addPlaceholder(key, o).send(path);
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        return Objects.requireNonNull(getSubCommand("help")).getPredicate().test(sender, args);
    }

    @NotNull
    protected ConfigurableFile getLang() {
        return FileData.Command.SIR.getFile();
    }

    @NotNull
    public TabBuilder getCompletionBuilder() {
        final TabBuilder builder = createBasicTabBuilder();

        for (BaseCommand sub : getSubCommands()) {
            Deque<String> list = new LinkedList<>(sub.getAliases());
            list.addFirst(sub.getName());

            builder.addArguments(0, (s, a) -> sub.isPermitted(s), list);
        }

        return builder;
    }
}
