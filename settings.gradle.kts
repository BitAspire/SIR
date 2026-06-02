rootProject.name = "SIR"
include("api", "runtime", "plugin")
include(
    "module:advancements", "module:channels", "module:discord", "module:announcements",
    "module:cooldowns", "module:emojis", "module:join-quit", "module:login",
    "module:mentions", "module:moderation", "module:motd", "module:tags", "module:vanish",
    "module:all"
)
include(
    "command:clear-chat", "command:color", "command:ignore", "command:message",
    "command:mute", "command:nick", "command:print", "command:settings", "command:all"
)
