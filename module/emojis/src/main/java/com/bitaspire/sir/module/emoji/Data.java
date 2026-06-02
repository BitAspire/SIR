package com.bitaspire.sir.module.emoji;

import lombok.SneakyThrows;
import com.bitaspire.sir.file.ExtensionFile;
import com.bitaspire.sir.PermissibleUnit;

import java.util.LinkedHashMap;
import java.util.Map;

public class Data {

    final Map<String, Emoji> emojis = new LinkedHashMap<>();

    @SneakyThrows
    Data(Emojis main) {
        PermissibleUnit.loadUnits(new ExtensionFile(main, "emojis", true)
                        .getSection("emojis"), Emoji::new)
                .forEach(e -> this.emojis.put(e.getKey(), e));
    }
}
