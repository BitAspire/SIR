package me.croabeast.sir.module.emoji;

import me.croabeast.sir.ExtensionFile;
import me.croabeast.sir.PermissibleUnit;

import java.util.LinkedHashMap;
import java.util.Map;

public class Data {

    final Map<String, Emoji> emojis = new LinkedHashMap<>();

    Data(Emojis main) {
        try {
            PermissibleUnit.loadUnits(new ExtensionFile(main, "emojis", true)
                    .getSection("emojis"), Emoji::new)
                    .forEach(e -> this.emojis.put(e.getKey(), e));
        } catch (Exception ignored) {}
    }
}
