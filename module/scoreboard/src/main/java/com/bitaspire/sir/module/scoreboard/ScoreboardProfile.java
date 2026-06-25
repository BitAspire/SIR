package com.bitaspire.sir.module.scoreboard;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
final class ScoreboardProfile {

    private final String name;
    private final int priority;
    private final List<String> worlds;
    private final List<String> permissions;
    private final AnimatedText title;
    private final AnimatedLines lines;

    static ScoreboardProfile from(String name, ConfigurationSection section) {
        ConfigurationSection conditions = section.getConfigurationSection("conditions");
        List<String> worlds = conditions == null ? Collections.emptyList() : conditions.getStringList("worlds");
        List<String> permissions = conditions == null ? Collections.emptyList() : conditions.getStringList("permissions");

        return new ScoreboardProfile(
                name,
                section.getInt("priority", 0),
                worlds,
                permissions,
                AnimatedText.from(section.getConfigurationSection("title"), name),
                AnimatedLines.from(section.getConfigurationSection("lines"))
        );
    }

    boolean hasAnimations() {
        return title.isAnimated() || lines.isAnimated();
    }

    String title(Player player, ScoreboardModule module, ScoreboardRefreshContext context) {
        return ScoreboardPlaceholders.apply(module, player, context, title.frame(context.getTick()));
    }

    List<String> lines(Player player, ScoreboardModule module, ScoreboardRefreshContext context) {
        List<String> rendered = new ArrayList<>();
        for (String line : lines.frame(context.getTick()))
            rendered.add(ScoreboardPlaceholders.apply(module, player, context, line));
        return rendered;
    }

    boolean matches(ScoreboardModule module, Player player) {
        if (!worlds.isEmpty() && !containsIgnoreCase(worlds, player.getWorld().getName()))
            return false;

        for (String permission : permissions) {
            if (StringUtils.isBlank(permission)) continue;
            if (!module.getApi().getUserManager().hasPermission(player, permission))
                return false;
        }

        return true;
    }

    private static boolean containsIgnoreCase(List<String> values, String target) {
        return values.stream().anyMatch(value -> value != null && value.equalsIgnoreCase(target));
    }

    private static int ticks(ConfigurationSection section, String... paths) {
        for (String path : paths) {
            if (section.contains(path))
                return Math.max(1, section.getInt(path, 20));
        }

        return Math.max(1, 20);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class AnimatedText {
        private final boolean animated;
        private final int intervalTicks;
        private final List<String> frames;

        static AnimatedText from(ConfigurationSection section, String fallback) {
            if (section == null)
                return create(false, 20, Collections.singletonList(fallback));

            boolean animated = section.getBoolean("animated", false);
            int intervalTicks = ticks(section, "interval-ticks", "interval");
            List<String> frames = section.getStringList("frames");
            if (frames.isEmpty() && StringUtils.isNotBlank(section.getString("value")))
                frames.add(section.getString("value"));

            return create(animated, intervalTicks, frames);
        }

        private static AnimatedText create(boolean animated, int intervalTicks, List<String> frames) {
            return new AnimatedText(animated, Math.max(1, intervalTicks), frames.isEmpty() ? Collections.singletonList("") : frames);
        }

        boolean isAnimated() {
            return animated && frames.size() > 1;
        }

        String frame(long tick) {
            if (!isAnimated()) return frames.get(0);
            int index = (int) ((tick / intervalTicks) % frames.size());
            return frames.get(index);
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class AnimatedLines {
        private final boolean animated;
        private final int intervalTicks;
        private final List<List<String>> frames;

        static AnimatedLines from(ConfigurationSection section) {
            if (section == null)
                return create(false, 20, Collections.emptyList());

            boolean animated = section.getBoolean("animated", false);
            int intervalTicks = ticks(section, "interval-ticks", "interval", "update-ticks");
            List<List<String>> frames = new ArrayList<>();

            if (animated) {
                List<?> rawFrames = section.getList("frames");
                if (rawFrames != null) {
                    for (Object raw : rawFrames) {
                        if (raw instanceof List) {
                            List<String> frame = new ArrayList<>();
                            for (Object line : (List<?>) raw)
                                frame.add(String.valueOf(line));
                            frames.add(frame);
                        }
                    }
                }
            }

            if (frames.isEmpty())
                frames.add(section.getStringList("value"));

            return create(animated, intervalTicks, frames);
        }

        private static AnimatedLines create(boolean animated, int intervalTicks, List<List<String>> frames) {
            return new AnimatedLines(animated, Math.max(1, intervalTicks),
                    frames.isEmpty() ? Collections.singletonList(Collections.emptyList()) : frames);
        }

        boolean isAnimated() {
            return animated && frames.size() > 1;
        }

        List<String> frame(long tick) {
            if (!isAnimated()) return frames.get(0);
            int index = (int) ((tick / intervalTicks) % frames.size());
            return frames.get(index);
        }
    }
}
