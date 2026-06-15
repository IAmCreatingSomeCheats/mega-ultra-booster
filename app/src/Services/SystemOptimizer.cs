/*
 * MEGA Ultra Booster
 * Copyright (c) 2026 IAmCreatingSomeCheats. All Rights Reserved.
 * Proprietary software — unauthorized use, copying, modification, or
 * distribution of this source code is prohibited. See LICENSE for terms.
 */

using System.Diagnostics;
using System.Runtime.InteropServices;
using MegaUltraBooster.Native;
using Microsoft.Win32;

namespace MegaUltraBooster.Services;

/// <summary>
/// Windows-side tweaks. Everything is best-effort and wrapped in try/catch so a
/// blocked operation (e.g. a protected process) never takes the app down.
/// None of this requires admin for same-user processes / the current power plan.
/// </summary>
public sealed class SystemOptimizer
{
    // Built-in Windows power scheme GUIDs.
    private const string HighPerformance = "8c5e7fda-e8bf-4a96-9a85-a6e23a8c635c";
    private const string Balanced = "381b4222-f694-41f0-9685-ff5bb260df2e";

    // "Minecraft.Windows" is the Bedrock (UWP) process; the rest are Java.
    private static readonly string[] McProcessNames = { "javaw", "java", "Minecraft", "Minecraft.Windows", "MinecraftLauncher" };

    /// <summary>Critical processes the suspend feature must never touch.</summary>
    private static readonly HashSet<string> SuspendDenyList = new(StringComparer.OrdinalIgnoreCase)
    {
        "system", "idle", "explorer", "csrss", "wininit", "winlogon", "services",
        "lsass", "smss", "svchost", "dwm", "fontdrvhost", "MegaUltraBooster",
        "javaw", "java", "Minecraft", "Minecraft.Windows"
    };

    public (long totalMb, long availMb) GetMemory()
    {
        var s = new NativeMethods.MEMORYSTATUSEX { dwLength = (uint)Marshal.SizeOf<NativeMethods.MEMORYSTATUSEX>() };
        if (NativeMethods.GlobalMemoryStatusEx(ref s))
            return ((long)(s.ullTotalPhys / 1048576UL), (long)(s.ullAvailPhys / 1048576UL));
        return (0, 0);
    }

    public string SetHighPerformancePowerPlan() => SetPowerPlan(HighPerformance, "High-Performance power plan");

    public string RestoreBalancedPowerPlan() => SetPowerPlan(Balanced, "Balanced power plan");

    private string SetPowerPlan(string guid, string label)
    {
        try
        {
            RunHidden("powercfg.exe", $"/setactive {guid}");
            return $"✔ {label} active";
        }
        catch (Exception e)
        {
            return $"✖ Power plan: {e.Message}";
        }
    }

    /// <summary>Raise the priority of running Minecraft/Java processes to High.</summary>
    public string PrioritizeMinecraft()
    {
        int count = 0;
        foreach (var name in McProcessNames)
        {
            foreach (var p in SafeGetByName(name))
            {
                try
                {
                    p.PriorityClass = ProcessPriorityClass.High;
                    count++;
                }
                catch { /* access denied / exited */ }
                finally { p.Dispose(); }
            }
        }
        return count > 0 ? $"✔ Set {count} Minecraft process(es) to High priority"
                         : "• No Minecraft process running yet";
    }

    /// <summary>Trim every accessible process's working set, returning RAM to the pool.</summary>
    public string FreeStandbyMemory()
    {
        int trimmed = 0;
        foreach (var p in Process.GetProcesses())
        {
            try
            {
                if (NativeMethods.EmptyWorkingSet(p.Handle)) trimmed++;
            }
            catch { /* protected process */ }
            finally { p.Dispose(); }
        }
        return $"✔ Trimmed working sets of {trimmed} process(es)";
    }

    /// <summary>Enable/disable Windows Game Mode via the current-user registry keys.</summary>
    public string SetGameMode(bool on)
    {
        try
        {
            using var key = Registry.CurrentUser.CreateSubKey(@"Software\Microsoft\GameBar");
            key.SetValue("AllowAutoGameMode", on ? 1 : 0, RegistryValueKind.DWord);
            key.SetValue("AutoGameModeEnabled", on ? 1 : 0, RegistryValueKind.DWord);
            return on ? "✔ Game Mode enabled" : "• Game Mode disabled";
        }
        catch (Exception e)
        {
            return $"✖ Game Mode: {e.Message}";
        }
    }

    /// <summary>Suspend user-named background apps (opt-in). Skips the deny-list.</summary>
    public string SuspendByNames(IEnumerable<string> names) => SetSuspended(names, true);

    public string ResumeByNames(IEnumerable<string> names) => SetSuspended(names, false);

    private string SetSuspended(IEnumerable<string> names, bool suspend)
    {
        int affected = 0;
        foreach (var raw in names)
        {
            var name = raw.Trim();
            if (name.Length == 0 || SuspendDenyList.Contains(name)) continue;
            foreach (var p in SafeGetByName(name))
            {
                try
                {
                    uint r = suspend ? NativeMethods.NtSuspendProcess(p.Handle)
                                     : NativeMethods.NtResumeProcess(p.Handle);
                    if (r == 0) affected++;
                }
                catch { /* access denied */ }
                finally { p.Dispose(); }
            }
        }
        string verb = suspend ? "Paused" : "Resumed";
        return $"✔ {verb} {affected} process(es)";
    }

    private static IEnumerable<Process> SafeGetByName(string name)
    {
        try { return Process.GetProcessesByName(name); }
        catch { return Array.Empty<Process>(); }
    }

    private static void RunHidden(string exe, string args)
    {
        var psi = new ProcessStartInfo(exe, args)
        {
            CreateNoWindow = true,
            UseShellExecute = false,
            WindowStyle = ProcessWindowStyle.Hidden,
        };
        using var p = Process.Start(psi);
        p?.WaitForExit(4000);
    }
}
