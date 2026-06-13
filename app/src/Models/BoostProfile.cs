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

    public static BoostProfile Aggressive() => new();

    public static BoostProfile Balanced() => new()
    {
        RenderDistance = 12,
        Particles = "decreased",
        EntityDistance = 0.75,
        GraphicsFast = true,
        CloudsOff = true,
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
