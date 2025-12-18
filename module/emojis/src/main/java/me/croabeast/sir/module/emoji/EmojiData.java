package me.croabeast.sir.module.emoji;

import me.croabeast.sir.ExtensionFile;
import me.croabeast.sir.PermissibleUnit;
import me.croabeast.sir.user.SIRUser;

import java.util.LinkedHashMap;
import java.util.Map;

public class EmojiData {

    final Map<String, Emoji> emojis = new LinkedHashMap<>();

    EmojiData(Emojis main) {
        try {
            PermissibleUnit.loadUnits(new ExtensionFile(main, "emojis", true)
                    .getSection("emojis"), Emoji::new)
                    .forEach(e -> this.emojis.put(e.getKey(), e));
        } catch (Exception ignored) {}
    }

    Emoji getEmoji(SIRUser user) {
        return PermissibleUnit.getUnit(user, emojis.values(), true);
    }
}
