# Formatting Toolkit

SIR gives you fine-grained control over how text appears in chat, announcements, Discord, and private messages. This page summarises the tools available across the configuration files.

## Global Formatting Settings (`config.yml`)
- **Prefix management:**
  - `values.lang-prefix-key` triggers the main prefix anywhere it appears (default `<P>`).
  - `values.lang-prefix` is the formatted prefix injected into messages (default `&e&lSIR &8>&7`).
- **Centred text:** Prefix messages with `values.center-prefix` (default `<C>`) to centre them in chat.
- **Line breaks:** Use `values.line-separator` (default `<n>`) to create multi-line outputs, including titles and action bars.
- **Coloured console:** Toggle `options.colored-console` if your terminal supports ANSI colours.
- **Message type prefixing:** Enable `options.show-prefix` to see `[CHAT]`, `[ACTION-BAR]`, etc., in the console.

## Gradient & Rainbow Text
- `<G:#FF0000:#0000FF>` applies a gradient between two hex colours.
- `<R:1>` cycles through rainbow colours; adjust the number to change the step.
- These tags work anywhere SIR parses messages: chat, announcements, MOTDs, join messages, and Discord outputs.

## Interactive Components
Many YAML entries accept hover and click actions:
- **Hover lists:** Supply an array of lines under `hover:` (e.g., channel definitions, mentions, tags).
- **Click actions:** Use `SUGGEST:`, `RUN:`, or `URL:` prefixes inside `click-action` or mention `click` fields.
- **Mentions:** Configure sender/receiver hover and sound feedback separately in `modules/chat/mentions.yml`.
- **Announcements:** Combine chat lines, action bars (`[ACTION-BAR]`), and titles (`[TITLE] main<n>subtitle`).

## Sounds & Feedback
- Join, first-join, quit, and announcement entries can define a `sound:` value using vanilla sound keys.
- Mentions support distinct sounds for the sender and receiver.
- Discord messages can include embed colours to visually classify events.

## Tags & Emojis
- **Tags (`modules/chat/tags.yml`):** Create display prefixes with hover descriptions. Attach permissions, priorities, and groups.
- **Emojis (`modules/chat/emojis.yml`):** Replace keys such as `:heart:` with coloured text or Unicode. Checks include regex matching, word boundaries, case sensitivity, and permission/group requirements.

## Cooldowns & Feedback Messages
- Cooldowns reference `{time}` to communicate remaining wait time.
- Moderation modules provide `{player}`, `{type}`, `{message}` placeholders in log and notification formats.
- Mention messages can include `{sender}` and `{receiver}` for personalised notifications.

## Discord Embeds
Inside `modules/hook/discord.yml` you can:
- Set `embed.color` using named colours or hex values.
- Configure `author`, `thumbnail`, and `title` fields with placeholders like `{player}`, `{UUID}`, `{prefix}`, `{suffix}`.
- Toggle `timeStamp` to append the current time automatically.

## Boss Bars & Titles
- Announcements and other features that use Takion can trigger animated boss bars and layered titles. Use `[TITLE]` lines with `<n>` separators and let the module handle delivery.

## Formatting Best Practices
1. **Validate colour codes:** Hex colours require braces (`{#C0C0C0}`) in chat formats; `&#FFFFFF` also works in announcements.
2. **Escape quotes where needed:** Use `"` inside strings defined with double quotes.
3. **Stay consistent:** Keep similar modules (e.g., join messages) aligned in style for a cohesive presentation.
4. **Test in-game:** Gradients and centred text can look different depending on font width—preview before finalising.

With these tools you can build memorable experiences, from immersive story events to polished server announcements—all without leaving YAML.
