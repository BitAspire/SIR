# Chat Channels

SIR’s channel system lets you create layered chat experiences without editing code. Everything is managed from `resources/modules/chat/channels.yml`.

## File Structure Overview
```yaml
default-channel: {...}
channels:
  default: {...}
  vip: {...}
  local-staff-channel: {...}
```
- **`default-channel`** defines fallback values for every setting.
- **`channels`** lists each active channel. Entries inherit unspecified options from `default-channel`.
- A channel can also declare a **`local`** subsection to create a child channel that shares formatting but limits range.

## Default Channel Blueprint
`default-channel` is your safety net. Use it to decide:
- Whether channels are enabled by default (`enabled`).
- Base colour permissions (`color-options.normal`, `special`, `rgb`).
- Default radius (`0` = global, anything above `0` limits chat to that many blocks).
- Cooldown behaviour and warning message.
- Fallback message format.

Any channel that omits a property automatically pulls the value from this section.

## Creating a Global Channel
1. Add a new entry under `channels:` (e.g., `staff:`).
2. Configure optional fields:
   - `permission`: Who can join the channel.
   - `priority`: Higher numbers take precedence when multiple channels compete.
   - `group`: Require a specific permission-group name.
   - `prefix` / `suffix`: Visual markers that wrap the player name.
   - `color`: Default colour applied to `{message}`.
   - `color-options`: Override which colour codes are allowed.
   - `hover`: A list of lines that appears when players hover the chat line.
   - `click-action`: Create suggest/run/URL actions (`SUGGEST:/msg {player}` etc.).
   - `format`: Final render for the message. Available placeholders include `{player}`, `{message}`, `{prefix}`, `{suffix}`, `{color}`.
3. Set `global: true` (or omit it, because the default radius of `0` already makes the channel global).
4. Save the file and reload with `/sir reload`.

## Adding Local Channels
You can either create a standalone local channel (`global: false`) or a local subchannel inside a global entry.

### Standalone Local Channel
```yaml
local-market:
  global: false
  permission: chat.market
  radius: 100
  access:
    prefix: "!"
    commands:
      - /market
  format: '[MARKET] &7{player}&8: {color}{message}'
```
- Players type the prefix (e.g., `!Hello`) or use the `/market` command to send to this channel.
- If `radius` is omitted, the value from `default-channel` is used.

### Local Subchannel (Child)
```yaml
vip:
  permission: chat.vip
  format: '{prefix} {player}: {color}{message}'
  local:
    permission: chat.vip.local
    radius: 50
    access:
      prefix: "@"
      commands:
        - /viplocal
    format: '[VIP-LOCAL] {prefix} {player}: {message}'
```
- The local entry inherits all properties from its parent (`vip`) unless you override them.
- Useful for toggling between server-wide VIP chat and a nearby whisper with the same branding.

## Access Controls & Quality-of-Life
- **Permissions:** Define `permission` on the channel and on any local variant to control membership separately.
- **Groups:** Optional string that matches the player’s permission-group (handy when multiple permissions share the same tag).
- **Priority:** Ensures the highest-priority channel claims the message if multiple channels share the same prefix.
- **Commands & prefixes:** Give players multiple ways to access local channels. Commands appear in tab completion when registered.
- **World filters:** Add a `worlds:` list to restrict where the channel is available.

## Colour Governance
- `color-options.normal` toggles standard `&0`-`&f` colours.
- `color-options.special` toggles `&l`, `&o`, `&n`, `&m`, `&k` formatting.
- `color-options.rgb` enables hex colours such as `{#C0C0C0}`.
- Combine these with the `color` field to provide a default tone while still letting players customise within limits.

## Cooldowns & Spam Control
- Use the global `default-channel.cooldown` to apply a base delay between messages.
- Override per channel if needed by adding a `cooldown:` block to the channel entry.
- The `{time}` placeholder inside the cooldown message tells players how long they must wait.

## Troubleshooting
- **Players see the wrong format:** Ensure the target channel has the highest `priority` among the channels they can access.
- **Local prefixes not working:** Double-check the `access.prefix` value and confirm `global: false` (or the local child) is enabled.
- **Hex colours ignored:** Make sure `color-options.rgb` is `true` either on the channel or inherited from `default-channel`.

Design channels that match your community’s needs—SIR’s inheritance model keeps configurations tidy and predictable.
