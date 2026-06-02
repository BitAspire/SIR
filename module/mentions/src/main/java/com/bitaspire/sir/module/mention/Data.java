package com.bitaspire.sir.module.mention;

import lombok.Getter;
import lombok.SneakyThrows;
import com.bitaspire.sir.file.ExtensionFile;
import com.bitaspire.sir.PermissibleUnit;

import java.util.HashSet;
import java.util.Set;

@Getter
final class Data {

    private final Set<Mention> mentions = new HashSet<>();

    @SneakyThrows
    Data(Mentions main) {
        mentions.addAll(PermissibleUnit.loadUnits(
                new ExtensionFile(main, "mentions", true).getSection("mentions"),
                Mention::new
        ));
    }
}
