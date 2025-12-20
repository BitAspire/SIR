package me.croabeast.sir.module.announcement;

import lombok.Getter;
import me.croabeast.command.BaseCommand;
import me.croabeast.command.TabBuilder;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.sir.command.SIRCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Deque;
import java.util.LinkedList;

final class Command extends SIRCommand {

    private final Announcements main;
    @Getter
    private final ConfigurableFile lang;

    Command(Announcements main, ConfigurableFile lang) {
        this.main = main;
        this.lang = lang;

        editSubCommand("help", (sender, args) -> args.length < 1 ?
                createSender(sender).send("help") :
                isWrongArgument(sender, args[args.length - 1]));

        editSubCommand("preview", (sender, args) -> {
            if (!(sender instanceof Player)) {
                main.getLogger().log("&cYou can't preview an announce in console.");
                return true;
            }

            if (args.length == 1) {
                main.announce(args[0]);
                return true;
            }

            return createSender(sender).send("select");
        });
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        return createSender(sender).send("help");
    }

    @Override
    public TabBuilder getCompletionBuilder() {
        final TabBuilder builder = createBasicTabBuilder();

        for (BaseCommand sub : getSubCommands()) {
            Deque<String> list = new LinkedList<>(sub.getAliases());
            list.addFirst(sub.getName());
            builder.addArguments(0, (s, a) -> sub.isPermitted(s), list);
        }

        return builder.addArguments(1,
                (s, a) -> a[0].matches("(?i)preview"),
                main.data.getAnnouncements().keySet()
        );
    }
}
