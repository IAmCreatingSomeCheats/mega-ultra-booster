/*
 * MEGA Ultra Booster
 * Copyright (c) 2026 IAmCreatingSomeCheats. All Rights Reserved.
 * Proprietary software — unauthorized use, copying, modification, or
 * distribution of this source code is prohibited. See LICENSE for terms.
 */

using System.Text.Json.Nodes;

namespace MegaUltraBooster.Models;

/// <summary>The set of video-option targets pushed to the mod by the BOOST button.</summary>
public sealed class BoostProfile
{
    public int RenderDistance { get; set; } = 8;
    public int MaxFps { get; set; } = 0;       // 0 = unlimited
    public bool GraphicsFast { get; set; } = true;
    public bool CloudsOff { get; set; } = true;
    public string Particles { get; set; } = "minimal";
    public bool EntityShadows { get; set; } = false;
    public bool Vsync { get; set; } = false;
    public double EntityDistance { get; set; } = 0.5;
    public bool DynamicFps { get; set; } = true;

    public static BoostProfile Aggressive() => Potato();

    /// <summary>Max FPS — lowest settings (matches the mod's Potato preset).</summary>
    public static BoostProfile Potato() => new()
    {
        RenderDistance = 4,
        Particles = "minimal",
        EntityDistance = 0.5,
        GraphicsFast = true,
        CloudsOff = true,
        EntityShadows = false,
    };

    public static BoostProfile Balanced() => new()
    {
        RenderDistance = 8,
        Particles = "decreased",
        EntityDistance = 0.75,
        GraphicsFast = true,
        CloudsOff = true,
        EntityShadows = false,
    };

    /// <summary>Lightest boost — keeps it pretty (fancy graphics, clouds, shadows).</summary>
    public static BoostProfile Quality() => new()
    {
        RenderDistance = 12,
        Particles = "all",
        EntityDistance = 1.0,
        GraphicsFast = false,
        CloudsOff = false,
        EntityShadows = true,
    };

    public JsonObject ToJson() => new()
    {
        ["renderDistance"] = RenderDistance,
        ["maxFps"] = MaxFps,
        ["graphicsFast"] = GraphicsFast,
        ["cloudsOff"] = CloudsOff,
        ["particles"] = Particles,
        ["entityShadows"] = EntityShadows,
        ["vsync"] = Vsync,
        ["entityDistance"] = EntityDistance,
        ["dynamicFps"] = DynamicFps,
    };
}
