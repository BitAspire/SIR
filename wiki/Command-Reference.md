# Command Reference

Every command is configured through `resources/commands/`. Use this page to understand what each command does and which permissions control it.

## Administrative Commands
| Command | Default Permission | Purpose | Configuration Files |
| --- | --- | --- | --- |
| `/sir` | `sir.admin` | Displays plugin info, module status, reload options, help, and support links. | `commands/sir.yml` |
| `/sir modules` | `sir.admin.modules` | View enabled/disabled modules. | `commands/sir.yml` |
| `/sir reload` | `sir.admin.reload` | Reload configurations without restarting. | `commands/sir.yml` |
| `/sir update` | `sir.admin.update` | Check for updates using Takion’s updater. | `commands/sir.yml` |

## Broadcasting & Messaging
| Command | Default Permission | Highlights | Configuration Files |
| --- | --- | --- | --- |
| `/print` (`/rawmsg`, `/rm`) | `sir.print` | Send formatted chat, action bar, title, boss bar, or webhook messages. | `commands/print.yml` |
| `/print targets` | `sir.print.targets` | Target specific players or groups. | `commands/print.yml` |
| `/print webhook` | `sir.print.webhook` | Use presets from `webhooks.yml` to post to Discord. | `commands/print.yml`, `webhooks.yml` |
| `/announcer` | `sir.announcer` | Start/stop/reboot the automated announcer or preview the next message. | `commands/announcer.yml` |
| `/chatview` | `sir.chatview` | Toggle visibility for individual chat channels. | `commands/chat_view/` |
| `/chatcolor` (`/color`, `/cc`) | `sir.chatcolor` | Open the InventoryFramework GUI to choose unlocked colours/effects. | `commands/chat_color/` |
| `/clearchat` (`/cc`) | `sir.clearchat` | Wipe public chat for moderation. | `commands/clear-chat.yml` |

## Private Messaging Suite
| Command | Default Permission | Highlights | Configuration Files |
| --- | --- | --- | --- |
| `/msg` (`/m`, `/message`, `/tell`) | `sir.message` | Send private messages with hover/click feedback and sounds. | `commands/msg-reply.yml` |
| `/reply` (`/r`) | `sir.reply` | Respond to the last private message sender. | `commands/msg-reply.yml` |
| `/ignore` (`/ig`) | `sir.ignore` | Manage a personal ignore list persisted in `commands/ignore/data.yml`. | `commands/ignore/` |

## Moderation Commands
| Command | Default Permission | Highlights | Configuration Files |
| --- | --- | --- | --- |
| `/mute` | `sir.mute.perm` | Apply indefinite mutes; ties into moderation module messaging. | `commands/mute/` |
| `/tempmute` | `sir.mute.temp` | Apply timed mutes with configurable templates. | `commands/mute/` |
| `/unmute` | `sir.unmute` | Remove active mutes. | `commands/mute/` |
| `/checkmute` | `sir.checkmute` | Inspect mute status for a player. | `commands/mute/` |

## Command Tips
- Many commands support aliases defined in their YAML files (e.g., `/color`, `/rm`, `/ig`).
- Language strings for success/error messages live alongside each command configuration under `lang.yml` files.
- Use `/sir commands` in-game to review command availability with their current status and permissions.
- Disable any command by setting `enabled: false` or `override-existing: false` if you prefer another plugin’s implementation.

Grant permissions through your Vault-compatible manager and reload after editing the YAML files to apply changes.
