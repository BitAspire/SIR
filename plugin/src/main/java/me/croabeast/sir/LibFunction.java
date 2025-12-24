package me.croabeast.sir;

import lombok.experimental.UtilityClass;
import me.croabeast.sir.module.ModuleManager;
import me.croabeast.sir.module.SIRModule;
import me.croabeast.sir.user.SIRUser;
import me.croabeast.takion.message.MessageSender;

import java.lang.reflect.Method;
import java.util.function.BiFunction;

@UtilityClass
class LibFunction {

    void applyFunctions(MessageSender sender) {
        ModuleManager manager = SIRApi.instance().getModuleManager();

        attachModuleFunction(sender, manager, "Emojis", "parseEmojis");
        attachModuleFunction(sender, manager, "Tags", "parseTags");
    }

    private void attachModuleFunction(MessageSender sender, ModuleManager manager, String name, String methodName) {
        if (!manager.isEnabled(name)) return;

        SIRModule module = manager.getModule(name);
        if (module == null) return;

        try {
            Method method = module.getClass().getMethod(methodName, SIRUser.class, String.class);
            BiFunction<SIRUser, String, String> function = (user, message) -> {
                try {
                    return (String) method.invoke(module, user, message);
                } catch (Exception ignored) {
                    return message;
                }
            };

            sender.addFunctions((u, s) -> function.apply(SIRApi.instance().getUserManager().getUser(u), s));
        } catch (Exception ignored) {
        }
    }
}