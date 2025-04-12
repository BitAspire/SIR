package me.croabeast.sir.plugin.command;

import lombok.Getter;
import me.croabeast.command.TabBuilder;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.sir.plugin.FileData;
import me.croabeast.sir.plugin.misc.SIRUser;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

@Getter
final class ClearCommand extends SIRCommand {

    private final ConfigurableFile lang = FileData.Command.CLEAR_CHAT.getFile();

    ClearCommand() {
        super(Key.CLEAR_CHAT, true);
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (!isPermitted(sender)) return true;

        if (args.length != 1)
            return createSender(sender).send("help");

        if (args[0].matches("(?i)@a")) {
            int i = 0;
            while (i++ < 200) Bukkit.broadcastMessage("");
            return createSender(sender).send("success.all");
        }

        SIRUser user = plugin.getUserManager().getUser(args[0]);
        if (user == null)
            return createSender(sender)
                    .addPlaceholder("{target}", args[0])
                    .send("not-player");

        int i = 0;
        while (i++ < 200) user.getPlayer().sendMessage("");

        return createSender(sender)
                .addPlaceholder("{player}", args[0])
                .send("success.player");
    }

    @Override
    public TabBuilder getCompletionBuilder() {
        return null;
    }
}
