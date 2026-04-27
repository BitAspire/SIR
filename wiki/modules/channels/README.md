# Module: channels

Archivos incluidos en esta carpeta:
- `channels.yml`
- `commands.yml`
- `config.yml`
- `lang.yml`
- `module.yml`

## Formato Soportado
- `version: 2` usa el formato nuevo.
- Si el archivo es legacy o híbrido, SIR lo migra automáticamente al formato `version: 2`.

## Idea del Formato v2
- `default-channel` es la base y el fallback final.
- `inherits` hereda de otro canal; si no se declara, hereda de `default-channel`.
- `access` define cómo se entra al canal.
- `audience` define quién recibe el mensaje.
- `style` define cómo se ve.
- `logging` define el log personalizado del canal.

## Reglas Importantes
- Si `access.default` es `false` y no hay `prefixes` ni `commands`, el canal no se puede usar directamente.
- Todos los filtros dentro de `audience` son acumulativos.
- Si `logging.enabled` es `false`, se usa el logging legacy de SIR.
- Si `logging.enabled` es `true` pero no hay `logging.format`, se hereda del canal padre o de `default-channel`; si ninguno tiene formato, se usa el logging legacy.

## Ejemplo Rapido

```yml
version: 2

default-channel:
  access:
    default: true
  audience:
    radius: 120
  style:
    tag: '&8[&aLocal&8]'
    colors:
      default: '&f'
    format: '{tag} &7{player}&8: {color}{message}'

channels:
  global:
    access:
      prefixes: [ "!" ]
      commands: [ "/g", "/global" ]
    style:
      tag: '&8[&6Global&8]'
      format: '{tag} &7{player}&8: {color}{message}'
```

Resultado:
- `hola` -> chat local
- `!hola` -> chat global
- `/g hola` -> chat global

Fuente original: `module/channels/src/main/resources`
