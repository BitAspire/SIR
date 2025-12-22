package me.croabeast.sir.command.message;

import me.croabeast.sir.SIRApi;
import me.croabeast.sir.user.SIRUser;
import me.croabeast.takion.message.MessageSender;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Supplier;

public class Reply extends Command {

    private final MessageProvider main;

    Reply(MessageProvider main) {
        super("reply", main.getLang());
        this.main = main;
    }

    @Override
    protected boolean execute(CommandSender s, String[] args) {
        if (!isPermitted(s)) return true;

        SIRUser receiver = main.getApi().getUserManager().getUser(s);
        if (receiver != null && receiver.getMuteData().isMuted())
            return createSender(s).setLogger(false).send("is-muted");

        CommandSender init = main.replies.get(s);
        if (init == null)
            return createSender(s).setLogger(false).send("not-replied");

        SIRUser initiator = main.getApi().getUserManager().getUser(init);

        if (initiator.getIgnoreData().isIgnoring(receiver, false))
            return main.getApi().getLibrary().getLoadedSender()
                    .setLogger(false)
                    .addPlaceholder("{type}", getLang().get("lang.channels.msg", ""))
                    .send(getLang().toStringList("lang.ignoring"));

        if (getLang().get("lang.vanish-messages.enabled", true) &&
                initiator.isVanished())
            return createSender(s).setLogger(false).send("vanish-messages.message");

        String message = SIRApi.joinArray(0, args);
        if (StringUtils.isBlank(message))
            return createSender(s).setLogger(false).send("empty-message");

        Values initValues = new Values(main, true);
        Values receiveValues = new Values(main, false);

        initValues.playSound(init);
        receiveValues.playSound(s);

        final MessageSender sender = createSender(null)
                .setLogger(false)
                .addPlaceholder("{receiver}", isConsoleValue(init))
                .addPlaceholder("{message}", message)
                .addPlaceholder("{sender}", isConsoleValue(s));

        sender.copy().setTargets(s).send(initValues.getOutput());
        sender.copy().setTargets(init).send(receiveValues.getOutput());

        return sender.setErrorPrefix(null)
                .setLogger(true).send("console-formatting.format");
    }

    @NotNull
    public Supplier<Collection<String>> generateCompletions(CommandSender sender, String[] arguments) {
        return () -> createBasicTabBuilder().addArgument(0, "<message>").build(sender, arguments);
    }
}
