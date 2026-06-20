/*
 * MEGA Ultra Booster
 * Copyright (c) 2026 IAmCreatingSomeCheats. All Rights Reserved.
 * Proprietary software — unauthorized use, copying, modification, or
 * distribution of this source code is prohibited. See LICENSE for terms.
 */

using System.Diagnostics;
using System.IO;
using System.Text.Json;
using System.Text.Json.Nodes;

namespace MegaUltraBooster.Services;

/// <summary>
/// Bridges to the official Minecraft launcher: writes an "optimized profile" into
/// launcher_profiles.json (RAM + Aikar's flags, pointed at the installed Fabric
/// version) and opens the launcher. We deliberately don't reimplement Microsoft
/// account login — your normal launcher keeps handling auth.
/// </summary>
public sealed class MinecraftLauncher
{
    public string MinecraftDir { get; }

    public MinecraftLauncher(string? overrideDir = null)
    {
        MinecraftDir = overrideDir ?? Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".minecraft");
    }

    public bool MinecraftInstalled => Directory.Exists(MinecraftDir);
    public string ProfilesPath => Path.Combine(MinecraftDir, "launcher_profiles.json");

    /// <summary>Find an installed <c>fabric-loader-*</c> version id, preferring 1.21.11.</summary>
    public string? DetectFabricVersionId()
    {
        var versionsDir = Path.Combine(MinecraftDir, "versions");
        if (!Directory.Exists(versionsDir)) return null;

        var ids = Directory.GetDirectories(versionsDir)
            .Select(Path.GetFileName)
            .Where(n => n != null && n.StartsWith("fabric-loader", StringComparison.OrdinalIgnoreCase))
            .ToList();

        return ids.FirstOrDefault(n => n!.Contains("1.21.11")) ?? ids.FirstOrDefault();
    }

    public string PatchOptimizedProfile(int heapMb, string jvmArgs)
    {
        if (!File.Exists(ProfilesPath))
            return $"✖ launcher_profiles.json not found.\n   Run the official launcher once first ({MinecraftDir}).";

        try
        {
            var root = JsonNode.Parse(File.ReadAllText(ProfilesPath))!.AsObject();
            if (root["profiles"] is not JsonObject profiles)
            {
                profiles = new JsonObject();
                root["profiles"] = profiles;
            }

            var fabricId = DetectFabricVersionId();
            var profile = new JsonObject
            {
                ["name"] = "⚡ MEGA Ultra Boosted",
                ["type"] = "custom",
                ["javaArgs"] = jvmArgs,
                ["created"] = DateTime.UtcNow.ToString("yyyy-MM-ddTHH:mm:ss.fffZ"),
                ["lastUsed"] = DateTime.UtcNow.ToString("yyyy-MM-ddTHH:mm:ss.fffZ"),
            };
            if (fabricId != null) profile["lastVersionId"] = fabricId;

            // The launcher only keeps profiles whose key is a 32-char hex id, so use a
            // stable one (re-saving updates the same entry) and drop the old bad key.
            const string boostKey = "b0057b0057b0057b0057b0057b0057b0";
            profiles.Remove("megaultraboosted");
            profiles[boostKey] = profile;

            File.WriteAllText(ProfilesPath,
                root.ToJsonString(new JsonSerializerOptions { WriteIndented = true }));

            string ver = fabricId != null
                ? $" → '{fabricId}'"
                : " (install Fabric for your version, then re-apply)";
            return $"✔ Saved '⚡ MEGA Ultra Boosted' ({heapMb} MB heap{ver}). " +
                   "Keep the launcher CLOSED while saving (it overwrites the file when open), then open it.";
        }
        catch (Exception e)
        {
            return $"✖ Could not patch profile: {e.Message}";
        }
    }

    // Microsoft Store "AppsFolder" ids (stable per package family).
    private const string JavaLauncherAumid = @"Microsoft.4297127D64EC6_8wekyb3d8bbwe!Minecraft";
    private const string BedrockAumid = @"Microsoft.MinecraftUWP_8wekyb3d8bbwe!Game";

    /// <summary>Launch the Java edition via the official launcher (with the boosted profile).</summary>
    public string LaunchJava()
    {
        // Standalone (non-Store) installs first.
        string pf = Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles);
        string pfx86 = Environment.GetFolderPath(Environment.SpecialFolder.ProgramFilesX86);
        string local = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
        string[] candidates =
        {
            Path.Combine(pfx86, "Minecraft Launcher", "MinecraftLauncher.exe"),
            Path.Combine(pf,    "Minecraft Launcher", "MinecraftLauncher.exe"),
            Path.Combine(local, "Programs", "Minecraft Launcher", "MinecraftLauncher.exe"),
        };
        foreach (var exe in candidates)
        {
            if (File.Exists(exe))
            {
                try
                {
                    Process.Start(new ProcessStartInfo(exe) { UseShellExecute = true });
                    return "✔ Launching Java Minecraft… pick the '⚡ MEGA Ultra Boosted' profile.";
                }
                catch (Exception e) { return $"✖ {e.Message}"; }
            }
        }

        // Microsoft Store unified launcher (most installs) — by its app id.
        if (LaunchAumid(JavaLauncherAumid))
            return "✔ Launching Java Minecraft (Store launcher)… pick the '⚡ MEGA Ultra Boosted' profile.";

        return "✖ Java launcher not found. Open the Minecraft Launcher yourself and pick '⚡ MEGA Ultra Boosted'.";
    }

    /// <summary>Launch the Bedrock edition (UWP). System tweaks only — Bedrock has no Java mods.</summary>
    public string LaunchBedrock()
    {
        if (LaunchAumid(BedrockAumid))
            return "✔ Launching Minecraft Bedrock…";

        try
        {
            Process.Start(new ProcessStartInfo("minecraft://") { UseShellExecute = true });
            return "✔ Asked Windows to open Minecraft Bedrock…";
        }
        catch (Exception e)
        {
            return $"✖ Bedrock not found ({e.Message}). Install it from the Microsoft Store.";
        }
    }

    /// <summary>Open <c>.minecraft/mods</c> in Explorer (create it if missing).</summary>
    public string OpenModsFolder()
    {
        try
        {
            string mods = Path.Combine(MinecraftDir, "mods");
            Directory.CreateDirectory(mods);
            Process.Start(new ProcessStartInfo(mods) { UseShellExecute = true });
            return $"✔ Opened {mods}";
        }
        catch (Exception e)
        {
            return $"✖ Could not open mods folder: {e.Message}";
        }
    }

    /// <summary>Start a Microsoft Store app by its Application User Model ID.</summary>
    private static bool LaunchAumid(string aumid)
    {
        try
        {
            Process.Start(new ProcessStartInfo("explorer.exe", $@"shell:AppsFolder\{aumid}") { UseShellExecute = true });
            return true;
        }
        catch
        {
            return false;
        }
    }
}
