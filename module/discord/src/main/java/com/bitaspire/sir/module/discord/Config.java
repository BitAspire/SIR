package com.bitaspire.sir.module.discord;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.WebhookUtil;
import lombok.SneakyThrows;
import me.croabeast.common.applier.StringApplier;
import me.croabeast.file.Configurable;
import me.croabeast.prismatic.PrismaticAPI;
import com.bitaspire.sir.SIRApi;
import com.bitaspire.sir.file.ExtensionFile;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.*;
import java.util.function.UnaryOperator;

final class Config {

    private final String defaultServer;
    final boolean restricted;

    private final Map<String, List<String>> channelIds = new HashMap<>();
    private final Map<String, EmbedTemplate> embeds = new HashMap<>();

    @SneakyThrows
    Config(Discord main, boolean restricted) {
        this.restricted = restricted;
        ExtensionFile file = new ExtensionFile(main, "config", true);
        defaultServer = file.get("default-server", new Object()).toString();
        loadEmbeds(file);
        loadChannelIds(file);
    }

    private void loadChannelIds(ExtensionFile file) {
        ConfigurationSection idsSection = file.getSection("channel-ids");
        if (idsSection == null) return;

        for (String key : idsSection.getKeys(false))
            channelIds.put(key, Configurable.toStringList(idsSection, key));
    }

    private void loadEmbeds(ExtensionFile file) {
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

    private static final class EmbedTemplate {

        // DiscordSRV drops webhook messages with blank content, even if they carry embeds.
        private static final String WEBHOOK_BLANK_CONTENT = "\u200B";

        private final boolean restricted;
        private final String defaultServer;

        private final boolean enabled;
        private final boolean playerWebhook;
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

            this.enabled = section.getBoolean("embed.enabled");
            this.playerWebhook = section.getBoolean("player-webhook");
            this.timeStamp = section.getBoolean("embed.timestamp");
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
                embed.setDescription(operator.apply(description));

            if (isUrl(thumbnail))
                embed.setThumbnail(operator.apply(thumbnail));

            if (timeStamp) embed.setTimestamp(Instant.now());

            return embed;
        }

        private static UnaryOperator<String> createFormatter(Player player, UnaryOperator<String> base) {
            return string -> {
                if (StringUtils.isBlank(string)) return string;

                return StringApplier.simplified(string).apply(base)
                        .apply(s -> SIRApi.instance().getLibrary().replace(player, s, false))
                        .apply(PrismaticAPI::stripAll)
                        .apply(DiscordUtil::translateEmotes)
                        .toString();
            };
        }

        void send(Player player, List<String> ids, UnaryOperator<String> operator) {
            UnaryOperator<String> formatter = createFormatter(player, operator);
            if (!restricted) {
                sendViaDiscordSRV(player, ids, formatter);
                return;
            }
            sendViaEssentialsDiscord(player, formatter);
        }

        private void sendViaEssentialsDiscord(Player player, UnaryOperator<String> formatter) {
            SIRApi.instance().getScheduler().runTask(() -> Essentials.send(player, text, formatter));
        }

        private void sendViaDiscordSRV(Player player, List<String> ids, UnaryOperator<String> formatter) {
            List<TextChannel> channels = resolveChannels(ids);
            if (channels.isEmpty()) return;

            String formattedText = StringUtils.isNotBlank(text) ? formatter.apply(text) : null;
            MessageEmbed embed = enabled ? createEmbed(formatter).build() : null;

            for (TextChannel channel : channels)
                sendToChannel(channel, player, formattedText, embed);
        }

        private void sendToChannel(TextChannel channel, Player player, String formattedText, MessageEmbed embed) {
            if (playerWebhook && player != null) {
                sendViaWebhook(channel, player, formattedText, embed);
                return;
            }

            if (StringUtils.isNotBlank(formattedText))
                channel.sendMessage(formattedText).queue();

            if (embed != null)
                channel.sendMessageEmbeds(Collections.singletonList(embed)).queue();
        }

        private void sendViaWebhook(TextChannel channel, Player player, String formattedText, MessageEmbed embed) {
            boolean hasText = StringUtils.isNotBlank(formattedText);
            if (!hasText && embed == null) return;

            Collection<MessageEmbed> embeds = embed == null
                    ? Collections.emptyList()
                    : Collections.singletonList(embed);

            WebhookUtil.deliverMessage(
                    channel,
                    player.getName(),
                    DiscordSRV.getAvatarUrl(player),
                    hasText ? formattedText : WEBHOOK_BLANK_CONTENT,
                    embeds
            );
        }

        private List<TextChannel> resolveChannels(List<String> ids) {
            List<TextChannel> channels = new ArrayList<>();

            TextChannel main = DiscordSRV.getPlugin().getMainTextChannel();
            if (main != null) channels.add(main);

            for (String id : ids) {
                String guildId = null, channelId = id;

                if (id.contains(":")) {
                    String[] array = id.split(":", 2);
                    guildId = array[0];
                    channelId = array[1];
                }

                TextChannel channel = null;
                try {
                    Guild guild = getGuild(guildId);
                    if (guild != null)
                        channel = guild.getTextChannelById(channelId);
                } catch (Exception ignored) {}

                if (channel != null) channels.add(channel);
            }

            return channels;
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
