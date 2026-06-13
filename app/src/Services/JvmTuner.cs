using System.Text;

namespace MegaUltraBooster.Services;

/// <summary>
/// Picks a sensible heap size for the machine and builds Aikar's G1GC flags.
/// (Aikar's flags: https://docs.papermc.io/paper/aikars-flags — widely used for
/// smooth Minecraft frame times.)
/// </summary>
public sealed class JvmTuner
{
    /// <summary>
    /// Recommend an Xmx/Xms in MB: about half of RAM, always leaving ~4 GB for the
    /// OS and the rest, clamped to a practical 2–8 GB window.
    /// </summary>
    public int RecommendHeapMb(long totalRamMb)
    {
        if (totalRamMb <= 0) return 4096;
        long half = totalRamMb / 2;
        long leaveHeadroom = totalRamMb - 4096; // never starve the OS
        long target = Math.Min(half, leaveHeadroom);
        target = Math.Clamp(target, 2048, 8192);
        // round down to the nearest 512 MB
        return (int)(target / 512 * 512);
    }

    /// <summary>Full Aikar's flags string with Xms == Xmx == <paramref name="heapMb"/>.</summary>
    public string BuildAikarFlags(int heapMb)
    {
        bool large = heapMb >= 12288; // >12 GB uses the large-heap variant
        var sb = new StringBuilder();
        sb.Append($"-Xms{heapMb}M -Xmx{heapMb}M ");
        sb.Append("-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 ");
        sb.Append("-XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch ");
        sb.Append(large
            ? "-XX:G1NewSizePercent=40 -XX:G1MaxNewSizePercent=50 -XX:G1HeapRegionSize=16M -XX:G1ReservePercent=15 -XX:InitiatingHeapOccupancyPercent=20 "
            : "-XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:InitiatingHeapOccupancyPercent=15 ");
        sb.Append("-XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 ");
        sb.Append("-XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 ");
        sb.Append("-XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 ");
        sb.Append("-Dusing.aikars.flags=https://mcflags.emc.gs -Daikars.new.flags=true");
        return sb.ToString();
    }
}
