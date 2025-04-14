package me.croabeast.sir.plugin.command;

import lombok.Getter;
import me.croabeast.command.TabBuilder;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.common.time.TimeFormatter;
import me.croabeast.common.time.TimeValues;
import me.croabeast.sir.plugin.Commandable;
import me.croabeast.sir.plugin.SIRPlugin;
import me.croabeast.sir.plugin.aspect.AspectButton;
import me.croabeast.sir.plugin.FileData;
import me.croabeast.sir.plugin.aspect.AspectKey;
import me.croabeast.sir.plugin.aspect.SIRAspect;
import me.croabeast.sir.plugin.misc.SIRUser;
import me.croabeast.sir.plugin.LangUtils;
import me.croabeast.takion.message.MessageSender;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
final class MuteHandler implements Commandable {

    private final Set<SIRCommand> commands = new HashSet<>();
    private final SIRAspect aspect = new MuteAspect();

    MuteHandler() {
        commands.add(new BaseCommand("checkmute") {
            @Override
            protected boolean execute(CommandSender sender, String[] args) {
                return isPermitted(sender);
            }
        });

        commands.add(new BaseCommand("mute") {
            @Override
            protected boolean execute(CommandSender sender, String[] args) {
                if (!isPermitted(sender)) return true;

                if (args.length == 0)
                    return createSender(sender).send("help.perm");

                SIRUser target = plugin.getUserManager().fromClosest(args[0]);
                if (target == null)
                    return createSender(sender).send("not-player");

                String reason = getLang().get(
                        "lang.default.mute-reason", "Not following server rules.");

                if (args.length > 1) {
                    String temp = LangUtils.stringFromArray(args, 1);
                    if (temp != null) reason = temp;
                }

                final MessageSender message = createSender(sender)
                        .addPlaceholder("{reason}", reason)
                        .addPlaceholder("{target}", target.getName())
                        .addPlaceholder("{admin}", sender.getName());

                if (target.isMuted()) {
                    final long remaining = target.getRemainingMute();
                    String path = remaining < 1 ? "perm" : "temp";

                    if (remaining > 0)
                        message.addPlaceholder("{time}", parseTime(remaining));

                    return message.send("is-muted." + path);
                }

                plugin.getUserManager().mute(target, -1, sender.getName(), reason);
                return message.setTargets(Bukkit.getOnlinePlayers()).send("action.perm");
            }
        });

        commands.add(new BaseCommand("tempmute") {

            int convertToSeconds(String string) {
                Pattern p = Pattern.compile("^(?i)(\\d+)([a-z])?$");

                Matcher matcher = p.matcher(string);
                if (!matcher.find()) return 1;

                final String before = matcher.group(2);
                char identifier =
                        before == null ? 's' : before.toCharArray()[0];

                int number = Integer.parseInt(matcher.group(1));

                switch (identifier) {
                    case 'm':
                        number = number * 60;
                        break;
                    case 'h': case 'H':
                        number = number * 3600;
                        break;
                    case 'd': case 'D':
                        number = number * 3600 * 24;
                        break;
                    case 'w': case 'W':
                        number = number * 3600 * 24 * 7;
                        break;
                    case 'M':
                        number = number * 3600 * 24 * 30;
                        break;
                    case 'y': case 'Y':
                        number = number * 3600 * 24 * 365;
                        break;
                    case 's': default:
                        break;
                }

                return number;
            }

            @Override
            protected boolean execute(CommandSender sender, String[] args) {
                if (!isPermitted(sender)) return true;

                if (args.length < 2)
                    return createSender(sender).send("help.temp");

                SIRUser target = plugin.getUserManager().fromClosest(args[0]);
                if (target == null)
                    return createSender(sender).send("not-player");

                String reason = getLang().get(
                        "lang.default.mute-reason", "Not following server rules.");

                if (args.length > 2) {
                    String temp = LangUtils.stringFromArray(args, 2);
                    if (temp != null) reason = temp;
                }

                MessageSender message = createSender(sender)
                        .addPlaceholder("{reason}", reason)
                        .addPlaceholder("{target}", target.getName())
                        .addPlaceholder("{admin}", sender.getName());

                if (target.isMuted()) {
                    final long remaining = target.getRemainingMute();
                    String path = remaining < 1 ? "perm" : "temp";

                    if (remaining > 0)
                        message.addPlaceholder("{time}", parseTime(remaining));

                    return message.send("is-muted." + path);
                }

                final int time = convertToSeconds(args[1]);
                plugin.getUserManager().mute(target, time, sender.getName(), reason);

                return message.setTargets(Bukkit.getOnlinePlayers())
                        .addPlaceholder("{time}", parseTime(time)).send("action.temp");
            }

            @Override
            public TabBuilder getCompletionBuilder() {
                return createBasicTabBuilder()
                        .addArguments(0, getOnlineNames())
                        .addArgument(1, "<time>")
                        .addArgument(2, "<reason>");
            }
        });

        commands.add(new BaseCommand("unmute") {
            @Override
            protected boolean execute(CommandSender sender, String[] args) {
                if (!isPermitted(sender)) return true;

                if (args.length < 1)
                    return createSender(sender).send("help.unmute");

                SIRUser target = plugin.getUserManager().fromClosest(args[0]);
                if (target == null)
                    return createSender(sender).send("not-player");

                String reason = getLang().get(
                        "lang.default.unmute-reason", "Time ended.");

                if (args.length > 1) {
                    String temp = LangUtils.stringFromArray(args, 1);
                    if (temp != null) reason = temp;
                }

                MessageSender message = createSender(sender)
                        .addPlaceholder("{reason}", reason)
                        .addPlaceholder("{target}", target.getName())
                        .addPlaceholder("{admin}", sender.getName());

                if (target.isMuted()) {
                    plugin.getUserManager().unmute(target);
                    return message.send("action.unmute");
                }

                return message.send("is-muted.unmute");
            }
        });
    }

    @Getter
    private class MuteAspect implements SIRAspect {

        private final ConfigurableFile file;

        private final AspectKey key;
        private final AspectButton button;

        MuteAspect() {
            file = FileData.Command.getMain();

            SIRCommand.Key key = SIRCommand.Key.MUTE;
            this.key = key;

            (button = new AspectButton(
                    key,
                    file.get("commands.mute.enabled", true)
            )).setDefaultItems("Mute Commands");

            button.setOnClick(b -> e -> {
                file.set("commands.mute.enabled", b.isEnabled());
                file.save();

                if (b.isEnabled()) {
                    commands.forEach(SIRCommand::register);
                } else {
                    commands.forEach(SIRCommand::unregister);
                }

                String s = "Mute commands active status:" + b.isEnabled();
                SIRPlugin.getLib().getLogger().log(s);
            });

            key.setSupplier(button::isEnabled);
        }
    }

    abstract class BaseCommand extends SIRCommand {

        protected BaseCommand(String name) {
            super(aspect, name);
            setButton(aspect.getButton());
        }

        @NotNull
        protected ConfigurableFile getLang() {
            return FileData.Command.Multi.MUTE.getFile(true);
        }

        String parseTime(long remaining) {
            ConfigurationSection section = getLang().getSection("lang.time");
            return new TimeFormatter(plugin.getLibrary(),
                    section == null ?
                            null :
                            TimeValues.fromSection(section),
                    remaining).formatTime();
        }

        @Override
        public TabBuilder getCompletionBuilder() {
            return createBasicTabBuilder()
                    .addArguments(0, getOnlineNames())
                    .addArgument(1, "<reason>");
        }
    }
}
