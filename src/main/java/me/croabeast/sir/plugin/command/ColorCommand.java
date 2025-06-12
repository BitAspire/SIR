package me.croabeast.sir.plugin.command;

import lombok.Getter;
import me.croabeast.command.BaseCommand;
import me.croabeast.command.TabBuilder;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.sir.plugin.FileData;
import me.croabeast.sir.plugin.user.SIRUser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;

@Getter
final class ColorCommand extends SIRCommand {

    final ConfigurableFile lang = FileData.Command.Multi.CHAT_COLOR.getFile(true);

    ColorCommand() {
        super(Key.CHAT_COLOR, true);

        editSubCommand("help", (s, a) -> createSender(s).send("help"));

        for (ChatColor color : ChatColor.values()) {
            final String name = color.asBungee().getName();

            switch (color) {
                case MAGIC:
                case STRIKETHROUGH:
                case UNDERLINE:
                case BOLD:
                case ITALIC:
                    editSubCommand(name, (s, a) -> {
                        return true;
                    });
                    continue;

                case RED:
                case GREEN:
                case BLUE:
                case YELLOW:
                case AQUA:
                case DARK_RED:
                case DARK_GREEN:
                case DARK_BLUE:
                case DARK_AQUA:
                case DARK_GRAY:
                case GRAY:
                case BLACK:
                case WHITE:
                case GOLD:
                case DARK_PURPLE:
                case LIGHT_PURPLE:
                    continue;

                case RESET:
                    editSubCommand("reset", (s, a) -> {
                        if (!(s instanceof Player))
                            return createSender(s).send("need-player");

                        SIRUser user = plugin.getUserManager().getUser(s);
                        if (user == null)
                            return checkPlayer(s, s.getName());

                        user.getColorData().removeAnyFormats();
                        return createSender(s).send("reset");
                    });
            }
        }
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        return Objects.requireNonNull(getSubCommand("help")).getPredicate().test(sender, args);
    }

    @Override
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
