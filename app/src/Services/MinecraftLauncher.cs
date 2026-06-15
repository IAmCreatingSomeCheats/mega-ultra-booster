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

            profiles["megaultraboosted"] = profile;
            File.WriteAllText(ProfilesPath,
                root.ToJsonString(new JsonSerializerOptions { WriteIndented = true }));

            string ver = fabricId != null
                ? $" → version '{fabricId}'"
                : " (install Fabric for 1.21.11, then re-apply to bind the version)";
            return $"✔ Optimized profile saved: {heapMb} MB heap{ver}";
        }
        catch (Exception e)
        {
            return $"✖ Could not patch profile: {e.Message}";
        }
    }

    /// <summary>Launch the Java edition via the official launcher (with the boosted profile).</summary>
    public string LaunchJava()
    {
        string pf = Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles);
        string pfx86 = Environment.GetFolderPath(Environment.SpecialFolder.ProgramFilesX86);
        string[] candidates =
        {
            Path.Combine(pfx86, "Minecraft Launcher", "MinecraftLauncher.exe"),
            Path.Combine(pf,    "Minecraft Launcher", "MinecraftLauncher.exe"),
            Path.Combine(pfx86, "Minecraft", "MinecraftLauncher.exe"),
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

        // NOTE: deliberately no "minecraft://" fallback here — that URI is registered
        // by the Bedrock UWP app on most systems, so it would open Bedrock instead.
        return "✖ Java launcher not found. Open the Minecraft Launcher yourself and pick '⚡ MEGA Ultra Boosted'.";
    }

    /// <summary>Launch the Bedrock edition (UWP). System tweaks only — Bedrock has no Java mods.</summary>
    public string LaunchBedrock()
    {
        // Standard way to start a UWP app by its Application User Model ID.
        try
        {
            Process.Start(new ProcessStartInfo("explorer.exe",
                @"shell:AppsFolder\Microsoft.MinecraftUWP_8wekyb3d8bbwe!App") { UseShellExecute = true });
            return "✔ Launching Minecraft Bedrock…";
        }
        catch
        {
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
    }
}
