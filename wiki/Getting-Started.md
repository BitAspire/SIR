# Getting Started

Esta guia esta enfocada en dos objetivos:
1. Instalar/usar SIR en tu servidor.
2. Ubicar rapidamente los archivos de configuracion dentro de esta wiki.

## 1. Instalacion Basica
1. Copia el jar de SIR en `plugins/`.
2. Inicia el servidor una vez para generar archivos.
3. Deten el servidor y edita configuraciones.

## 2. Donde Esta Cada Configuracion En Esta Wiki
- Configuraciones base del plugin: `wiki/default-configs/`
- Configuraciones de modulos: `wiki/modules/<modulo>/`
- Configuraciones de commands: `wiki/command-providers/<provider>/`

## 3. Orden Recomendado de Configuracion
1. Edita primero `wiki/default-configs/config.yml`.
2. Ajusta `wiki/default-configs/webhooks.yml` y `wiki/default-configs/bossbars.yml` si los usaras.
3. Activa y configura modulos en `wiki/modules/` segun tu red.
4. Configura providers de comandos en `wiki/command-providers/`.

## 4. Archivos Clave
- Core:
  - `wiki/default-configs/config.yml`
  - `wiki/default-configs/webhooks.yml`
  - `wiki/default-configs/bossbars.yml`
- Chat/moderacion:
  - `wiki/modules/channels/channels.yml`
  - `wiki/modules/moderation/config.yml`
  - `wiki/modules/moderation/swearing.yml`
- Mensajes/eventos:
  - `wiki/modules/join-quit/messages.yml`
  - `wiki/modules/announcements/announcements.yml`
- Commands:
  - `wiki/command-providers/mute/commands.yml`
  - `wiki/command-providers/message/commands.yml`
  - `wiki/command-providers/settings/settings.yml`

## 5. Nota Importante
La carpeta `wiki/default-configs/` no incluye `plugin.yml` a proposito, tal como se solicito para la documentacion operativa.
