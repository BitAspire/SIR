package me.croabeast.sir.command.clear;

import me.croabeast.command.TabBuilder;
import me.croabeast.sir.ExtensionFile;
import me.croabeast.sir.command.SIRCommand;
import me.croabeast.takion.message.MessageSender;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Locale;

final class Command extends SIRCommand {

    Command(ClearProvider main) throws IOException {
        super("clear-chat", new ExtensionFile(main, "lang", true));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, String[] args) {
        if (!isPermitted(sender)) return true;

        MessageSender result = Utils.create(this, sender);
        if (args.length != 1)
            return result.send("help");

        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "help":
                return result.send("help");

            case "@a":
                for (int i = 0; i < 200; i++)
                    Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(""));
                return result.send("success.all");

            default:
                Player player = Bukkit.getPlayer(args[0]);
                if (player == null)
                    return checkPlayer(sender, args[0]);

                for (int i = 0; i < 200; i++)
                    player.sendMessage("");

                return result
                        .addPlaceholder("{player}", args[0])
                        .send("success.player");
        }
    }

    @Override
    public TabBuilder getCompletionBuilder() {
        return Utils.newBuilder()
                .addArgument(0, "sir.clear-chat.help", "help")
                .addArgument(0, "@a")
                .addArguments(0, Utils.getOnlineNames());
    }
}
