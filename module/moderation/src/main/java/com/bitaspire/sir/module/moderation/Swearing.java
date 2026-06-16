package com.bitaspire.sir.module.moderation;

import com.bitaspire.sir.chat.ChatProcessor;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import me.croabeast.common.CollectionBuilder;
import me.croabeast.takion.logger.LogLevel;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Swearing extends Module {

    private static final Gson GSON = new Gson();
    private static final Pattern JSON_PREFIX = Pattern.compile("(?i)^\\[json] *(.*)$");
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 10000;

    private volatile List<RegexLine> lines = new ArrayList<>();
    private final Moderation moderation;

    Swearing(Moderation main) {
        super(main, "swearing");
        this.moderation = main;
    }

    @Override
    public boolean register() {
        reloadLines();
        return super.register();
    }

    @Override
    void process0(ChatProcessor.Context context) {
        String message = context.getMessage();
        boolean foundAny = false;
        boolean block = file.get("control", "BLOCK").matches("(?i)block");

        for (RegexLine line : lines) {
            Matcher matcher = line.matcher(message);

            if (block && matcher.find()) {
                foundAny = true;
                break;
            }

            while (matcher.find()) {
                List<String> list = file.toStringList("replace-options.replacements");

                String group = matcher.group();
                String replace = getReplacement(list, group);

                if (!foundAny) foundAny = true;
                context.setMessage(message = message.replace(group, replace));
            }
        }

        if (!foundAny) return;

        validateAndExecuteActions(context.getPlayer(), message, file.getConfiguration().getInt("actions.maximum-violations", 3));
        if (block) context.cancel();
    }

    private void reloadLines() {
        Set<String> words = new LinkedHashSet<>();

        for (String value : file.toStringList("banned-words")) {
            if (value == null || value.trim().isEmpty()) continue;

            Matcher matcher = JSON_PREFIX.matcher(value.trim());
            if (matcher.matches()) {
                loadJsonWords(matcher.group(1).trim(), words);
                continue;
            }

            words.add(value);
        }

        lines = CollectionBuilder.of(new ArrayList<>(words)).map(RegexLine::new).toList();
    }

    private void loadJsonWords(String source, Set<String> words) {
        if (source.isEmpty()) {
            log(LogLevel.WARN, "Ignored empty [json] source in swearing.yml.");
            return;
        }

        try {
            URI uri = URI.create(source);
            String protocol = uri.getScheme();

            if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
                log(LogLevel.WARN, "Ignored unsupported swearing JSON source: " + source);
                return;
            }

            URL url = uri.toURL();
            JsonElement element = GSON.fromJson(read(url), JsonElement.class);
            if (element == null || !element.isJsonArray()) {
                log(LogLevel.WARN, "Swearing JSON source must be a JSON array: " + source);
                return;
            }

            int loaded = 0;
            for (JsonElement child : element.getAsJsonArray()) {
                if (child == null || !child.isJsonPrimitive()) continue;

                JsonPrimitive primitive = child.getAsJsonPrimitive();
                if (primitive.isBoolean() || primitive.isNumber()) continue;

                String word = primitive.getAsString();
                if (word == null || word.trim().isEmpty()) continue;

                if (words.add(word)) loaded++;
            }

            log(LogLevel.INFO, "Loaded " + loaded + " swearing words from JSON source: " + source);
        } catch (IllegalArgumentException | IOException | JsonParseException exception) {
            log(LogLevel.WARN, "Failed to load swearing JSON source '" + source + "': " + exception.getMessage());
        }
    }

    private String read(URL url) throws IOException {
        URLConnection connection = getUrlConnection(url);

        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
        )) {
            String line;
            while ((line = reader.readLine()) != null)
                builder.append(line).append('\n');
        }

        return builder.toString();
    }

    @NotNull
    private static URLConnection getUrlConnection(URL url) throws IOException {
        URLConnection connection = url.openConnection();

        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setRequestProperty("User-Agent", "SIR-Swearing-Filter");

        if (connection instanceof HttpURLConnection) {
            HttpURLConnection http = (HttpURLConnection) connection;
            int response = http.getResponseCode();
            if (response < 200 || response >= 300)
                throw new IOException("HTTP " + response);
        }

        return connection;
    }

    private void log(LogLevel level, String message) {
        moderation.getLogger().log(level, message);
    }
}
