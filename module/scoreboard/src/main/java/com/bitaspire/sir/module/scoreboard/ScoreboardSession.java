package com.bitaspire.sir.module.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"deprecation"})
final class ScoreboardSession {

    private static final String[] HIDDEN_ENTRIES = {
            "\u00A70", "\u00A71", "\u00A72", "\u00A73", "\u00A74",
            "\u00A75", "\u00A76", "\u00A77", "\u00A78", "\u00A79",
            "\u00A7a", "\u00A7b", "\u00A7c", "\u00A7d", "\u00A7e"
    };

    private final Scoreboard scoreboard;
    private final Objective objective;
    private final String[] entries = new String[15];
    private final String[] lastPrefixes = new String[15];
    private final String[] lastSuffixes = new String[15];
    private final boolean[] lastVisible = new boolean[15];
    private final int[] lastScores = new int[15];
    private String lastTitle;

    ScoreboardSession(Player player) {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective("sir_sidebar", "dummy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        Arrays.fill(lastScores, Integer.MIN_VALUE);

        for (int i = 0; i < entries.length; i++) {
            entries[i] = HIDDEN_ENTRIES[i];
            Team team = scoreboard.registerNewTeam("sir_sb_" + i);
            team.addEntry(entries[i]);
        }

        player.setScoreboard(scoreboard);
    }

    void render(Player player, String title, List<String> lines, int maxLines, boolean legacySplit) {
        if (player.getScoreboard() != scoreboard)
            player.setScoreboard(scoreboard);

        String displayTitle = LegacyScoreboardLine.trim(title, 32);
        if (!displayTitle.equals(lastTitle)) {
            objective.setDisplayName(displayTitle);
            lastTitle = displayTitle;
        }

        int visible = Math.min(Math.min(lines.size(), maxLines), entries.length);
        for (int i = 0; i < entries.length; i++) {
            Team team = team(i);
            String entry = entries[i];

            if (i >= visible) {
                hideLine(i, team, entry);
                continue;
            }

            String line = lines.get(i);
            String prefix;
            String suffix;
            if (legacySplit) {
                LegacyScoreboardLine.Parts parts = LegacyScoreboardLine.split(line);
                prefix = parts.prefix;
                suffix = parts.suffix;
            } else {
                prefix = LegacyScoreboardLine.trim(line, 64);
                suffix = "";
            }

            renderLine(i, team, entry, prefix, suffix, visible - i);
        }
    }

    private Team team(int index) {
        Team team = scoreboard.getTeam("sir_sb_" + index);
        if (team != null) return team;

        team = scoreboard.registerNewTeam("sir_sb_" + index);
        team.addEntry(entries[index]);
        return team;
    }

    private void hideLine(int index, Team team, String entry) {
        if (lastVisible[index]) {
            scoreboard.resetScores(entry);
            lastVisible[index] = false;
            lastScores[index] = Integer.MIN_VALUE;
        }

        if (lastPrefixes[index] != null || lastSuffixes[index] != null)
            updateTeamText(index, team, "", "");
    }

    private void renderLine(int index, Team team, String entry, String prefix, String suffix, int score) {
        updateTeamText(index, team, prefix, suffix);
        if (!lastVisible[index] || lastScores[index] != score) {
            objective.getScore(entry).setScore(score);
            lastScores[index] = score;
        }

        lastVisible[index] = true;
    }

    private void updateTeamText(int index, Team team, String prefix, String suffix) {
        if (!prefix.equals(lastPrefixes[index])) {
            team.setPrefix(prefix);
            lastPrefixes[index] = prefix;
        }

        if (!suffix.equals(lastSuffixes[index])) {
            team.setSuffix(suffix);
            lastSuffixes[index] = suffix;
        }
    }

    void close(Player player) {
        if (player.getScoreboard() == scoreboard)
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
}
