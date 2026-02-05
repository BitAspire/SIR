package me.croabeast.sir.command;

import lombok.Getter;
import me.croabeast.command.TabBuilder;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.sir.FileData;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Getter
final class ClearCommand extends SIRCommand {

    private final ConfigurableFile lang = FileData.Command.CLEAR_CHAT.getFile();

    ClearCommand() {
        super(Key.CLEAR_CHAT, true);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, String[] args) {
        if (!isPermitted(sender)) return true;

        if (args.length != 1)
            return Utils.create(this, sender).send("help");

        if (args[0].matches("(?i)@a")) {
            for (int i = 0; i < 200; i++)
                Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(""));
            return Utils.create(this, sender).send("success.all");
        }

        Player player = Bukkit.getPlayer(args[0]);
        if (player == null) return checkPlayer(sender, args[0]);

        for (int i = 0; i < 200; i++)
            player.sendMessage("");

        return Utils.create(this, sender)
                .addPlaceholder("{player}", args[0])
                .send("success.player");
    }

    @Override
    public TabBuilder getCompletionBuilder() {
        return Utils.newBuilder().addArgument(0, "@a").addArguments(0, Utils.getOnlineNames());
    }
}
