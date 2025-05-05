package me.croabeast.sir.plugin.module;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.util.DiscordUtil;
import me.croabeast.common.applier.StringApplier;
import me.croabeast.file.Configurable;
import me.croabeast.common.util.ArrayUtils;
import me.croabeast.common.util.ReplaceUtils;
import me.croabeast.prismatic.PrismaticAPI;
import me.croabeast.sir.plugin.FileData;
import me.croabeast.sir.plugin.HookChecker;
import me.croabeast.sir.plugin.misc.FileKey;
import me.croabeast.takion.chat.MultiComponent;
import me.croabeast.takion.format.PlainFormat;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

final class DiscordHook extends SIRModule implements Actionable, HookLoadable {

    private final Map<String, List<String>> idMap = new HashMap<>();
    private final Map<String, EmbedObject> embedMap = new HashMap<>();

    private final FileKey<Object> key;

    DiscordHook() {
        super(Key.DISCORD);
        key = FileData.Module.Hook.DISCORD;
    }

    @Override
    public boolean register() {
        idMap.clear();
        embedMap.clear();

        ConfigurationSection s = key.getFile().getSection("channels");
        if (s == null) return false;

        ConfigurationSection ids = key.getFile().getSection("ids");
        if (ids != null)
            for (String key : ids.getKeys(false))
                idMap.put(key, Configurable.toStringList(ids, key));

        for (String k : s.getKeys(false)) {
            ConfigurationSection c = s.getConfigurationSection(k);
            if (c != null) embedMap.put(k, new EmbedObject(c));
        }

        return true;
    }

    @Override
    public boolean unregister() {
        return false;
    }

    @Override
    public void act(Object... objects) {
        if (!isEnabled() && !HookChecker.DISCORD_ENABLED) return;

        if (Actionable.failsCheck(objects,
                String.class, Player.class, String[].class, String[].class))
            return;

        String channel = (String) objects[0];
        Player player = (Player) objects[1];

        String[] keys = (String[]) objects[2], values = (String[]) objects[3];

        if (!idMap.containsKey(channel) ||
                !embedMap.containsKey(channel))
            return;

        List<String> ids = idMap.get(channel);
        EmbedObject object = embedMap.get(channel);

        new Sender(ids, object, player).set(keys, values).send();
    }

    @NotNull
    public String[] getSupportedPlugins() {
        return new String[] {"DiscordSRV"};
    }

    @Override
    public Plugin getHookedPlugin() {
        return Bukkit.getPluginManager().getPlugin("DiscordSRV");
    }

    @Override
    public void load() {}

    @Override
    public void unload() {}

    private class EmbedObject {

        private final String text;
        private final String color;

        private final String authorName;
        private final String authorUrl;
        private final String authorIcon;

        private final String thumbnail;

        private final String titleText;
        private final String tUrl;

        private final String description;

        private final boolean timeStamp;

        EmbedObject(ConfigurationSection section) {
            this.text = section.getString("text");
            this.color = section.getString("color");

            this.authorName = section.getString("author.name");
            this.authorUrl = section.getString("author.url");
            this.authorIcon = section.getString("author.iconURL");

            this.thumbnail = section.getString("thumbnail");

            this.titleText = section.getString("title.text");
            this.tUrl = section.getString("title.url");

            this.description = section.getString("description");
            this.timeStamp = section.getBoolean("timeStamp");
        }

        private boolean isUrl(String string) {
            return StringUtils.isNotBlank(string) && string.startsWith("https");
        }

        private Guild getGuild(String guildName) {
            final DiscordSRV srv = DiscordSRV.getPlugin();

            Guild guild = srv.getMainGuild();
            String def = key.getFile().get("default-server", "");

            try {
                return srv.getJda().getGuildById(guildName);
            } catch (Exception e) {
                try {
                    return srv.getJda().getGuildById(def);
                } catch (Exception ignored) {}
            }

            return guild;
        }

        EmbedBuilder createEmbed(UnaryOperator<String> operator) {
            final EmbedBuilder embed = new EmbedBuilder();

            int colorInt = Color.BLACK.asRGB();
            String c = this.color;

            if (StringUtils.isNotBlank(c))
                try {
                    try {
                        colorInt = java.awt.Color.decode(c).getRGB();
                    } catch (Exception e) {
                        Field color = Class
                                .forName("org.bukkit.Color")
                                .getField(c);

                        colorInt = ((Color) color.get(null)).asRGB();
                    }
                } catch (Exception ignored) {}

            embed.setColor(colorInt);

            embed.setAuthor(operator.apply(authorName),
                    isUrl(authorUrl) ? operator.apply(authorUrl) : null,
                    isUrl(authorIcon) ? operator.apply(authorIcon) : null
            );

            if (StringUtils.isNotBlank(titleText)) {
                String url = isUrl(tUrl) ? operator.apply(tUrl) : null;
                embed.setTitle(operator.apply(titleText), url);
            }

            if (StringUtils.isNotBlank(description))
                embed.setDescription(description);

            if (isUrl(thumbnail))
                embed.setThumbnail(operator.apply(thumbnail));

            if (timeStamp) embed.setTimestamp(Instant.now());
            return embed;
        }

        boolean send(List<String> ids, UnaryOperator<String> operator) {
            TextChannel main = DiscordSRV.getPlugin().getMainTextChannel();
            if (ids.isEmpty() || main != null) {
                if (StringUtils.isNotBlank(text)) {
                    main.sendMessage(operator.apply(text)).queue();
                    return true;
                }

                MessageEmbed embed = createEmbed(operator).build();
                main.sendMessageEmbeds(ArrayUtils.toList(embed)).queue();
                return true;
            }

            boolean atLeastOneMessage = false;

            for (String id : ids) {
                String guildId = null, channelId = id;

                if (id.contains(":")) {
                    String[] array = id.split(":", 2);

                    guildId = array[0];
                    channelId = array[1];
                }

                Guild guild = getGuild(guildId);

                TextChannel channel = null;
                try {
                    channel = guild.getTextChannelById(channelId);
                } catch (Exception ignored) {}

                if (channel == null) continue;

                if (StringUtils.isNotBlank(text)) {
                    channel.sendMessage(operator.apply(text)).queue();
                    if (!atLeastOneMessage) atLeastOneMessage = true;
                    continue;
                }

                MessageEmbed embed = createEmbed(operator).build();
                channel.sendMessageEmbeds(ArrayUtils.toList(embed)).queue();

                if (!atLeastOneMessage) atLeastOneMessage = true;
            }

            return atLeastOneMessage;
        }
    }

    private class Sender {

        private String[] keys = null, values = null;

        private final List<String> ids;
        private final EmbedObject object;

        private final UnaryOperator<String> operator;

        String replacePlaceholders(Player player, String s) {
            return plugin.getLibrary().getPlaceholderManager().replace(player, s);
        }

        private Sender(List<String> ids, EmbedObject object, Player player) {
            this.ids = ids;
            this.object = object;

            this.operator = string -> StringUtils.isBlank(string) ?
                    string :
                    StringApplier.simplified(string)
                            .apply(s -> ReplaceUtils.replaceEach(keys, values, s))
                            .apply(s -> replacePlaceholders(player, s))
                            .apply(s -> PlainFormat.PLACEHOLDER_API.accept(player, s))
                            .apply(PrismaticAPI::stripAll)
                            .apply(DiscordUtil::translateEmotes).toString();
        }

        Sender set(String[] keys, String[] values) {
            if (!ReplaceUtils.isApplicable(keys, values))
                return this;

            this.keys = keys;

            List<String> list = ArrayUtils.toList(values);

            list.replaceAll(MultiComponent.DEFAULT_FORMAT::removeFormat);
            list.replaceAll(operator);

            this.values = list.toArray(new String[0]);
            return this;
        }

        public void send() {
            object.send(ids, operator);
        }
    }
}
