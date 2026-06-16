package com.bitaspire.sir.module.moderation;

import com.bitaspire.sir.chat.ChatProcessor;

import java.util.Locale;

final class Caps extends Module {

    Caps(Moderation main) {
        super(main, "caps");
    }

    private int longestConsecutiveUppercase(String msg) {
        int maxConsecutive = 0;
        int currentCount = 0;

        for (final char ch : msg.toCharArray()) {
            if (Character.isUpperCase(ch)) {
                currentCount++;
                if (currentCount > maxConsecutive)
                    maxConsecutive = currentCount;

                continue;
            }
            currentCount = 0;
        }

        return maxConsecutive;
    }

    @Override
    void process0(ChatProcessor.Context context) {
        String message = context.getMessage();

        int capsCount = longestConsecutiveUppercase(message);

        int max = file.getConfiguration().getInt("maximum-caps", 10);
        if (capsCount <= max) return;

        validateAndExecuteActions(context.getPlayer(), message, max);
        if (file.get("control", "BLOCK").matches("(?i)block")) {
            context.cancel();
            return;
        }

        context.setMessage(message.toLowerCase(Locale.ENGLISH));
    }
}
