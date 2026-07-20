package com.bitaspire.sir.module.discord;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.WebhookUtil;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Color;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

@UtilityClass
class DiscordSrv {

    // DiscordSRV drops webhook messages with blank content, even if they carry embeds.
    private final String WEBHOOK_BLANK_CONTENT = "\u200B";

    String translateEmotes(String string) {
        try {
            return DiscordUtil.translateEmotes(string);
        } catch (Throwable ignored) {
            return string;
        }
    }

    void send(Player player, List<String> ids, Config.EmbedTemplate template,
              UnaryOperator<String> formatter) {
        try {
            List<TextChannel> channels = resolveChannels(ids, template.defaultServer);
            if (channels.isEmpty()) return;

            String formattedName = StringUtils.isNotBlank(template.name) ? formatter.apply(template.name) : null;
            String formattedText = StringUtils.isNotBlank(template.text) ? formatter.apply(template.text) : null;
            MessageEmbed embed = template.enabled ? createEmbed(template, formatter).build() : null;

            for (TextChannel channel : channels)
                sendToChannel(channel, player, template.playerWebhook, formattedName, formattedText, embed);
        } catch (Throwable ignored) {}
    }

    private Guild getGuild(String guildName, String defaultServer) {
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

    private EmbedBuilder createEmbed(Config.EmbedTemplate template, UnaryOperator<String> operator) {
        EmbedBuilder embed = new EmbedBuilder();

        int colorInt = Color.BLACK.asRGB();
        String c = template.color;

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

        Config.EmbedTemplate.Author author = template.author;
        if (author != null)
            embed.setAuthor(
                    operator.apply(author.name),
                    isUrl(author.url) ? operator.apply(author.url) : null,
                    isUrl(author.iconUrl) ? operator.apply(author.iconUrl) : null
            );

        if (StringUtils.isNotBlank(template.titleText)) {
            String url = isUrl(template.titleUrl) ? operator.apply(template.titleUrl) : null;
            embed.setTitle(operator.apply(template.titleText), url);
        }

        if (StringUtils.isNotBlank(template.description))
            embed.setDescription(operator.apply(template.description));

        if (isUrl(template.thumbnail))
            embed.setThumbnail(operator.apply(template.thumbnail));

        if (template.timeStamp) embed.setTimestamp(Instant.now());

        return embed;
    }

    private boolean isUrl(String string) {
        return StringUtils.isNotBlank(string) && string.startsWith("https");
    }

    private void sendToChannel(TextChannel channel, Player player, boolean playerWebhook, String formattedName,
                               String formattedText, MessageEmbed embed) {
        if (playerWebhook && player != null) {
            sendViaWebhook(channel, player, formattedName, formattedText, embed);
            return;
        }

        if (StringUtils.isNotBlank(formattedText))
            channel.sendMessage(formattedText).queue();

        if (embed != null)
            channel.sendMessageEmbeds(Collections.singletonList(embed)).queue();
    }

    private void sendViaWebhook(TextChannel channel, Player player, String formattedName, String formattedText,
                                MessageEmbed embed) {
        boolean hasText = StringUtils.isNotBlank(formattedText);
        if (!hasText && embed == null) return;

        Collection<MessageEmbed> embeds = embed == null
                ? Collections.emptyList()
                : Collections.singletonList(embed);

        WebhookUtil.deliverMessage(
                channel,
                StringUtils.defaultIfBlank(formattedName, player.getName()),
                DiscordSRV.getAvatarUrl(player),
                hasText ? formattedText : WEBHOOK_BLANK_CONTENT,
                embeds
        );
    }

    private List<TextChannel> resolveChannels(List<String> ids, String defaultServer) {
        List<TextChannel> channels = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        TextChannel main = DiscordSRV.getPlugin().getMainTextChannel();
        addChannel(channels, seen, main);

        for (String id : ids) {
            String guildId = null, channelId = id;

            if (id.contains(":")) {
                String[] array = id.split(":", 2);
                guildId = array[0];
                channelId = array[1];
            }

            TextChannel channel = null;
            try {
                Guild guild = getGuild(guildId, defaultServer);
                if (guild != null)
                    channel = guild.getTextChannelById(channelId);
            } catch (Exception ignored) {}

            addChannel(channels, seen, channel);
        }

        return channels;
    }

    private void addChannel(List<TextChannel> channels, Set<String> seen, TextChannel channel) {
        if (channel != null && seen.add(channel.getId())) channels.add(channel);
    }
}
