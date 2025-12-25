package me.croabeast.sir.module.announcement;

import me.croabeast.command.TabBuilder;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.sir.command.SIRCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

final class Command extends SIRCommand {

    private final Announcements main;

    Command(Announcements main, ConfigurableFile lang) {
        super("announce", lang);
        this.main = main;
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (!isPermitted(sender)) return true;
        if (args.length == 0) return createSender(sender).send("help");

        String subCommand = args[0].toLowerCase(Locale.ENGLISH);
        switch (subCommand) {
            case "help":
                if (!isSubCommandPermitted(sender, "help", true)) return true;
                return args.length < 2 ?
                        createSender(sender).send("help") :
                        isWrongArgument(sender, args[args.length - 1]);

            case "preview":
                if (!isSubCommandPermitted(sender, "preview", true)) return true;
                if (!(sender instanceof Player)) {
                    main.getLogger().log("&cYou can't preview an announce in console.");
                    return true;
                }

                if (args.length == 2) {
                    main.announce(args[1]);
                    return true;
                }

                return createSender(sender).send("select");

            default:
                return isWrongArgument(sender, args[0]);
        }
    }

    @Override
    public TabBuilder getCompletionBuilder() {
        final TabBuilder builder = createBasicTabBuilder();
        builder.addArguments(0, (s, a) -> isSubCommandPermitted(s, "help", false), "help");
        builder.addArguments(0, (s, a) -> isSubCommandPermitted(s, "preview", false), "preview");

        return builder.addArguments(1,
                (s, a) -> a[0].matches("(?i)preview"),
                main.data.getAnnouncements().keySet());
    }
}
