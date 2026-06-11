package com.bitaspire.sir.module.announcement;

import lombok.Getter;
import lombok.SneakyThrows;
import com.bitaspire.sir.file.ExtensionFile;
import com.bitaspire.sir.PermissibleUnit;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

@Getter
final class Data {

    private final Map<String, Announcement> announcements = new LinkedHashMap<>();

    @SneakyThrows
    Data(Announcements main) {
        PermissibleUnit.loadUnits(new ExtensionFile(main, "announcements", true)
                        .getSection("announcements"), section -> new Announcement(main, section))
                .forEach(e -> announcements.put(e.getName(), e));
    }

    Announcement fromIndex(int count) {
        LinkedList<Announcement> list = new LinkedList<>(announcements.values());
        return count < 0 || count >= list.size() ? null : list.get(count);
    }
}
