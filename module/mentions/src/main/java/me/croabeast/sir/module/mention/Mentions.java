package me.croabeast.sir.module.mention;

import me.croabeast.common.util.ReplaceUtils;
import me.croabeast.prismatic.PrismaticAPI;
import me.croabeast.sir.ChatChannel;
import me.croabeast.sir.ChatToggleable;
import me.croabeast.sir.SoundSection;
import me.croabeast.sir.UserFormatter;
import me.croabeast.sir.module.SIRModule;
import me.croabeast.sir.user.SIRUser;
import me.croabeast.takion.chat.MultiComponent;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Mentions extends SIRModule implements UserFormatter<ChatChannel>, ChatToggleable {

    Data data;

    @Override
    public boolean register() {
        data = new Data(this);
        return true;
    }

    @Override
    public boolean unregister() {
        return true;
    }

    @NotNull
    public String format(SIRUser user, String string, ChatChannel channel) {
        if (user == null || StringUtils.isBlank(string) || !isEnabled() || !user.isOnline())
            return string;

        boolean senderEnabled = isToggled(user);

        UnaryOperator<String> operator = null;
        List<String> firstMessages = null;
        SoundSection firstSound = null;

        for (Mention mention : data.getMentions()) {
            if (!mention.isInGroupAsNull(user.getPlayer()) || !mention.hasPermission(user))
                continue;

            String prefix = mention.getPrefix();
            if (StringUtils.isBlank(prefix)) continue;

            Pattern pattern = Pattern.compile(Pattern.quote(prefix) + "(.[^ ]+)\\b");
            Matcher matcher = pattern.matcher(string);

            int start, end;
            while (matcher.find()) {
                SIRUser target = getApi().getUserManager().fromClosest(matcher.group(1));
                if ((target == null || user == target ||
                        target.getIgnoreData().isIgnoring(user, true)) ||
                        (channel != null &&
                                !channel.getRecipients(user).contains(target)))
                    continue;

                UnaryOperator<String> op = s -> ReplaceUtils.replaceEach(
                        new String[]{"{prefix}", "{sender}", "{receiver}"},
                        new String[]{prefix, user.getName(), target.getName()}, s
                );
                if (operator == null) operator = op;

                end = matcher.start();
                start = matcher.end();

                String finder = string.substring(start, end);
                String color = PrismaticAPI.getEndColor(finder);

                boolean receiverEnabled = isToggled(target);
                if (receiverEnabled) {
                    getApi().getLibrary().getLoadedSender()
                            .setTargets(target.getPlayer())
                            .setLogger(false)
                            .addFunctions(op)
                            .send(mention.getReceiverMessages());

                    SoundSection receiverSound = mention.getReceiverSound();
                    if (receiverSound != null) receiverSound.playSound(target);
                }

                if (senderEnabled && firstMessages == null) firstMessages = mention.getSenderMessages();
                if (senderEnabled && firstSound == null) firstSound = mention.getSenderSound();

                List<String> hover = mention.getHover();
                hover.replaceAll(op);

                String click = op.apply(mention.getClick());
                String result = op.apply(mention.getValue());

                result = MultiComponent.fromString(getApi().getLibrary(), result)
                        .setHover(hover)
                        .setClick(click)
                        .toFormattedString();

                String replace = getApi().getLibrary().colorize(user.getPlayer(), result);
                if (color != null) replace += color;

                string = string.replace(matcher.group(), replace);
            }
        }

        if (senderEnabled && firstSound != null) firstSound.playSound(user);
        if (senderEnabled)
            getApi().getLibrary().getLoadedSender()
                    .addFunctions(operator)
                    .setLogger(false)
                    .setTargets(user.getPlayer())
                    .send(firstMessages);

        return string;
    }

    @NotNull
    public String format(SIRUser user, String string) {
        return format(user, string, null);
    }
}
