package me.croabeast.sir.plugin.command;

import lombok.Getter;
import me.croabeast.command.TabBuilder;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.sir.plugin.FileData;
import org.bukkit.command.CommandSender;

@Getter
final class ColorCommand extends SIRCommand {

    final ConfigurableFile lang = FileData.Command.Multi.CHAT_COLOR.getFile(true);

    ColorCommand() {
        super(Key.CHAT_COLOR, true);
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        return false;
    }

    @Override
    public TabBuilder getCompletionBuilder() {
        return null;
    }
}
