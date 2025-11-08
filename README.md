# 🤖 SIR – Simple In-game Receptionist

A modular Minecraft assistant that centralizes chat management, announcements, onboarding, and cross-platform communication. SIR ships with a rich configuration toolkit so you can tailor every interaction players see without touching Java code.

---

## 🌟 Feature Highlights
- **Modular architecture** – Toggle self-contained modules for chat, join/quit experiences, announcements, MOTDs, advancements, and third-party hooks.
- **Deep chat customization** – Build channel hierarchies, apply gradient colours, emojis, hover/click actions, tags, and mention effects directly from YAML.
- **Player journey controls** – Craft first-join flows, spawn teleport rules, welcome/farewell messages, and timed invulnerability windows.
- **Automated outreach** – Schedule announcements that mix chat, action bar, title, boss bar, sounds, and console commands.
- **Discord-ready** – Mirror activity with DiscordSRV or EssentialsDiscord using embed templates, per-event routing, and cross-server relays.
- **Login & vanish awareness** – Coordinate with major authentication and vanish plugins so muted, hidden, or unauthenticated players follow your rules.
- **Takion-powered runtime** – The Takion framework is bundled, providing the scheduler, text processors, gradients, and boss bars you rely on—no extra download needed.
- **Live updates & metrics** – Automatic update checks, optional coloured console logging, and bStats analytics keep administrators informed.

---

## 🧭 Compatibility Matrix
| Area | Details |
| --- | --- |
| **Minecraft server** | Spigot/Paper/Folia API 1.13+ (Folia flag enabled in `plugin.yml`). |
| **Java runtime** | Compiled for Java 8 targets while building with Maven (Java 21 toolchain supported). |
| **Permissions** | Vault-driven chat adapter with automatic fallback when Vault is absent. |
| **Placeholder support** | PlaceholderAPI expansion included; formats across messages, announcements, and Discord bridge respect PAPI placeholders. |
| **Text effects** | Takion gradient & rainbow tags, hover/click actions, and InventoryFramework-powered GUIs for chat colour menus. |
| **Analytics** | bStats (ID 25264) with drill-down charts for detected integrations. |

---

## 🧩 Built-in & Optional Integrations
| Category | Supported Plugins / Libraries | Notes |
| --- | --- | --- |
| **Bundled libraries** | Takion 1.3, GlobalScheduler, LangUtils | Shaded inside SIR—no external jars needed. |
| **Metrics** | bStats | Relocated package (`me.croabeast.metrics`) to avoid conflicts. |
| **Chat & permissions** | Vault, InteractiveChat | Automatically detected; permissions exposed through `commands.yml` and channel configs. |
| **Placeholder system** | PlaceholderAPI | Optional but fully supported; registers a dedicated expansion. |
| **Discord bridge** | DiscordSRV, EssentialsDiscord | Route join/quit/advancement/chat events with per-channel embed templates. |
| **Authentication** | AuthMe, UserLogin, NexAuth, nLogin, OpeNLogin | Shared spawn handling via `modules/hook/login.yml`. |
| **Vanish handling** | CMI, EssentialsX, SuperVanish, PremiumVanish | Optional vanish chat key rules to block message leaks. |
| **Moderation synergy** | AdvancedBan and other punishment suites | Use cooldowns, mute commands, and violation actions to complement third-party moderation. |

---

## 🗂️ Modular Platform Overview
| Module | Purpose | Key Configuration Files |
| --- | --- | --- |
| **Join/Quit** | Welcomes players, controls vanilla join/quit spam, and manages spawn & invulnerability. | `modules/join_quit/config.yml`, `modules/join_quit/messages.yml` |
| **Announcements** | Time-based broadcasts mixing chat, titles, action bars, sounds, and commands. | `modules/announcements/config.yml`, `modules/announcements/announces.yml` |
| **MOTD** | Rotates server list MOTDs and icons with custom counters. | `modules/motd/config.yml`, `modules/motd/motds.yml` |
| **Advancements** | Broadcasts achievements with rewards and per-world/mode filters. | `modules/advancements/config.yml`, `modules/advancements/messages.yml` |
| **Chat – Channels** | Hierarchical global/local channels with inheritance and colour governance. | `modules/chat/channels.yml` (see wiki guide) |
| **Chat – Cooldowns** | Anti-spam timers and scripted punishments. | `modules/chat/cooldowns.yml` |
| **Chat – Emojis & Tags** | Replace shortcuts with formatted text and manage permission-based tags. | `modules/chat/emojis.yml`, `modules/chat/tags.yml` |
| **Chat – Mentions** | Highlight recipients with sounds, hover/click actions, and custom outputs. | `modules/chat/mentions.yml` |
| **Chat – Moderation** | Filter swears, caps, links, and enforce format standards. | `modules/chat/moderation.yml` |
| **Hooks – Discord** | DiscordSRV channel routing and embed templates. | `modules/hook/discord.yml`, `webhooks.yml` |
| **Hooks – Login/Vanish** | Sync behaviour with authentication and vanish plugins. | `modules/hook/login.yml`, `modules/hook/vanish.yml` |

All modules are toggled from `modules/modules.yml`, which also manages update tracking for shipped configs.

---

## 💬 Command Suite
| Command | Summary | Highlights |
| --- | --- | --- |
| `/sir` | Administrative hub | About, reload, module status, help, support links. |
| `/print` | Raw formatted broadcaster | Targeted chat, titles, action bars, Discord webhooks. |
| `/announcer` | Manage automated announcements | Preview, start/stop, reboot sequences. |
| `/chatview` | Channel visibility | Players opt in/out of channel subscriptions. |
| `/chatcolor` | GUI colour selector | Unlockable colours, gradients, rainbow styles. |
| `/ignore` | Personal mute list | Persistent ignore data per player. |
| `/clearchat` | Clear local/global chat | Instant moderation tool. |
| `/msg`, `/reply` | Private messaging | Sound & action feedback for mentions. |
| `/mute`, `/tempmute`, `/unmute`, `/checkmute` | Moderation controls | Permanent or timed mutes powered by configurable lang/data files. |

Command behaviour, aliases, and language strings live under `resources/commands/` for complete customization.

---

## 🎨 Formatting & Player Experience Toolkit
- **Message prefixing:** Define the main plugin prefix, centred text tag, and line separators in `config.yml`.
- **Gradient & rainbow tags:** Use `<G:#RRGGBB:#RRGGBB>` or `<R:n>` syntax anywhere the plugin parses text.
- **Interactive components:** Attach hover lists and click actions across messages, announcements, and mentions.
- **Sound design:** Assign unique join/quit, mention, and announcement sounds using vanilla sound keys.
- **Spawn logic:** Per-message group spawn teleporters align with login plugins when enabled.
- **Boss bars & titles:** Tap into Takion’s animated boss bars for announcement or module-specific effects.

---

## 🚀 Getting Started
1. **Drop SIR into `plugins/`** – No external dependencies are required; Takion is already shaded inside the jar.
2. **Start your server once** – Configuration files populate under `plugins/SIR/resources/`.
3. **Review `config.yml` and `modules/modules.yml`** – Toggle features and adjust update checks.
4. **Tailor modules** – Edit the YAML files under each module directory to match your server’s tone and rules.
5. **Grant permissions** – Use your permission manager (Vault-compatible) to assign command and channel access.
6. **Reload or restart** – `/sir reload` applies configuration changes without a full reboot.

Need more detail? The wiki pages below dive into each subsystem with copy-paste-ready examples.

---

## 📚 Documentation & Support
- **Home:** [`wiki/Home.md`](wiki/Home.md)
- **Setup guide:** [`wiki/Getting-Started.md`](wiki/Getting-Started.md)
- **Module reference:** [`wiki/Module-Reference.md`](wiki/Module-Reference.md)
- **Integrations:** [`wiki/Integrations.md`](wiki/Integrations.md)
- **Chat channels deep dive:** [`wiki/Chat-Channels.md`](wiki/Chat-Channels.md)
- **Formatting toolkit:** [`wiki/Formatting-Toolkit.md`](wiki/Formatting-Toolkit.md)
- **Command reference:** [`wiki/Command-Reference.md`](wiki/Command-Reference.md)
- **Community Discord:** [discord.gg/s9YFGMrjyF](https://discord.gg/s9YFGMrjyF)
- **Issue tracker:** [github.com/CroaBeast/SIR/issues](https://github.com/CroaBeast/SIR/issues)

---

## 🔒 Telemetry & Privacy
SIR reports anonymous usage data to bStats (metrics ID `25264`) to help the developer understand which integrations are active. Disable metrics globally in `plugins/bStats/config.yml` or use your firewall if you prefer to opt out.

---

## 🛠️ Contributing & Feedback
Found a bug or have a feature request? Open an issue on GitHub or reach out through the Discord community. Contributions are welcome—fork the repository, make your improvements, and submit a pull request.

With SIR, you control every step of the conversation players see. Configure once, delight forever.
