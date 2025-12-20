package me.croabeast.sir.module.mention;

import lombok.Getter;
import me.croabeast.sir.ExtensionFile;
import me.croabeast.sir.PermissibleUnit;

import java.util.HashSet;
import java.util.Set;

@Getter
final class Data {

    private final Set<Mention> mentions = new HashSet<>();

    Data(Mentions main) {
        try {
            mentions.addAll(PermissibleUnit.loadUnits(
                    new ExtensionFile(main, "mentions", true).getSection("mentions"),
                    Mention::new
            ));
        } catch (Exception ignored) {}
    }
}
