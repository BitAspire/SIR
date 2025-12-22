package me.croabeast.sir.command.message;

import lombok.Getter;
import me.croabeast.command.TabBuilder;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.sir.command.SIRCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Supplier;

@Getter
abstract class Command extends SIRCommand {

    Command(String name, ConfigurableFile lang) {
        super(name, lang);
    }

    protected final String isConsoleValue(CommandSender sender) {
        return !(sender instanceof Player) ?
                getLang().get("lang.console-formatting.name", "") :
                sender.getName();
    }

    @NotNull
    public abstract Supplier<Collection<String>> generateCompletions(CommandSender sender, String[] arguments);

    public final TabBuilder getCompletionBuilder() {
        return null;
    }
}
