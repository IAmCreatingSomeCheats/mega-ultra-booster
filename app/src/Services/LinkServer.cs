using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Text.Json.Nodes;
using MegaUltraBooster.Models;

namespace MegaUltraBooster.Services;

/// <summary>
/// Loopback TCP server (the app side of the live link). Speaks newline-delimited
/// JSON with the mod's AppLinkClient. See shared/LINK_PROTOCOL.md.
/// Events are raised on background threads — the UI marshals them via Dispatcher.
/// </summary>
public sealed class LinkServer : IDisposable
{
    public const int Port = 38910;

    private TcpListener? _listener;
    private CancellationTokenSource? _cts;
    private readonly List<TcpClient> _clients = new();
    private readonly object _clientsLock = new();

    public bool ModConnected
    {
        get { lock (_clientsLock) return _clients.Count > 0; }
    }

    public event Action<string, string>? ModHello;     // (mcVersion, modVersion)
    public event Action<Telemetry>? TelemetryReceived;
    public event Action<string>? EasyServerRequested;  // category
    public event Action<string>? Log;

    /// <summary>Supplied by the UI: pick the quietest server for a category.</summary>
    public Func<string, Task<ServerEntry?>>? QuietestPicker;

    public void Start()
    {
        if (_listener != null) return;
        _cts = new CancellationTokenSource();
        _listener = new TcpListener(IPAddress.Loopback, Port);
        _listener.Start();
        Log?.Invoke($"Live link listening on 127.0.0.1:{Port}");
        _ = AcceptLoop(_cts.Token);
    }

    private async Task AcceptLoop(CancellationToken ct)
    {
        while (!ct.IsCancellationRequested)
        {
            TcpClient client;
            try { client = await _listener!.AcceptTcpClientAsync(ct); }
            catch { break; }
            lock (_clientsLock) _clients.Add(client);
            _ = HandleClient(client, ct);
        }
    }

    private async Task HandleClient(TcpClient client, CancellationToken ct)
    {
        try
        {
            client.NoDelay = true;
            using var stream = client.GetStream();
            using var reader = new StreamReader(stream, Encoding.UTF8);
            string? line;
            while (!ct.IsCancellationRequested && (line = await reader.ReadLineAsync(ct)) != null)
            {
                HandleLine(line);
            }
        }
        catch { /* dropped */ }
        finally
        {
            lock (_clientsLock) _clients.Remove(client);
            client.Dispose();
            Log?.Invoke("Mod disconnected");
        }
    }

    private void HandleLine(string line)
    {
        if (string.IsNullOrWhiteSpace(line)) return;
        try
        {
            var root = JsonNode.Parse(line)?.AsObject();
            string type = root?["type"]?.GetValue<string>() ?? "";
            var data = root?["data"] as JsonObject ?? new JsonObject();

            switch (type)
            {
                case "hello":
                    ModHello?.Invoke(Str(data, "mcVersion"), Str(data, "modVersion"));
                    Log?.Invoke($"Mod connected — MC {Str(data, "mcVersion")}, TurboBoost {Str(data, "modVersion")}");
                    break;

                case "telemetry":
                    TelemetryReceived?.Invoke(new Telemetry
                    {
                        Fps = Int(data, "fps"),
                        FrameTimeMs = Dbl(data, "frameTimeMs"),
                        MemUsedMb = Lng(data, "memUsedMb"),
                        MemMaxMb = Lng(data, "memMaxMb"),
                        Dimension = Str(data, "dimension"),
                        Server = Str(data, "server"),
                    });
                    break;

                case "request_easy_server":
                    string cat = Str(data, "category");
                    EasyServerRequested?.Invoke(cat);
                    _ = AnswerEasyServer(cat);
                    break;
            }
        }
        catch (Exception e)
        {
            Log?.Invoke("Bad link message: " + e.Message);
        }
    }

    private async Task AnswerEasyServer(string category)
    {
        if (QuietestPicker == null) return;
        var s = await QuietestPicker(category);
        if (s != null)
        {
            SwitchServer(s.Name, s.Address);
            Log?.Invoke($"→ Quietest '{category}': {s.Name} ({(s.Online < 0 ? "?" : s.Online.ToString())} online) — switching mod.");
        }
        else
        {
            Log?.Invoke($"No servers tagged '{category}' to route to.");
        }
    }

    // ── outgoing commands ──
    public void ApplyProfile(BoostProfile p) => Broadcast("apply_profile", p.ToJson());

    public void SwitchServer(string name, string address) =>
        Broadcast("switch_server", new JsonObject { ["name"] = name, ["address"] = address });

    public void SetHud(bool enabled) =>
        Broadcast("set_hud", new JsonObject { ["enabled"] = enabled });

    private void Broadcast(string type, JsonObject data)
    {
        var env = new JsonObject { ["type"] = type, ["data"] = data };
        byte[] bytes = Encoding.UTF8.GetBytes(env.ToJsonString() + "\n");
        List<TcpClient> snapshot;
        lock (_clientsLock) snapshot = _clients.ToList();
        foreach (var c in snapshot)
        {
            try
            {
                var s = c.GetStream();
                s.Write(bytes, 0, bytes.Length);
                s.Flush();
            }
            catch { /* client gone */ }
        }
    }

    // JSON numbers may arrive as doubles; read defensively.
    private static string Str(JsonObject o, string k) => o[k]?.GetValue<string>() ?? "";
    private static int Int(JsonObject o, string k) => o[k] is JsonNode n ? (int)n.GetValue<double>() : 0;
    private static long Lng(JsonObject o, string k) => o[k] is JsonNode n ? (long)n.GetValue<double>() : 0;
    private static double Dbl(JsonObject o, string k) => o[k] is JsonNode n ? n.GetValue<double>() : 0;

    public void Dispose()
    {
        try { _cts?.Cancel(); _listener?.Stop(); } catch { }
        lock (_clientsLock)
        {
            foreach (var c in _clients) c.Dispose();
            _clients.Clear();
        }
    }
}
