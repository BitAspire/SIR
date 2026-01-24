package me.croabeast.sir;

import lombok.experimental.UtilityClass;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Locale;

@UtilityClass
public class SoundUtils {

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
            Object registry;

            try {
                registry = registryClass.getField("SOUNDS").get(null);
            } catch (NoSuchFieldException ignored) {
                registry = registryClass.getField("SOUND").get(null);
            }

            if (registry == null) return null;

            NamespacedKey key = toKey(rawSound);
            if (key == null) return null;

            Method getMethod = registry.getClass().getMethod("get", NamespacedKey.class);
            return (Sound) getMethod.invoke(registry, key);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private NamespacedKey toKey(String rawSound) {
        String trimmed = rawSound.trim().toLowerCase(Locale.ROOT);
        if (trimmed.contains(":"))
            return NamespacedKey.fromString(trimmed);

        String normalized = trimmed.replace('_', '.');
        return NamespacedKey.fromString("minecraft:" + normalized);
    }
}