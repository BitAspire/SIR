package me.croabeast.sir.command.print;

import lombok.Getter;
import lombok.SneakyThrows;
import me.croabeast.command.TabBuilder;
import me.croabeast.common.CollectionBuilder;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.sir.ExtensionFile;
import me.croabeast.sir.command.SIRCommand;
import me.croabeast.sir.user.SIRUser;
import org.bukkit.command.CommandSender;

final class Command extends SIRCommand {

    private final PrintProvider main;
    @Getter
    private final ConfigurableFile lang;

    @SneakyThrows
    Command(PrintProvider main) {
        super("print");
        this.main = main;
        lang = new ExtensionFile(main, "lang", true);

        editSubCommand("targets", (sender, args) -> args.length == 0 ?
                createSender(sender).send("help.targets") :
                isWrongArgument(sender, args[args.length - 1]));

        editSubCommand("chat", (sender, args) -> {
            if (args.length == 0) return createSender(sender).send("help.chat");
            if (args.length < 3) return createSender(sender).send("empty-message");

            TargetLoader.sendConfirmation(this, sender, args[0]);

            boolean hasArg = args[1].matches("(?i)DEFAULT|CENTERED|MIXED");
            new Printer(args, hasArg ? 2 : 1).print(sender, args[0], "");
            return true;
        });

        editSubCommand("action-bar", (sender, args) -> {
            if (args.length == 0) return createSender(sender).send("help.action-bar");
            if (args.length < 2) return createSender(sender).send("empty-message");

            TargetLoader.sendConfirmation(this, sender, args[0]);
            new Printer(args, 1).print(sender, args[0], "ACTION-BAR");
            return true;
        });

        editSubCommand("title", (sender, args) -> {
            if (args.length == 0)
                return createSender(sender).send("help.title");
            if (args.length < 2)
                return createSender(sender).send("empty-message");

            TargetLoader.sendConfirmation(this, sender, args[0]);
            new Printer(args, 2).print(sender, args[0], "TITLE");
            return true;
        });
    }

    @Override
    protected boolean execute(CommandSender sender, String[] args) {
        return createSender(sender).send("help.main");
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
