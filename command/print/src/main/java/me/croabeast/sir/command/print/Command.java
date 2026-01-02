package me.croabeast.sir.command.print;

import me.croabeast.command.TabBuilder;
import me.croabeast.common.CollectionBuilder;
import me.croabeast.sir.ExtensionFile;
import me.croabeast.sir.command.SIRCommand;
import me.croabeast.sir.user.SIRUser;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

final class Command extends SIRCommand {

    private final PrintProvider main;

    Command(PrintProvider main) throws IOException {
        super("print", new ExtensionFile(main, "lang", true));
        this.main = main;
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        if (!isPermitted(sender)) return true;
        if (args.length == 0) return createSender(sender).send("help.main");

        String subCommand = args[0].toLowerCase(Locale.ENGLISH);
        String[] rest = Arrays.copyOfRange(args, 1, args.length);

        switch (subCommand) {
            case "targets":
                if (!isSubCommandPermitted(sender, "targets", true)) return true;
                return rest.length == 0 ?
                        createSender(sender).send("help.targets") :
                        isWrongArgument(sender, rest[rest.length - 1]);

            case "chat":
                if (!isSubCommandPermitted(sender, "chat", true)) return true;
                if (rest.length == 0) return createSender(sender).send("help.chat");
                if (rest.length < 3) return createSender(sender).send("empty-message");

                TargetLoader.sendConfirmation(this, sender, rest[0]);

                boolean hasArg = rest[1].matches("(?i)DEFAULT|CENTERED|MIXED");
                new Printer(main, rest, hasArg ? 2 : 1).print(sender, rest[0], "");
                return true;

            case "action-bar":
                if (!isSubCommandPermitted(sender, "action-bar", true)) return true;
                if (rest.length == 0) return createSender(sender).send("help.action-bar");
                if (rest.length < 2) return createSender(sender).send("empty-message");

                TargetLoader.sendConfirmation(this, sender, rest[0]);
                new Printer(main, rest, 1).print(sender, rest[0], "ACTION-BAR");
                return true;

            case "title":
                if (!isSubCommandPermitted(sender, "title", true)) return true;
                if (rest.length == 0) return createSender(sender).send("help.title");
                if (rest.length < 2) return createSender(sender).send("empty-message");

                TargetLoader.sendConfirmation(this, sender, rest[0]);
                new Printer(main, rest, 2).print(sender, rest[0], "TITLE");
                return true;

            default:
                return isWrongArgument(sender, args[0]);
        }
    }

    @Override
    public TabBuilder getCompletionBuilder() {
        TabBuilder builder = createBasicTabBuilder().addArgument(0, "sir.print.targets", "targets")
                .addArgument(0, "sir.print.chat", "chat")
                .addArgument(0, "sir.print.action-bar", "action-bar")
                .addArgument(0, "sir.print.title", "title")
                .addArguments(1, "@a", "perm:", "world:")
                .addArguments(1,
                        CollectionBuilder.of(main.getApi().getUserManager().getUsers())
                                .filter(SIRUser::isVanished)
                                .map(SIRUser::getName).toList());

        if (main.getApi().getChat().isEnabled()) builder.addArgument(1, "group:");

        return builder.addArgument(2, (s, a) -> a[0].matches("(?i)action-bar"), "<message>")
                .addArguments(2, (s, a) -> a[0].matches("(?i)title"), "default", "10,50,10")
                .addArguments(2, (s, a) -> a[0].matches("(?i)chat"), "default", "centered", "mixed")
                .addArgument(3, (s, a) -> a[0].matches("(?i)chat|title"), "<message>");
    }
}
