package me.croabeast.sir.command.message;

import me.croabeast.common.CollectionBuilder;
import me.croabeast.sir.SIRApi;
import me.croabeast.sir.user.SIRUser;
import me.croabeast.takion.message.MessageSender;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

final class Message extends Command {

    private final MessageProvider main;

    Message(MessageProvider main) {
        super("message", main.getLang());
        this.main = main;
    }

    @Override
    public boolean execute(@NotNull CommandSender s, String[] args) {
        if (!isPermitted(s)) return true;

        final SIRUser user = main.getApi().getUserManager().getUser(s);
        if (user != null && user.getMuteData().isMuted())
            return Utils.create(this, s).setLogger(false).send("is-muted");

        if (args.length == 0)
            return Utils.create(this, s).setLogger(false).send("need-player");

        SIRUser target = main.getApi().getUserManager().fromClosest(args[0]);
        if (target == null) return checkPlayer(s, args[0]);

        if (Objects.equals(target, user))
            return Utils.create(this, s).setLogger(false).send("not-yourself");

        if (target.getIgnoreData().isIgnoring(user, false))
            return main.getApi().getLibrary().getLoadedSender()
                    .setLogger(false)
                    .addPlaceholder("{type}", getLang().get("lang.channels.msg", ""))
                    .send(getLang().toStringList("lang.ignoring"));

        boolean vanished = getLang().get("lang.vanish-messages.enabled", true);
        if (target.isVanished() && vanished)
            return Utils.create(this, s).setLogger(false).send("vanish-messages.message");

        String message = SIRApi.joinArray(1, args);
        if (StringUtils.isBlank(message))
            return Utils.create(this, s).setLogger(false).send("empty-message");

        Values initValues = new Values(main, true);
        Values receiveValues = new Values(main, false);

        boolean senderEnabled = user == null || main.isToggled(user);
        boolean receiverEnabled = main.isToggled(target);

        Player player = target.getPlayer();

        MessageSender sender = Utils.create(this, null)
                .setLogger(false)
                .addPlaceholder("{receiver}", isConsoleValue(player))
                .addPlaceholder("{message}", message)
                .addPlaceholder("{sender}", isConsoleValue(s));

        if (senderEnabled) initValues.playSound(s);
        if (receiverEnabled) receiveValues.playSound(player);

        if (senderEnabled) sender.copy().setTargets(s).send(initValues.getOutput());
        if (receiverEnabled) sender.copy().setTargets(player).send(receiveValues.getOutput());

        main.replies.put(player, s);
        main.replies.put(s, player);

        return sender.setErrorPrefix(null)
                .setLogger(true).send("console-formatting.format");
    }

    @NotNull
    public Supplier<Collection<String>> generateCompletions(CommandSender sender, String[] arguments) {
        return () -> Utils.newBuilder()
                .addArguments(0,
                        CollectionBuilder.of(main.getApi().getUserManager().getUsers())
                                .filter(u -> !u.isVanished())
                                .map(SIRUser::getName).toList()
                )
                .addArgument(1, "<message>").build(sender, arguments);
    }
}
