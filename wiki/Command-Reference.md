# Command Reference

Referencia de command providers y sus archivos por defecto dentro de la wiki.

## Ubicacion
- Carpeta raiz: `wiki/command-providers/`
- Indice: [command-providers/README.md](command-providers/README.md)

## Providers
| Provider | Carpeta | Archivos |
| --- | --- | --- |
| clear-chat | `wiki/command-providers/clear-chat/` | `commands.yml`, `lang.yml` |
| color | `wiki/command-providers/color/` | `commands.yml`, `data.yml`, `lang.yml`, `menu.yml` |
| ignore | `wiki/command-providers/ignore/` | `commands.yml`, `data.yml`, `lang.yml` |
| message | `wiki/command-providers/message/` | `commands.yml`, `lang.yml` |
| mute | `wiki/command-providers/mute/` | `commands.yml`, `data.yml`, `lang.yml` |
| print | `wiki/command-providers/print/` | `commands.yml`, `lang.yml` |
| settings | `wiki/command-providers/settings/` | `commands.yml`, `lang.yml`, `settings.yml` |

## Flujo Recomendado
1. Ajusta `commands.yml` para aliases/permisos/comportamiento.
2. Edita `lang.yml` para textos y feedback.
3. Si existe `data.yml` o `settings.yml`, revisa persistencia/estado.
4. Recarga y valida comandos en juego.
