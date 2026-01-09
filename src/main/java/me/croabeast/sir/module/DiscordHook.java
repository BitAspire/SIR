package me.croabeast.sir.module;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.util.DiscordUtil;
import lombok.Getter;
import me.croabeast.common.CollectionBuilder;
import me.croabeast.common.applier.StringApplier;
import me.croabeast.common.reflect.Reflector;
import me.croabeast.common.util.Exceptions;
import me.croabeast.file.Configurable;
import me.croabeast.common.util.ArrayUtils;
import me.croabeast.common.util.ReplaceUtils;
import me.croabeast.prismatic.PrismaticAPI;
import me.croabeast.sir.FileData;
import me.croabeast.sir.SIRPlugin;
import me.croabeast.sir.misc.FileKey;
import me.croabeast.takion.chat.MultiComponent;
import me.croabeast.takion.format.PlainFormat;
import net.essentialsx.api.v2.ChatType;
import net.essentialsx.api.v2.events.discord.DiscordChatMessageEvent;
import net.essentialsx.discord.EssentialsDiscord;
import net.essentialsx.discord.JDADiscordService;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

final class DiscordHook extends SIRModule implements Actionable, HookLoadable {

    private final Map<String, List<String>> idMap = new HashMap<>();
    private final Map<String, EmbedObject> embedMap = new HashMap<>();

    @Getter
    private final String[] supportedPlugins = {"DiscordSRV", "EssentialsDiscord"};
    boolean restricted;

    private final FileKey<Object> key;
    private final List<Plugin> loadedHooks;

    DiscordHook() {
        super(Key.DISCORD);
        loadedHooks = CollectionBuilder.of(supportedPlugins)
                .filter(Exceptions::isPluginEnabled)
                .map(Bukkit.getPluginManager()::getPlugin).toList();

        restricted = loadedHooks.size() == 1 &&
                loadedHooks.get(0).getName().equals("EssentialsDiscord");
        key = FileData.Module.Hook.DISCORD;
    }

    @Override
    public boolean register() {
        if (restricted) return false;

        idMap.clear();
        embedMap.clear();

        ConfigurationSection ids = key.getFile().getSection("ids");
        if (ids != null)
            for (String key : ids.getKeys(false))
                idMap.put(key, Configurable.toStringList(ids, key));

        ConfigurationSection s = key.getFile().getSection("channels");
        if (s == null) return false;

        for (String k : s.getKeys(false)) {
            ConfigurationSection c = s.getConfigurationSection(k);
            if (c != null) embedMap.put(k, new EmbedObject(c));
        }

        return true;
    }

    @Override
    public boolean unregister() {
        return restricted;
    }

    @Override
    public void accept(Object... objects) {
        if (!isEnabled() && !isPluginEnabled()) return;

        if (Actionable.failsCheck(objects,
                String.class, Player.class, String[].class, String[].class))
            return;

        String channel = (String) objects[0];
        Player player = (Player) objects[1];

        String[] keys = (String[]) objects[2], values = (String[]) objects[3];

        if (restricted) {
            EssentialsHolder.send(player, keys, values);
            return;
        }

        if (!idMap.containsKey(channel) || !embedMap.containsKey(channel))
            return;

        List<String> ids = idMap.get(channel);
        EmbedObject object = embedMap.get(channel);

        new Sender(ids, object, player).set(keys, values).send();
    }

    @Override
    public Plugin getHookedPlugin() {
        return loadedHooks.size() != 1 ? null : loadedHooks.get(0);
    }

    @Override
    public void load() {}

    @Override
    public void unload() {}

    static String replacePlaceholders(Player player, String s) {
        return SIRPlugin.getLib().getPlaceholderManager().replace(player, s);
    }

    static class EssentialsHolder {

        static void send(Player player, String[] keys, String[] values) {
            SIRPlugin.getScheduler().runTask(() -> {
                EssentialsDiscord discord;
                try {
                    discord = JavaPlugin.getPlugin(EssentialsDiscord.class);
                } catch (Exception e) {
                    return;
                }

                JDADiscordService service = Reflector.from(() -> discord).get("jda");
                if (service == null) return;

                Object jda = Reflector.from(() -> service).get("jda");
                if (jda == null) return;

                String message = StringUtils.isBlank(values[5]) ? "" :
                        StringApplier.simplified(values[5])
                                .apply(s -> ReplaceUtils.replaceEach(keys, values, s))
                                .apply(s -> replacePlaceholders(player, s))
                                .apply(s -> PlainFormat.PLACEHOLDER_API.accept(player, s))
                                .apply(PrismaticAPI::stripAll).toString();

                if (StringUtils.isBlank(message)) return;

                try {
                    DiscordChatMessageEvent event =
                            new DiscordChatMessageEvent(player, message, ChatType.UNKNOWN);
                    Bukkit.getPluginManager().callEvent(event);
                    service.sendChatMessage(player, event.getMessage());
                } catch (Exception e) {
                    // JDA service is not ready or disconnected, silently ignore
                }
            });
        }
    }

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

            this.color = section.getString("embed.color");

            this.authorName = section.getString("embed.author.name");
            this.authorUrl = section.getString("embed.author.url");
            this.authorIcon = section.getString("embed.author.iconURL");

            this.thumbnail = section.getString("embed.thumbnail");

            if (section.isConfigurationSection("embed.title")) {
                this.titleText = section.getString("embed.title.text");
                this.tUrl = section.getString("embed.title.url");
            } else {
                this.titleText = section.getString("embed.title");
                this.tUrl = null;
            }

            this.description = section.getString("embed.description");
            this.timeStamp = section.getBoolean("embed.timeStamp");
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

            if (StringUtils.isNotBlank(description)) {
                String desc = operator.apply(description);
                embed.setDescription(StringUtils.isNotBlank(desc) ? desc : "");
            }

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
            list.replaceAll(s -> StringUtils.isBlank(s) ?
                    s :
                    StringApplier.simplified(s)
                            .apply(MultiComponent.DEFAULT_FORMAT::removeFormat)
                            .apply(operator).toString());

            this.values = list.toArray(new String[0]);
            return this;
        }

        public void send() {
            object.send(ids, operator);
        }
    }
}
