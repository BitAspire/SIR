package com.bitaspire.sir;

import lombok.experimental.UtilityClass;
import org.bukkit.Sound;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Utility methods for parsing Bukkit {@link Sound} values across server versions.
 *
 * <p> Supports both legacy enum-based sounds (pre-1.21) and the registry-based API
 * (1.21+) by attempting enum lookup first and falling back to reflection-based
 * registry lookup.
 */
@UtilityClass
public class SoundUtils {

    /**
     * Parses a sound from a raw string, supporting both legacy enum names and
     * namespaced key formats (e.g., {@code BLOCK_NOTE_BLOCK_PLING} or {@code minecraft:block.note_block.pling}).
     *
     * @param rawSound the raw sound string, or {@code null}.
     * @return the corresponding {@link Sound}, or {@code null} if not recognized.
     */
    @Nullable
    public Sound parseSound(@Nullable String rawSound) {
        if (rawSound == null || rawSound.trim().isEmpty())
            return null;

        Sound fromEnum = parseEnum(rawSound);
        if (fromEnum != null)
            return fromEnum;

        return parseRegistry(rawSound);
    }

    @Nullable
    private Sound parseEnum(String rawSound) {
        if (!Sound.class.isEnum())
            return null;

        try {
            String normalized = rawSound.trim().toUpperCase(Locale.ROOT);
            return Enum.valueOf(Sound.class, normalized);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private Sound parseRegistry(String rawSound) {
        try {
            Class<?> registryClass = Class.forName("org.bukkit.Registry");
            Class<?> keyClass = Class.forName("org.bukkit.NamespacedKey");
            Object registry;

            try {
                registry = registryClass.getField("SOUNDS").get(null);
            } catch (NoSuchFieldException ignored) {
                registry = registryClass.getField("SOUND").get(null);
            }

            if (registry == null) return null;

            Object key = toKey(rawSound, keyClass);
            if (key == null) return null;

            Method getMethod = registry.getClass().getMethod("get", keyClass);
            return (Sound) getMethod.invoke(registry, key);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private Object toKey(String rawSound, Class<?> keyClass) {
        String trimmed = rawSound.trim().toLowerCase(Locale.ROOT);
        String value = trimmed.contains(":") ?
                trimmed :
                "minecraft:" + trimmed.replace('_', '.');

        try {
            return keyClass.getMethod("fromString", String.class).invoke(null, value);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
