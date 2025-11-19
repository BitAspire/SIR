package me.croabeast.sir.module.discord;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.util.DiscordUtil;
import me.croabeast.common.applier.StringApplier;
import me.croabeast.common.reflect.Reflector;
import me.croabeast.common.util.ArrayUtils;
import me.croabeast.file.Configurable;
import me.croabeast.prismatic.PrismaticAPI;
import me.croabeast.sir.SIRApi;
import me.croabeast.sir.module.ModuleFile;
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
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.*;
import java.util.function.UnaryOperator;

final class Config {

    private String defaultServer = "";
    private final boolean restricted;

    private final Map<String, List<String>> channelIds = new HashMap<>();
    private final Map<String, EmbedTemplate> embeds = new HashMap<>();

    Config(Discord main, boolean restricted) {
        this.restricted = restricted;
        load(main);
    }

    private void load(Discord main) {
        try {
            ModuleFile file = new ModuleFile(main, "config");
            defaultServer = file.get("default-server", "");
            loadEmbeds(file);
            loadChannelIds(file);
        } catch (Exception ignored) {}
    }

    private void loadChannelIds(ModuleFile file) {
        ConfigurationSection idsSection = file.getSection("channel-ids");
        if (idsSection == null) return;

        for (String key : idsSection.getKeys(false))
            channelIds.put(key, Configurable.toStringList(idsSection, key));
    }

    private void loadEmbeds(ModuleFile file) {
        ConfigurationSection messagesSection = file.getSection("messages");
        if (messagesSection == null) return;

        for (String key : messagesSection.getKeys(false)) {
            ConfigurationSection section = messagesSection.getConfigurationSection(key);
            if (section != null)
                embeds.put(key, new EmbedTemplate(section, restricted, defaultServer));
        }
    }

    void send(String channel, Player player, UnaryOperator<String> operator) {
        List<String> ids = channelIds.getOrDefault(channel, new ArrayList<>());
        EmbedTemplate template = embeds.get(channel);
        if (template != null) template.send(player, ids, operator);
    }

    static String replace(Player player, String s) {
        return SIRApi.instance().getLibrary().getPlaceholderManager().replace(player, s);
    }

    private static final class EmbedTemplate {

        private final boolean restricted;
        private final String defaultServer;

        private final boolean enabled;
        private final boolean timeStamp;

        private final Author author;

        private final String text;
        private final String color;
        private final String thumbnail;
        private final String description;
        private final String titleText;
        private final String titleUrl;

        EmbedTemplate(ConfigurationSection section, boolean restricted, String defaultServer) {
            this.restricted = restricted;
            this.defaultServer = defaultServer;

            this.text = section.getString("text");

            ConfigurationSection authorSection = section.getConfigurationSection("embed.author");
            this.author = authorSection != null ? new Author(authorSection) : null;

            this.color = section.getString("embed.color");
            this.thumbnail = section.getString("embed.thumbnail");
            this.description = section.getString("embed.description");
            this.titleText = section.getString("embed.title.text");
            this.titleUrl = section.getString("embed.title.url");

            this.enabled = section.getBoolean("embed.enabled", false);
            this.timeStamp = section.getBoolean("embed.timestamp", false);
        }

        private static boolean isUrl(String string) {
            return StringUtils.isNotBlank(string) && string.startsWith("https");
        }

        private Guild getGuild(String guildName) {
            DiscordSRV srv = DiscordSRV.getPlugin();
            Guild fallback = srv.getMainGuild();

            try {
                if (StringUtils.isNotBlank(guildName)) {
                    Guild byId = srv.getJda().getGuildById(guildName);
                    if (byId != null) return byId;
                }
            } catch (Exception ignored) {}

            try {
                if (StringUtils.isNotBlank(defaultServer)) {
                    Guild byId = srv.getJda().getGuildById(defaultServer);
                    if (byId != null) return byId;
                }
            } catch (Exception ignored) {}

            return fallback;
        }

        private EmbedBuilder createEmbed(UnaryOperator<String> operator) {
            EmbedBuilder embed = new EmbedBuilder();

            int colorInt = Color.BLACK.asRGB();
            String c = this.color;

            if (StringUtils.isNotBlank(c))
                try {
                    try {
                        colorInt = java.awt.Color.decode(c).getRGB();
                    } catch (Exception e) {
                        Field colorField = Class
                                .forName("org.bukkit.Color")
                                .getField(c);

                        colorInt = ((Color) colorField.get(null)).asRGB();
                    }
                } catch (Exception ignored) {}

            embed.setColor(colorInt);

            if (author != null)
                embed.setAuthor(
                        operator.apply(author.name),
                        isUrl(author.url) ? operator.apply(author.url) : null,
                        isUrl(author.iconUrl) ? operator.apply(author.iconUrl) : null
                );

            if (StringUtils.isNotBlank(titleText)) {
                String url = isUrl(titleUrl) ? operator.apply(titleUrl) : null;
                embed.setTitle(operator.apply(titleText), url);
            }

            if (StringUtils.isNotBlank(description))
                embed.setDescription(description);

            if (isUrl(thumbnail))
                embed.setThumbnail(operator.apply(thumbnail));

            if (timeStamp)
                embed.setTimestamp(Instant.now());

            return embed;
        }

        private static UnaryOperator<String> createFormatter(Player player, UnaryOperator<String> base) {
            return string -> {
                if (StringUtils.isBlank(string)) return string;

                return StringApplier.simplified(string)
                        .apply(base)
                        .apply(s -> Config.replace(player, s))
                        .apply(s -> PlainFormat.PLACEHOLDER_API.accept(player, s))
                        .apply(PrismaticAPI::stripAll)
                        .apply(DiscordUtil::translateEmotes)
                        .toString();
            };
        }

        void send(Player player, List<String> ids, UnaryOperator<String> operator) {
            UnaryOperator<String> formatter = createFormatter(player, operator);

            if (!restricted) {
                sendViaDiscordSRV(ids, formatter);
                return;
            }

            sendViaEssentialsDiscord(player, formatter);
        }

        private void sendViaEssentialsDiscord(Player player, UnaryOperator<String> formatter) {
            SIRApi.instance().getScheduler().runTask(() -> {
                EssentialsDiscord discord;
                try {
                    discord = JavaPlugin.getPlugin(EssentialsDiscord.class);
                } catch (Exception e) {
                    return;
                }

                JDADiscordService service = Reflector.from(() -> discord).get("jda");
                String message = StringApplier.simplified(text).apply(formatter).toString();

                DiscordChatMessageEvent event = new DiscordChatMessageEvent(player, message, ChatType.UNKNOWN);
                Bukkit.getPluginManager().callEvent(event);

                service.sendChatMessage(player, event.getMessage());
            });
        }

        private void sendViaDiscordSRV(List<String> ids, UnaryOperator<String> formatter) {
            TextChannel main = DiscordSRV.getPlugin().getMainTextChannel();
            if (ids.isEmpty() || main != null) {
                if (StringUtils.isNotBlank(text))
                    main.sendMessage(formatter.apply(text)).queue();

                if (enabled) {
                    MessageEmbed embed = createEmbed(formatter).build();
                    main.sendMessageEmbeds(ArrayUtils.toList(embed)).queue();
                }
                return;
            }

            for (String id : ids) {
                String guildId = null;
                String channelId = id;

                if (id.contains(":")) {
                    String[] array = id.split(":", 2);
                    guildId = array[0];
                    channelId = array[1];
                }

                Guild guild = getGuild(guildId);

                TextChannel channel = null;
                try {
                    if (guild != null) channel = guild.getTextChannelById(channelId);
                } catch (Exception ignored) {}

                if (channel == null) continue;

                if (StringUtils.isNotBlank(text))
                    channel.sendMessage(formatter.apply(text)).queue();

                if (enabled) {
                    MessageEmbed embed = createEmbed(formatter).build();
                    channel.sendMessageEmbeds(ArrayUtils.toList(embed)).queue();
                }
            }
        }

        private static final class Author {

            private final String name, url, iconUrl;

            Author(ConfigurationSection section) {
                this.name = section.getString("name", "");
                this.url = section.getString("url");
                this.iconUrl = section.getString("icon-url");
            }
        }
    }
}
