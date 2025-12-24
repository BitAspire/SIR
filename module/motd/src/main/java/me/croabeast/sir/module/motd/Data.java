package me.croabeast.sir.module.motd;

import lombok.Getter;
import lombok.SneakyThrows;
import me.croabeast.sir.ExtensionFile;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Getter
final class Data {

    private final List<String[]> list = new ArrayList<>();

    @SneakyThrows
    Data(MOTD main) {
        ExtensionFile file = new ExtensionFile(main, "motd", true);

        for (String key : file.getKeys("motd")) {
            String one = file.get("motd." + key + ".1", "");
            if (StringUtils.isBlank(one)) continue;

            String[] array = {one, null};

            String two = file.get("motd." + key + ".2", "");
            if (StringUtils.isNotBlank(two)) array[1] = two;

            list.add(array);
        }
    }
}
