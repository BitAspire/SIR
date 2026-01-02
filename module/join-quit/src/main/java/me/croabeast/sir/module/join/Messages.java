package me.croabeast.sir.module.join;

import lombok.SneakyThrows;
import me.croabeast.sir.ExtensionFile;
import me.croabeast.sir.PermissibleUnit;
import me.croabeast.sir.user.SIRUser;

import java.util.*;

final class Messages {

    private final Map<Type, Set<MessageUnit>> units = new LinkedHashMap<>();
    private final JoinQuit main;

    private void loadUnits(ExtensionFile file, String path, Type type) {
        Set<MessageUnit> set = units.computeIfAbsent(type, v -> new LinkedHashSet<>());
        set.addAll(PermissibleUnit.loadUnits(file.getSection(path), s -> new MessageUnit(main, s, type)));
        units.put(type, set);
    }

    @SneakyThrows
    Messages(JoinQuit main) {
        ExtensionFile file = new ExtensionFile(this.main = main, "messages", true);

        loadUnits(file, "first-join", Type.FIRST_JOIN);
        loadUnits(file, "join", Type.JOIN);
        loadUnits(file, "quit", Type.QUIT);
    }

    MessageUnit get(SIRUser user, boolean join) {
        Type type = join ?
                (user.getPlayer().hasPlayedBefore() ?
                        Type.JOIN :
                        Type.FIRST_JOIN) :
                Type.QUIT;
        Set<MessageUnit> set = units.getOrDefault(type, new HashSet<>());
        return PermissibleUnit.getUnit(user, set, false);
    }

    enum Type {
        FIRST_JOIN,
        JOIN,
        QUIT
    }
}
