# Publishing TurboBoost to CurseForge

Same jars as Modrinth (`dist/turboboost-1.0.0+mc<version>.jar` — run `./build-all.ps1`).
The difference: CurseForge projects are **created on the website and must be
approved** before files are public, and file uploads use **numeric game-version
IDs** (the `publish-to-curseforge.ps1` script handles that part).

---

## Step 1 — Account + author access
1. Sign in at <https://www.curseforge.com> (or the studio console
   <https://console.curseforge.com>).
2. If it's your first project, you may need to enable author/creator access.

## Step 2 — Create the project (website, needs approval)
Author dashboard → **Create Project** (Minecraft → **Mods**).

| Field | Value |
|-------|-------|
| **Project type** | Mods |
| **Name** | `TurboBoost` |
| **Summary** | `One-key FPS profiles, a live FPS/RAM overlay, dynamic FPS, and a smart server switcher — with an optional desktop companion app. Pairs with Sodium + Lithium.` |
| **Description** | paste `MODRINTH_DESCRIPTION.md` (CurseForge's editor takes Markdown/rich text) |
| **Categories** | `Utility & QoL` (add `Server Utility` if you like) |
| **Mod loader** | Fabric |
| **License** | **All Rights Reserved** |
| **Avatar/icon** | upload `modrinth-icon.png` |

Submit. New projects sit in a **moderation queue** (can take a day or more).

## Step 3 — Get the two things the script needs
1. **Project ID** — a number on your project page (and in its URL). Copy it.
2. **API token** — <https://legacy.curseforge.com/account/api-tokens> → generate one. Copy it.

🔒 Don't paste the token into chat — set it locally (below).

## Step 4 — Upload all versions
From this `mod/` folder, in PowerShell:

```powershell
$env:CURSEFORGE_TOKEN = "PASTE_TOKEN"
./build-all.ps1                                   # if dist/ isn't fresh
./publish-to-curseforge.ps1 -ProjectId 123456     # <- your numeric id
```

It looks up CurseForge's game-version IDs, then uploads one file per Minecraft
version (1.21.1 / 1.21.4 / 1.21.8 / 1.21.11), each tagged **Fabric** + the right
MC version, release type *release*. Type `yes` to confirm (or pass `-Yes`).

## Notes / gotchas
- The project must be **approved** before uploaded files are publicly visible —
  uploading during review is fine; files appear once approved.
- If the script errors with `401` → token missing the upload scope or wrong.
- If it errors that a game version `name` wasn't found → CurseForge may not have
  added that MC version yet, or names it differently; check
  `https://minecraft.curseforge.com/api/game/versions`.
- If the upload host itself 404s, CurseForge moved the upload API base — tell me
  and I'll repoint the script.
- License is **All Rights Reserved**; keep "Allow CurseForge to distribute this
  project" enabled so people can still download it.
