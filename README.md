# 🤖 SIR – Simple In-game Receptionist

Welcome to **SIR**, the versatile and powerful Minecraft plugin that transforms server management and player interaction! With an extensive modular architecture and feature-rich tools, SIR is built to streamline your server’s chat, announcements, and overall communication systems.

---

## 🌟 Overview

**SIR** is designed with simplicity and efficiency in mind. Whether you need advanced chat management, customizable join/quit messages, automated announcements, or deep integration with other plugins, SIR has you covered.

- **Modular Design:** Activate only the modules you need.
- **User-Friendly:** Easy configuration and intuitive command system.
- **Highly Customizable:** Supports extensive formatting, colors, and emojis to enhance the player experience.
- **Optimized Performance:** Engineered to handle busy servers with minimal resource consumption.

---

## 🔧 Key Features

- **Chat Management System** 🗣️  
  Control chat channels, implement anti-spam cooldowns, and format messages using gradients, colors, and emojis.

- **Custom Join/Quit Messages** 👋  
  Automatically display personalized messages for players when they join or leave the server.

- **Automated Announcements** 📢  
  Schedule announcements with custom formats, sound effects, and commands, all adjustable with tick-based intervals.

- **Server MOTD System** 📝  
  Manage server list messages (MOTDs) with dynamic player count and server icons for that extra professional touch.

- **Advanced Moderation Tools** 🛡️  
  Filter swearing, manage excessive caps, and control link posting to keep your chat clean and fun.

- **Plugin Integrations** 🔌  
  Seamlessly integrate with popular plugins like DiscordSRV, PlaceholderAPI, Vault, and many more for an enhanced experience.

---

## ⚙️ Technical Details

- **API Version:** 1.13+  
- **Language:** Java  
- **Required Dependency:** [Takion](https://github.com/CroaBeast/Takion)  
- **Extensible Framework:** Supports additional integrations through a modular system and includes extensive APIs for developers.

---

## 📦 Module System

SIR’s modular design lets you tailor the plugin to your server’s needs. The modules fall under three main categories:

### 🎮 Core Modules
1. **Advancements** 🏆  
   Customize achievement notifications with rewards and commands.
2. **Announcements** 📢  
   Schedule and format automated messages and broadcasts.
3. **Join/Quit** 👋  
   Set up welcoming and farewell messages with special effects.
4. **MOTD** 📝  
   Craft attractive server list messages with dynamic player counts.

### 💬 Chat Modules
5. **Channels** 🔊  
   Manage global and local chat channels with custom permissions and formatting.
6. **Cooldowns** ⏳  
   Implement anti-spam measures and controlled message sending.
7. **Emojis** 😊  
   Replace text with colorful, custom emoji shortcuts.
8. **Mentions** 📧  
   Enable interactive player mentions with click and hover effects.
9. **Tags** 🏷️  
   Create custom chat tags for different player groups.
10. **Moderation** 🛡️  
    Enforce chat rules by blocking swearing, excessive caps, and unwanted links.

### 🔗 Integration Modules
11. **Discord** 🎮  
    Connect Minecraft chat with Discord channels via DiscordSRV.
12. **Login** 🔑  
    Support authentication plugins such as AuthMe and NexAuth.
13. **Vanish** 👻  
    Integrate with vanish plugins to manage invisible players in chat.

---

## 🌈 Dynamic Text & Formatting

SIR not only supports basic text messaging but lets you get creative:

- **Color Gradients & Rainbow Text:**  
  Use simple tags to create eye-catching gradients and rainbow effects.
- **Unicode and Small Caps:**  
  Easily transform text to small caps or include special Unicode characters.
- **Interactive Chat Components:**  
  Design messages that respond to hover and click actions, triggering commands or displaying tooltips.

Example:
```yaml
message: "<g:#FF0000:#0000FF>Amazing Text</g> <hover:\"Click for help\"|run:\"/help\">Need Help?</text>"
```

---

## 📝 Commands & GUI Integration

SIR offers an array of commands grouped under four categories:

### 🛠️ Administrative Commands
- `/sir` – The primary command for administration and configuration.
- `/print` – Broadcast raw messages with advanced formatting.
- `/announcer` – Manage your automated announcements.

### 💬 Chat Management
- `/chatview` – Toggle chat channel visibility.
- `/ignore` – Ignore messages from specific players.
- `/clearchat` – Quickly clear the chat screen.

### 📨 Messaging System
- `/msg` – Send private messages to fellow players.
- `/reply` – Quickly reply to recent messages.

### 🔇 Moderation Suite
- `/mute`, `/tempmute`, `/unmute`, `/checkmute` – Commands to handle player moderation and enforce chat rules.

All command configurations are accessible through `commands.yml` or via an intuitive GUI interface, making it simple to adjust settings on the fly.

---

## 🔄 Automatic Updates & Configuration

SIR comes with an **automatic update system** to keep your configurations fresh:
- Enable updater settings in your `config.yml` for notifications on startup.
- Support for dynamic configuration reloads via `/sir reload` ensures changes take effect immediately without disrupting gameplay.

---

## 🔗 Quick Links

- **Documentation:** [SIR Wiki](https://github.com/CroaBeast/SIR/wiki)
- **Discord Support:** [Join Our Discord](https://discord.gg/s9YFGMrjyF)
- **Issue Tracker:** [Report Issues](https://github.com/CroaBeast/SIR/issues)
- **Change Log:** [View Commits](https://github.com/CroaBeast/SIR/commits/main)

---

## 🛡️ Why Choose SIR?

- **Clean, Intuitive Design:** Enjoy streamlined chat and moderation, making server management hassle-free.
- **Flexible Configuration:** Customize every aspect using YAML files or a GUI.
- **Outstanding Performance:** Optimized for efficiency, even on high-traffic servers.
- **Robust Plugin Integration:** Connect seamlessly with popular plugins to expand functionalities.
- **Active Community & Developer Support:** Join an engaged community with continuous updates and improvements.

---

## 🎯 Final Thoughts

With **SIR**, you’re not just installing a plugin—you're upgrading your server’s communication framework. The combination of dynamic message formatting, complete module control, and extensive integration options makes SIR the ultimate solution for server administrators aiming for a next-level Minecraft experience. 

> *Powered by [Takion](https://github.com/CroaBeast/Takion) and [PrismaticAPI](https://github.com/CroaBeast/PrismaticAPI), SIR has been making server management simple and efficient since 2021.* 🚀

Feel free to explore the documentation further for in-depth configuration examples, command usage, and module management tips. Happy crafting! 🎉
