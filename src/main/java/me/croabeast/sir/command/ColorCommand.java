package me.croabeast.sir.command;

import lombok.Getter;
import me.croabeast.command.BaseCommand;
import me.croabeast.command.TabBuilder;
import me.croabeast.common.util.Exceptions;
import me.croabeast.file.ConfigurableFile;
import me.croabeast.sir.FileData;
import me.croabeast.sir.PAPIExpansion;
import me.croabeast.sir.user.ColorData;
import me.croabeast.sir.user.SIRUser;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;

@Getter
final class ColorCommand extends SIRCommand {

    final ConfigurableFile lang = FileData.Command.Multi.CHAT_COLOR.getFile(true);
    private Object expansion;

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
                        if (!(s instanceof Player))
                            return createSender(s).send("need-player");

                        SIRUser user = plugin.getUserManager().getUser(s);
                        if (user == null)
                            return checkPlayer(s, s.getName());

                        ColorData data = user.getColorData();
                        Set<String> formats = data.getFormats();

                        String to = color.toString();
                        boolean added = !formats.remove(to) && formats.add(to);

                        String path = added ? "add" : "remove";
                        return createSender(s)
                                .addPlaceholder("{format}", to + name)
                                .send("format." + path);
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
                    editSubCommand(name, (s, a) -> {
                        if (!(s instanceof Player))
                            return createSender(s).send("need-player");

                        SIRUser user = plugin.getUserManager().getUser(s);
                        if (user == null)
                            return checkPlayer(s, s.getName());

                        user.getColorData().setColorStart(color.toString());
                        return createSender(s)
                                .addPlaceholder("{color}", color + name)
                                .send("color");
                    });
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

        editSubCommand("gradient", (s, a) -> {
            if (!(s instanceof Player))
                return createSender(s).send("need-player");

            if (a.length != 2)
                return createSender(s).send("gradient.usage");

            SIRUser user = plugin.getUserManager().getUser(s);
            if (user == null)
                return checkPlayer(s, s.getName());

            ColorData data = user.getColorData();
            data.setColorStart(a[0]);
            data.setColorEnd(a[1]);

            return createSender(s)
                    .addPlaceholder("{gradient}", a[0])
                    .addPlaceholder("{end}", a[1])
                    .send("gradient.success");
        });

        editSubCommand("rainbow", (s, a) -> {
            if (!(s instanceof Player))
                return createSender(s).send("need-player");

            SIRUser user = plugin.getUserManager().getUser(s);
            if (user == null)
                return checkPlayer(s, s.getName());

            user.getColorData().setColorStart("<R:1>");
            user.getColorData().setColorEnd("</R>");

            return createSender(s)
                    .addPlaceholder("{rainbow}", "<R:1>")
                    .addPlaceholder("{end}", "</R>")
                    .send("rainbow");
        });

        if (!Exceptions.isPluginEnabled("PlaceholderAPI")) return;

        expansion = new PAPIExpansion("sir_color") {
            @Nullable
            public String onRequest(OfflinePlayer off, @NotNull String params) {
                Player player = off.getPlayer();
                if (player == null) {
                    ConfigurationSection section = FileData.Command.Multi
                            .CHAT_COLOR.getFile(false)
                            .getSection(off.getUniqueId().toString());
                    if (section == null) return null;

                    if (params.matches("(?i)start"))
                        return section.getString("start", "");
                    else if (params.matches("(?i)end"))
                        return section.getString("end");
                    return null;
                }

                SIRUser user = plugin.getUserManager().getUser(player);
                if (user == null) return null;

                ColorData data = user.getColorData();
                if (params.matches("(?i)start"))
                    return data.getStart();
                else if (params.matches("(?i)end"))
                    return data.getEnd();

                return null;
            }
        };
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

        return builder.addArguments(1,
                        (s, a) -> a[0].matches("(?i)gradient"),
                        "<#FFFFFF>", "<#FFFFFFF:#FFFFFF>"
                )
                .addArgument(2,
                        (s, a) -> a[0].matches("(?i)gradient"),
                        "</#FFFFFF>"
                );
    }

    void callExpansion(boolean register) {
        if (expansion != null)
            if (register) {
                ((PAPIExpansion) expansion).register();
            } else {
                ((PAPIExpansion) expansion).unregister();
            }
    }

    @Override
    public boolean register(boolean async) {
        boolean registered = super.register(async);
        if (registered)
            callExpansion(true);
        return registered;
    }

    @Override
    public boolean unregister(boolean async) {
        boolean unregistered = super.unregister(async);
        if (unregistered)
            callExpansion(false);
        return unregistered;
    }
}
