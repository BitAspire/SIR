# Module Reference

Resumen rapido de los modulos disponibles y donde estan sus archivos por defecto en esta wiki.

## Ubicacion
- Carpeta raiz: `wiki/modules/`
- Indice: [modules/README.md](modules/README.md)

## Modulos
| Modulo | Carpeta | Archivos Principales |
| --- | --- | --- |
| Advancements | `wiki/modules/advancements/` | `config.yml`, `messages.yml`, `data.yml`, `module.yml` |
| Announcements | `wiki/modules/announcements/` | `announcements.yml`, `config.yml`, `commands.yml`, `lang.yml`, `module.yml` |
| Channels | `wiki/modules/channels/` | `channels.yml`, `config.yml`, `commands.yml`, `lang.yml`, `module.yml` |
| Cooldowns | `wiki/modules/cooldowns/` | `cooldowns.yml`, `module.yml` |
| Discord | `wiki/modules/discord/` | `config.yml`, `module.yml` |
| Emojis | `wiki/modules/emojis/` | `emojis.yml`, `module.yml` |
| Join-Quit | `wiki/modules/join-quit/` | `config.yml`, `messages.yml`, `module.yml` |
| Login | `wiki/modules/login/` | `module.yml` |
| Mentions | `wiki/modules/mentions/` | `mentions.yml`, `module.yml` |
| Moderation | `wiki/modules/moderation/` | `config.yml`, `format.yml`, `links.yml`, `caps.yml`, `swearing.yml`, `module.yml` |
| MOTD | `wiki/modules/motd/` | `config.yml`, `motd.yml`, `module.yml`, `icons/server-icon.png` |
| Tags | `wiki/modules/tags/` | `tags.yml`, `module.yml` |
| Vanish | `wiki/modules/vanish/` | `config.yml`, `module.yml` |

## Flujo Recomendado
1. Revisa `module.yml` para entender objetivo/dependencias del modulo.
2. Ajusta su `config.yml` (si existe).
3. Edita archivos funcionales (`messages.yml`, `channels.yml`, etc.).
4. Recarga y valida en servidor.
