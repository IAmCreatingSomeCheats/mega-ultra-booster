using System.IO;
using System.Text.Json;
using System.Text.Json.Serialization;
using MegaUltraBooster.Models;

namespace MegaUltraBooster.Services;

/// <summary>
/// Loads/saves the saved-server list (servers.json under %APPDATA%) and runs the
/// "find the quietest server in a category" routine that backs the in-game
/// quick-switch key.
/// </summary>
public sealed class ServerStore
{
    private static readonly JsonSerializerOptions JsonOpts = new()
    {
        WriteIndented = true,
        DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull,
    };

    private readonly ServerPinger _pinger = new();

    public List<ServerEntry> Servers { get; private set; } = new();

    public static string DataDir =>
        Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "MegaUltraBooster");

    public static string FilePath => Path.Combine(DataDir, "servers.json");

    public void Load()
    {
        try
        {
            if (File.Exists(FilePath))
            {
                var root = JsonSerializer.Deserialize<Root>(File.ReadAllText(FilePath), JsonOpts);
                Servers = root?.Servers ?? new List<ServerEntry>();
                return;
            }
        }
        catch { /* fall through to defaults */ }

        Servers = DefaultServers();
        Save();
    }

    public void Save()
    {
        try
        {
            Directory.CreateDirectory(DataDir);
            var root = new Root { Servers = Servers };
            File.WriteAllText(FilePath, JsonSerializer.Serialize(root, JsonOpts));
        }
        catch { /* non-fatal */ }
    }

    public IEnumerable<string> Categories =>
        Servers.Select(s => s.Category).Where(c => !string.IsNullOrWhiteSpace(c))
               .Distinct(StringComparer.OrdinalIgnoreCase);

    /// <summary>Ping one server and write the result back onto its bound row.</summary>
    public async Task PingOneAsync(ServerEntry s)
    {
        s.Status = "pinging…";
        var r = await _pinger.PingAsync(s.Address);
        if (r.Ok)
        {
            s.Online = r.Online;
            s.Max = r.Max;
            s.Ping = r.LatencyMs;
            s.Status = string.IsNullOrWhiteSpace(r.Motd) ? "online" : r.Motd;
        }
        else
        {
            s.Online = -1; s.Max = -1; s.Ping = -1;
            s.Status = "offline";
        }
    }

    public async Task PingAllAsync()
    {
        await Task.WhenAll(Servers.Select(PingOneAsync));
    }

    /// <summary>
    /// Pings every server in <paramref name="category"/> and returns the one with the
    /// fewest players online (ties broken by lowest ping). This is the honest version
    /// of "send me somewhere quieter" — it ranks YOUR tagged servers, live.
    /// </summary>
    public async Task<ServerEntry?> PickQuietestAsync(string category)
    {
        var candidates = Servers
            .Where(s => string.Equals(s.Category, category, StringComparison.OrdinalIgnoreCase))
            .ToList();
        if (candidates.Count == 0) return null;

        await Task.WhenAll(candidates.Select(PingOneAsync));

        var reachable = candidates.Where(s => s.Online >= 0).ToList();
        var pool = reachable.Count > 0 ? reachable : candidates; // if none reachable, still return something

        return pool
            .OrderBy(s => s.Online < 0 ? int.MaxValue : s.Online)
            .ThenBy(s => s.Ping < 0 ? int.MaxValue : s.Ping)
            .First();
    }

    private static List<ServerEntry> DefaultServers() => new()
    {
        new ServerEntry { Name = "Hypixel", Address = "mc.hypixel.net", Category = "Main", Notes = "Big network" },
        new ServerEntry { Name = "Quiet SMP", Address = "play.example.net", Category = "Casual/Beginner", Notes = "Relaxed survival" },
        new ServerEntry { Name = "Beginner Skyblock", Address = "skyblock.example.org", Category = "Casual/Beginner", Notes = "Newer / chill community" },
        new ServerEntry { Name = "Local Test Server", Address = "127.0.0.1:25565", Category = "Practice", Notes = "My own server" },
    };

    private sealed class Root
    {
        public int Version { get; set; } = 1;
        public List<ServerEntry> Servers { get; set; } = new();
    }
}
