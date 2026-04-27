package com.bitaspire.sir.command.nick;

import com.bitaspire.sir.SIRApi;
import com.bitaspire.sir.command.SIRCommand;
import com.bitaspire.sir.user.SIRUser;
import me.croabeast.command.TabBuilder;
import me.croabeast.takion.message.MessageSender;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

final class Command extends SIRCommand {

    private static final String RESET_FLAG = "--reset";

    private final NickProvider main;

    Command(NickProvider main) {
        super("nick", Objects.requireNonNull(main.lang, "Nick lang file not initialized"));
        this.main = main;
    }

    private boolean isResetArgument(String value) {
        return RESET_FLAG.equalsIgnoreCase(value);
    }

    private boolean canManageOthers(CommandSender sender) {
        return isSubCommandPermitted(sender, "other", false);
    }

    private String getAccountName(SIRUser user) {
        if (user == null) return null;

        String name = user.getOffline().getName();
        return StringUtils.isBlank(name) ? ChatColor.stripColor(user.getName()) : name;
    }

    private boolean matchesName(String candidate, String input) {
        return StringUtils.isNotBlank(candidate) && candidate.equalsIgnoreCase(input);
    }

    private boolean matchesTarget(SIRUser user, String input) {
        if (user == null || StringUtils.isBlank(input)) return false;

        String display = user.getName();
        return user.getUuid().toString().equalsIgnoreCase(input)
                || matchesName(getAccountName(user), input)
                || matchesName(display, input)
                || matchesName(ChatColor.stripColor(display), input);
    }

    private SIRUser resolveTarget(String input) {
        Set<SIRUser> users = main.getApi().getUserManager().getUsers(false);
        for (SIRUser user : users)
            if (matchesTarget(user, input)) return user;
        return null;
    }

    private boolean isSamePlayer(CommandSender sender, SIRUser target) {
        return sender instanceof Player && target != null
                && target.getUuid().equals(((Player) sender).getUniqueId());
    }

    private boolean setNick(CommandSender sender, SIRUser target, String nick, boolean other) {
        if (StringUtils.isBlank(nick))
            return Utils.create(this, sender).send("invalid-nick");

        target.getNickData().setNick(nick);

        String accountName = getAccountName(target);
        String displayName = target.getName();

        MessageSender senderMessage = Utils.create(this, sender)
                .addPlaceholder("{target}", accountName)
                .addPlaceholder("{nick}", displayName)
                .addPlaceholder("{admin}", sender.getName());

        boolean result = senderMessage.send(other ? "success.other-set" : "success.self-set");

        if (other && target.isOnline() && !isSamePlayer(sender, target)) {
            Utils.create(this, target.getPlayer())
                    .addPlaceholder("{nick}", displayName)
                    .addPlaceholder("{admin}", sender.getName())
                    .send("notify.set");
        }

        return result;
    }

    private boolean resetNick(CommandSender sender, SIRUser target, boolean other) {
        target.getNickData().resetNick();

        String accountName = getAccountName(target);

        MessageSender senderMessage = Utils.create(this, sender)
                .addPlaceholder("{target}", accountName)
                .addPlaceholder("{admin}", sender.getName());

        boolean result = senderMessage.send(other ? "success.other-reset" : "success.self-reset");

        if (other && target.isOnline() && !isSamePlayer(sender, target)) {
            Utils.create(this, target.getPlayer())
                    .addPlaceholder("{admin}", sender.getName())
                    .send("notify.reset");
        }

        return result;
    }

    private Collection<String> firstCompletions(CommandSender sender) {
        TreeSet<String> completions = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        if (sender instanceof Player) completions.add(RESET_FLAG);
        if (!canManageOthers(sender)) return completions;

        for (SIRUser user : main.getApi().getUserManager().getUsers(false)) {
            String name = getAccountName(user);
            if (StringUtils.isNotBlank(name)) completions.add(name);
        }

        return completions;
    }

    private Collection<String> secondCompletions(CommandSender sender, String[] args) {
        if (!canManageOthers(sender) || args.length < 1) return Collections.emptyList();

        SIRUser target = resolveTarget(args[0]);
        if (target == null || isSamePlayer(sender, target)) return Collections.emptyList();

        return Collections.singletonList(RESET_FLAG);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, String[] args) {
        if (args.length == 0 || args[0].matches("(?i)help")) {
            if (!isPermitted(sender)) return true;
            return Utils.create(this, sender).send("help");
        }

        if (!(sender instanceof Player)) {
            if (!isSubCommandPermitted(sender, "other", true)) return true;
            if (args.length < 2) return Utils.create(this, sender).send("help");

            SIRUser target = resolveTarget(args[0]);
            if (target == null) return checkPlayer(sender, args[0]);

            return isResetArgument(args[1])
                    ? resetNick(sender, target, true)
                    : setNick(sender, target, SIRApi.joinArray(1, args), true);
        }

        Player player = (Player) sender;
        SIRUser self = main.getApi().getUserManager().getUser(player);
        if (self == null) return Utils.create(this, sender).send("player-only");

        if (args.length == 1 && isResetArgument(args[0])) {
            if (!isPermitted(sender)) return true;
            return resetNick(sender, self, false);
        }

        if (args.length >= 2 && canManageOthers(sender)) {
            SIRUser target = resolveTarget(args[0]);
            if (target != null && !target.getUuid().equals(self.getUuid())) {
                if (!isSubCommandPermitted(sender, "other", true)) return true;

                return isResetArgument(args[1])
                        ? resetNick(sender, target, true)
                        : setNick(sender, target, SIRApi.joinArray(1, args), true);
            }
        }

        if (!isPermitted(sender)) return true;
        return setNick(sender, self, SIRApi.joinArray(0, args), false);
    }

    @Override
    public TabBuilder getCompletionBuilder() {
        return Utils.newBuilder()
                .addArguments(0, (s, a) -> new ArrayList<>(firstCompletions(s)))
                .addArguments(1, (s, a) -> new ArrayList<>(secondCompletions(s, a)));
    }
}
