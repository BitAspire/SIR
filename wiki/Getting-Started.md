# Getting Started

This guide walks through installing SIR, preparing the configuration files, and activating the modules that suit your server.

## 1. Installation Checklist
1. **Download the latest SIR jar** from the project releases page.
2. **Copy the jar into `plugins/`** on your Spigot/Paper/Folia server. No external libraries are required—Takion and metrics tooling are already packaged in the jar.
3. **Start the server once**. SIR will generate its data folder and populate `resources/` with default YAML files.
4. **Confirm the folder layout:**
   - `plugins/SIR/config.yml`
   - `plugins/SIR/resources/modules/` (per-feature configuration)
   - `plugins/SIR/resources/commands/` (command behaviour & language)
   - `plugins/SIR/resources/webhooks.yml` (Discord webhook presets)
5. **Stop the server** so you can make configuration changes safely.

## 2. First-Run Priorities
| File | Purpose | Key Actions |
| --- | --- | --- |
| `config.yml` | Global preferences | Decide whether to colour console logs, expose message prefixes, or allow Bukkit’s default configuration methods. |
| `modules/modules.yml` | Module toggles | Enable/disable Join/Quit, Announcements, MOTD, Advancements, Chat submodules, and external hooks. |
| `modules/chat/channels.yml` | Channel layout | Review the default/global channel, then design any staff, VIP, or local channels. |
| `modules/join_quit/messages.yml` | Join, first-join, and quit flows | Customize public/private lines, sounds, and spawn/invulnerability behaviour per permission group. |
| `modules/announcements/announces.yml` | Automated broadcasts | Mix chat, action bar, title, and command actions to match your schedule. |

## 3. Enabling Modules
- Toggle each feature flag under `modules/modules.yml`. Nested sections (like `chat.channels` or `hook.discord`) let you disable a single submodule without touching the others.
- Reload in-game with `/sir reload` or restart the server. SIR logs a summary of loaded/enabled/registered modules so you can verify the state quickly.

## 4. Permissions & Groups
- SIR reads permissions from your Vault-compatible manager. Check `resources/commands/commands.yml` for every permission node the plugin exposes.
- Channel, emoji, tag, cooldown, and message rules can also enforce permissions defined inside their respective YAML entries.
- Use the `group` attribute in emojis, tags, and channels if you rely on group tracking from your permissions plugin.

## 5. Update Management
- Automatic update checks are controlled in `config.yml` (`updater.on-start`, `updater.send-op`).
- Each module file contains an `update: true` flag—leave this enabled to receive inline changelog hints when defaults change between versions.
- SIR’s update checker leverages the bundled Takion utilities and only notifies players with `sir.admin.update` unless you disable OP notifications.

## 6. Troubleshooting Tips
- **Missing permissions?** Use `/sir commands` or consult the command reference to confirm nodes, then verify they are granted through your permission plugin.
- **Discord messages not sending?** Make sure `modules/hook/discord.yml` contains valid channel IDs and that DiscordSRV or EssentialsDiscord is running.
- **Login teleport issues?** Set `spawn-before` in `modules/hook/login.yml` according to your authentication plugin’s expectations and define spawn targets inside join message entries.
- **Muted players bypassing chat?** Confirm the moderation module’s actions and the `/mute` command configuration under `resources/commands/mute/`.

When in doubt, review the [Module Reference](Module-Reference.md) for per-feature explanations or check console logs—SIR’s startup banner lists exactly which modules and hooks are active.
