# ⚡ TurboBoost

**One-key performance profiles, a live FPS/RAM overlay, dynamic FPS, and a smart server switcher — with an optional desktop companion app.**

TurboBoost is the in-game half of the **MEGA Ultra Booster**. It automates the
settings tweaking you'd normally do by hand, shows you exactly what your game is
doing, and (optionally) links up to a desktop app that tunes the running game and
optimizes your PC.

> 💡 **Designed to run *alongside* [Sodium](https://modrinth.com/mod/sodium) +
> [Lithium](https://modrinth.com/mod/lithium), not replace them.** Those mods do
> the heavy rendering-/tick-engine optimization. TurboBoost handles the
> automation, overlay, server tools, and app link on top. Install all three for
> the best result.

## Features

- **🚀 One-key Boost profile** — instantly applies a tuned set of video options
  (fast graphics, clouds off, minimal particles, no entity shadows, vsync off,
  sensible render/entity distance). One key to go fast, your settings restored
  when you want them back.
- **📊 Live FPS / RAM overlay** — a clean on-screen readout of FPS, frame time,
  JVM memory, current server, and link status. Toggle it with a key.
- **🔋 Dynamic FPS** — automatically throttles the framerate while the window is
  unfocused or minimized. Big heat/battery win on laptops.
- **🧭 Smart Server Switcher** — keep a list of your servers grouped by category;
  one keypress jumps you to the quietest, lowest-ping server in a category you
  choose (great for hopping to a relaxed, low-population server).
- **🔗 Live link to the desktop app** *(optional)* — the companion app's **BOOST**
  button tunes the game in real time, your FPS/RAM stream back to the app, and
  the server switcher can ask the app to live-ping your servers and pick the best
  one. TurboBoost works fully on its own if you don't run the app.

## Default keybinds (all rebindable in Controls)

| Key | Action |
|-----|--------|
| **F6** | Toggle the FPS/RAM overlay |
| **F7** | Apply the Boost profile |
| **K**  | Quick-switch to the quietest server in your category |

## Requirements

- **Minecraft 1.21.1, 1.21.4, 1.21.8, or 1.21.11** (Fabric) — download the jar matching your version
- **[Fabric API](https://modrinth.com/mod/fabric-api)** — required
- **[Sodium](https://modrinth.com/mod/sodium)** + **[Lithium](https://modrinth.com/mod/lithium)** — strongly recommended (not bundled)

## Companion app

The optional desktop app adds auto RAM + JVM (Aikar) tuning, Windows system
optimization, a one-click optimized launcher, and live server pinging. It's
not required — the mod is fully functional standalone.

---

*Client-side only. Safe on vanilla-compatible servers — it only changes your own
video settings and which server you connect to.*
