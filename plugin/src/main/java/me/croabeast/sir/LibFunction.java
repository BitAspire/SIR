package me.croabeast.sir;

import lombok.experimental.UtilityClass;
import me.croabeast.sir.module.ModuleManager;
import me.croabeast.sir.module.emoji.Emojis;
import me.croabeast.sir.module.tag.Tags;
import me.croabeast.takion.message.MessageSender;

@UtilityClass
class LibFunction {

    void applyFunctions(MessageSender sender) {
        ModuleManager manager = SIRApi.instance().getModuleManager();

        if (manager.isEnabled("Emojis")) {
            Emojis emojis = manager.getModule(Emojis.class);
            if (emojis != null) sender.addFunctions(emojis::parseEmojis);
        }

        if (manager.isEnabled("Tags")) {
            Tags tags = manager.getModule(Tags.class);
            if (tags != null) sender.addFunctions(tags::parseTags);
        }
    }
}
