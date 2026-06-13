# MEGA Ultra Booster (desktop app)

The Windows half: a WPF dashboard that optimizes your system + JVM, launches
Minecraft with the right settings, and live-links to the TurboBoost mod.

> ✅ Built & verified on **.NET 10** (SDK 10.0.301). The window opens, the live
> link listens on `127.0.0.1:38910`, and the full mod→app→mod round-trip
> (telemetry + "find quietest server" → `switch_server`) was tested working.

## Requirements

* Windows 10/11
* **.NET 10 SDK** (you already have 10.0.301) — https://dotnet.microsoft.com/download

## Run

From this `app/` folder:

```powershell
dotnet run
```

or build a standalone exe:

```powershell
dotnet build -c Release
# → bin/Release/net10.0-windows/MegaUltraBooster.exe
```

No admin rights needed. Everything is best-effort: a blocked tweak (e.g. a
protected process) is logged and skipped, never fatal.

## What the buttons do

| Control | Effect |
|---|---|
| **🚀 BOOST NOW** | High-Performance power plan · sets Minecraft to High priority · trims standby RAM · enables Game Mode · pushes the in-game profile to the mod (if linked) |
| **RAM slider** | Picks the Xmx/Xms; auto-recommended from your installed RAM |
| **💾 Save profile** | Writes an "⚡ MEGA Ultra Boosted" profile into the official launcher with your RAM + Aikar's flags, bound to your installed Fabric version |
| **▶ Launch Minecraft** | Opens the official launcher — pick the boosted profile |
| **📡 Ping all** | Live player-count + latency for every saved server |
| **🎯 Send me to quietest** | Pings the chosen category and routes the game to the emptiest, lowest-ping one |
| **Pause / Resume** | Optionally suspend named background apps (system processes are protected) |

## The "easier servers" feature, honestly

No service exposes how skilled players on a random server are. So instead of
faking it, you **tag** servers `Casual/Beginner` and the booster routes you to the
**quietest (fewest players), lowest-ping** one in that group — live-pinged each
time. In-game that's the **K** key; here it's **🎯 Send me to quietest**.

## Files & ports

* Saved servers: `%APPDATA%\MegaUltraBooster\servers.json`
* Live link: TCP `127.0.0.1:38910` (loopback only)
* Optimized launcher profile: written into `%APPDATA%\.minecraft\launcher_profiles.json`

## Scope notes

* **No Microsoft-account login.** The app patches a profile into your existing
  official launcher and opens it, so your normal auth keeps working.
* Project layout: UI in [`MainWindow.xaml`](MainWindow.xaml) /
  [`.cs`](MainWindow.xaml.cs); logic in [`src/Services/`](src/Services);
  protocol in [`../shared/LINK_PROTOCOL.md`](../shared/LINK_PROTOCOL.md).
