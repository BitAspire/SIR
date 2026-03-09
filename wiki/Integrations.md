# Integrations

SIR detects and collaborates with multiple third-party plugins. This page outlines what each integration does and how to configure it.

## Bundled Libraries
- **Takion 1.3** – Provides the scheduler, animated boss bars, gradient/formatting engine, updater utilities, and shared resource helpers. The library is shaded inside the jar, so no separate dependency is required.
- **GlobalScheduler** – Powers synchronous and asynchronous task execution across all modules.
- **LangUtils & CollectionBuilder** – Handle localisation, delayed logging, and structured configuration loading.
- **bStats** – Anonymous metrics collection (ID `25264`). Disable it globally through `plugins/bStats/config.yml` if desired.

## Permissions & Chat
- **Vault** – Grants SIR access to group prefixes/suffixes and permission checks. When Vault is absent the plugin falls back to basic permission logic.
- **InteractiveChat** – Detected for compatibility so both plugins can cooperate on hover/click components.

## Placeholder Support
- **PlaceholderAPI** – Optional but recommended. SIR registers an expansion so you can use placeholders in:
  - Chat formats (`channels.yml`)
  - Announcements and command outputs
  - Join/Quit messages and mentions
  - Discord embed templates

## Discord Bridges
- **DiscordSRV & EssentialsDiscord** – Enable the Discord hook module to stream first-join, join, quit, chat, and advancement events into Discord channels.
  - Configure routing in `modules/hook/discord.yml` using channel IDs or cross-server mappings (`OTHER_SERVER_ID:CHANNEL_ID`).
  - Each event supports plain text and embeds with author, thumbnail, title, description, colour, and timestamp fields.
  - EssentialsDiscord manages its own configuration; SIR only sends formatted payloads.

## Authentication Plugins
SIR recognises the following login plugins:
- AuthMe
- UserLogin
- NexAuth
- nLogin
- OpeNLogin

Use `modules/hook/login.yml` to decide whether players are teleported before or after authentication (`spawn-before`) and define spawn locations within your join message groups. This prevents location leaks and keeps the onboarding flow consistent across plugins.

## Vanish Plugins
SIR integrates with vanish solutions such as CMI, EssentialsX, SuperVanish, and PremiumVanish. With the vanish module enabled you can:
- Block vanished players from chatting entirely.
- Require a special chat prefix/suffix (with optional regex matching) so staff can intentionally talk while hidden.
- Provide custom feedback messages when a vanished player attempts to speak without the key.

## Moderation Suites
- **AdvancedBan and similar tools** – While not required, SIR’s moderation module can complement punishment plugins by running commands after violation limits (e.g., `mute {player} Swearing`).
- The `/mute`, `/tempmute`, `/unmute`, and `/checkmute` commands store their data under `resources/commands/mute/`, allowing you to align SIR’s mute system with your broader moderation stack.

## Discord Webhooks
Separate from the Discord hook, `resources/webhooks.yml` lets you define webhook endpoints that the `/print` command can target. Use this to broadcast announcements or staff alerts directly to Discord without relying on DiscordSRV.

Keep these integrations enabled only when you use them—SIR checks for each plugin at startup and logs a summary so you know what was detected.
