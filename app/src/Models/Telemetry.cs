/*
 * MEGA Ultra Booster
 * Copyright (c) 2026 IAmCreatingSomeCheats. All Rights Reserved.
 * Proprietary software — unauthorized use, copying, modification, or
 * distribution of this source code is prohibited. See LICENSE for terms.
 */

namespace MegaUltraBooster.Models;

/// <summary>Live stats streamed from the in-game mod over the link.</summary>
public sealed class Telemetry
{
    public int Fps { get; set; }
    public double FrameTimeMs { get; set; }
    public long MemUsedMb { get; set; }
    public long MemMaxMb { get; set; }
    public string Dimension { get; set; } = "";
    public string Server { get; set; } = "";
}
