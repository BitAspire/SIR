rootProject.name = "SIR"
include("core", "command", "module", "api", "plugin")
include(
    "module:advancements", "module:channels", "module:discord", "module:announcements",
    "module:cooldowns", "module:emojis", "module:join-quit", "module:login",
    "module:mentions", "module:moderation", "module:motd", "module:tags", "module:vanish"
)
