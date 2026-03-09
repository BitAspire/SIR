# SIR Wiki

Esta wiki fue reorganizada para que coincida con la estructura real del proyecto y para que sea facil mantenerla junto a los archivos por defecto.

## Navegacion Rapida
- [Inicio Rapido](Getting-Started.md)
- [Configuraciones Por Defecto (sin plugin.yml)](default-configs/README.md)
- [Modulos](modules/README.md)
- [Command Providers](command-providers/README.md)
- [Referencia de Modulos](Module-Reference.md)
- [Referencia de Comandos](Command-Reference.md)
- [Canales de Chat](Chat-Channels.md)
- [Integraciones](Integrations.md)
- [Toolkit de Formato](Formatting-Toolkit.md)

## Que Cambio
- Se agrego `wiki/default-configs/` con configuraciones por defecto del plugin base.
- Se excluyo intencionalmente `plugin.yml` de esa carpeta.
- Se agrego `wiki/modules/` con una carpeta por modulo y sus archivos reales.
- Se agrego `wiki/command-providers/` con una carpeta por provider y sus archivos reales.

## Convencion de Mantenimiento
- Fuente de verdad de defaults base: `plugin/src/main/resources`.
- Fuente de verdad de modulos: `module/<modulo>/src/main/resources`.
- Fuente de verdad de command providers: `command/<provider>/src/main/resources`.
- La carpeta `wiki/` contiene copias de referencia para documentacion y consulta rapida.
