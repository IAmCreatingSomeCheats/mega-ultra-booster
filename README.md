# ⚡ MEGA Ultra Minecraft Booster

A two-part Minecraft performance booster for **Windows** + **Java Edition (Fabric 1.21.x)**:

1. **`mod/`** — **TurboBoost**, a Fabric client mod: one-key performance profiles,
   a live FPS/RAM overlay, dynamic FPS when the window is unfocused, and a
   **Smart Server Switcher**.
2. **`app/`** — **MEGA Ultra Booster**, a C#/.NET 8 WPF desktop app: auto RAM +
   JVM (Aikar) tuning, Windows system optimization, a one-click optimized
   launcher, and live server pinging.

The two halves talk to each other over a loopback socket (the **Live Link**), so
the big **BOOST** button in the app tunes the game *while you're playing*, and the
in-game quick-switch key asks the app to route you to a quieter server.

```
mega_ultra pc_booster/
├─ README.md            ← you are here
├─ shared/
│  ├─ LINK_PROTOCOL.md  ← the mod↔app wire protocol
│  └─ servers.example.json
├─ mod/                 ← Fabric mod (Java 21, Gradle)  → see mod/README.md
└─ app/                 ← WPF app (.NET 8)              → see app/README.md
```

## What it does

| Feature | Lives in | Notes |
|---|---|---|
| Auto RAM allocation + Aikar's GC flags | app | Reads your hardware, picks a safe Xmx/Xms, writes an optimized profile |
| Windows system optimization | app | High-Performance power plan, process priority, trim working sets, Game Mode, optional bloatware suspend |
| One-click optimized launcher | app | Patches a profile in the official launcher and launches it |
| Live FPS / RAM overlay | mod | Toggle with a key or from the app |
| One-key performance profile | mod | Fast graphics, clouds off, minimal particles, no entity shadows, tuned render/entity distance |
| Dynamic FPS when unfocused | mod | Big battery/heat win on laptops |
| Smart Server Switcher | both | One key → app picks the quietest server in a category → mod connects you |
| Live BOOST + telemetry | both | App button tunes the running game; game streams FPS/RAM back to the app |

## ⚠️ Honest scope notes (read these)

* **This mod does not replace [Sodium] + [Lithium].** Those are years of
  rendering-/tick-engine work and will always beat a from-scratch mod at the raw
  engine level. TurboBoost is designed to run **alongside** them — it automates
  settings, adds the HUD + server tools + app link, and does *not* re-implement
  their rendering optimizations. **Install Sodium + Lithium for the biggest FPS
  gains, then TurboBoost on top.**
* **"Easier servers with noobs":** no API exposes how skilled the players on a
  random server are, so the booster does not fake that. Instead you **tag**
  servers as `Casual/Beginner`, and the switcher auto-routes you to the
  **lowest-population, lowest-ping** one in that group. See
  [`shared/LINK_PROTOCOL.md`](shared/LINK_PROTOCOL.md).
* **The launcher does not do Microsoft account login.** Full auth is a launcher
  in its own right (Prism/official do this). Instead the app **patches an
  optimized profile into your existing official launcher** and launches it, so
  your normal login keeps working. Advanced users can also run a raw `java`
  command.

[Sodium]: https://modrinth.com/mod/sodium
[Lithium]: https://modrinth.com/mod/lithium

## Quick start

1. **Mod:** `cd mod` → `./gradlew build` → drop `build/libs/turboboost-1.0.0.jar`
   into `.minecraft/mods` (with Fabric Loader + Fabric API + ideally Sodium +
   Lithium). See [`mod/README.md`](mod/README.md).
2. **App:** install the [.NET 8 SDK](https://dotnet.microsoft.com/download), then
   `cd app` → `dotnet run`. See [`app/README.md`](app/README.md).
3. Launch Minecraft, keep the app open — the mod auto-connects to the app, and
   the overlay shows `LINK ✔`.
