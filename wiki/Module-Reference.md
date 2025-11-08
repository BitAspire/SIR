# Module Reference

SIR divides its functionality into modular building blocks. Toggle them from `resources/modules/modules.yml`, then use the linked configuration files to adjust behaviour.

## Join & Quit
- **Primary files:** `modules/join_quit/config.yml`, `modules/join_quit/messages.yml`
- **Highlights:**
  - Disable vanilla join/quit spam while leaving SIR’s custom messages intact.
  - Apply cooldowns between join/quit announcements to prevent rapid reconnect spam.
  - Define first-join, group-based join, and quit messages with separate public/private outputs.
  - Attach sounds, invulnerability windows, spawn teleports, and priority-based overrides per entry.

## Announcements
- **Primary files:** `modules/announcements/config.yml`, `modules/announcements/announces.yml`
- **Highlights:**
  - Schedule broadcasts in ticks with sequential or random rotation.
  - Combine chat, action bar, title/subtitle, boss bar, and sound effects in a single announcement entry.
  - Execute console commands alongside each broadcast to trigger events or rewards.

## MOTD
- **Primary files:** `modules/motd/config.yml`, `modules/motd/motds.yml`
- **Highlights:**
  - Switch between default, maximum, or custom server-list player counts.
  - Rotate icons (single, random, or ordered lists) from the dedicated icons folder.
  - Maintain multiple MOTD styles with two-line formatting and rainbow/colour tags.

## Advancements
- **Primary files:** `modules/advancements/config.yml`, `modules/advancements/messages.yml`
- **Highlights:**
  - Filter advancement broadcasts by world, game mode, or specific advancement IDs.
  - Tailor message formats for task, goal, challenge, and custom advancements.
  - Reward players with console or player-executed commands triggered by advancement completions.

## Chat System
This module is composed of several submodules—enable or disable each individually.

### Channels
- **File:** `modules/chat/channels.yml`
- Manage global and local channels with inheritance from `default-channel`.
- Assign permissions, priorities, prefixes/suffixes, click or hover actions, and colour policies per channel.
- Configure local subchannels with message prefixes or commands for quick access. See the [Chat Channels](Chat-Channels.md) guide for an in-depth walkthrough.

### Cooldowns
- **File:** `modules/chat/cooldowns.yml`
- Define per-permission cooldown timers and spam checks.
- Trigger console commands and custom warning messages when thresholds are exceeded.

### Emojis
- **File:** `modules/chat/emojis.yml`
- Replace shortcuts with coloured, formatted text or Unicode characters.
- Enforce permission, priority, and group rules, plus regex or word-only matching to fine-tune replacements.

### Mentions
- **File:** `modules/chat/mentions.yml`
- Allow players to ping each other with sound feedback, hover tooltips, click-to-reply actions, and custom display formats.
- Configure different mention profiles with unique permissions or prefixes.

### Moderation
- **File:** `modules/chat/moderation.yml`
- Block or replace swearing, excessive caps, or unapproved links with tiered violation limits.
- Notify staff with configurable permission checks and log violations to the console.
- Optionally enforce automatic message formatting (capitalization, prefixes, suffixes).

### Tags
- **File:** `modules/chat/tags.yml`
- Provide customisable chat tags with permission, priority, and group requirements.
- Supply hover descriptions so players understand how tags are earned.

## Discord Hook
- **File:** `modules/hook/discord.yml`
- Route first-join, join, quit, chat, and advancement events to specific DiscordSRV or EssentialsDiscord channels.
- Mix plain text with embed sections (author, thumbnail, description, timestamp, colours) per event.
- Support cross-server relays using the `OTHER_SERVER_ID:CHANNEL_ID` syntax.

## Login Hook
- **File:** `modules/hook/login.yml`
- Coordinate with authentication plugins (AuthMe, UserLogin, NexAuth, nLogin, OpeNLogin).
- Teleport players before or after authentication and reuse join message spawn definitions for consistent behaviour.

## Vanish Hook
- **File:** `modules/hook/vanish.yml`
- Detect players hidden by vanish plugins (CMI, EssentialsX, SuperVanish, etc.).
- Optionally require a chat key prefix/suffix (with regex support) to let vanished players talk, or block them entirely with a friendly error message.

## Command Configurations
- **Directory:** `resources/commands/`
- Includes per-command toggles, aliases, permission nodes, GUI menus (chat colour selector), language entries, and stored data.
- Update these files to alter the behaviour of `/sir`, `/print`, `/announcer`, `/chatview`, `/chatcolor`, `/ignore`, `/clearchat`, `/msg`, `/reply`, and the mute suite.

Use the module toggles to keep only what you need—SIR’s architecture ensures unused systems stay dormant and do not impact performance.
