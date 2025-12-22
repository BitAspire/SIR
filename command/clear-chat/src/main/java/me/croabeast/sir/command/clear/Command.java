package me.croabeast.sir.command.clear;

import lombok.Getter;
import lombok.SneakyThrows;
import me.croabeast.command.TabBuilder;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.sir.ExtensionFile;
import me.croabeast.sir.command.SIRCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

final class Command extends SIRCommand {

    @Getter
    private final ConfigurableFile lang;

    @SneakyThrows
    Command(ClearProvider main) {
        super("clear-chat");
        lang = new ExtensionFile(main, "lang", true);

        editSubCommand("help", (s, a) -> createSender(s).send("help"));
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (!isPermitted(sender)) return true;

        if (args.length != 1)
            return createSender(sender).send("help");

        switch (args[0].toLowerCase(Locale.ENGLISH)) {
            case "help":
                return createSender(sender).send("help");

            case "@a":
                for (int i = 0; i < 200; i++)
                    Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(""));
                return createSender(sender).send("success.all");

            default:
                Player player = Bukkit.getPlayer(args[0]);
                if (player == null)
                    return checkPlayer(sender, args[0]);

                for (int i = 0; i < 200; i++)
                    player.sendMessage("");

                return createSender(sender)
                        .addPlaceholder("{player}", args[0])
                        .send("success.player");
        }
    }

    @Override
    public TabBuilder getCompletionBuilder() {
        return createBasicTabBuilder()
                .addArgument(0, "sir.clear-chat.help", "help")
                .addArgument(0, "@a")
                .addArguments(0, getOnlineNames());
    }
}
