using System.ComponentModel;
using System.Runtime.CompilerServices;
using System.Text.Json.Serialization;

namespace MegaUltraBooster.Models;

/// <summary>
/// A saved server. The first four properties are persisted to servers.json; the
/// rest are live ping results bound to the UI (and ignored by the serializer).
/// </summary>
public sealed class ServerEntry : INotifyPropertyChanged
{
    public string Name { get; set; } = "";
    public string Address { get; set; } = "";
    public string Category { get; set; } = "";
    public string Notes { get; set; } = "";

    private int _online = -1;
    private int _max = -1;
    private int _ping = -1;
    private string _status = "—";

    [JsonIgnore]
    public int Online { get => _online; set { _online = value; OnChanged(); OnChanged(nameof(Players)); } }

    [JsonIgnore]
    public int Max { get => _max; set { _max = value; OnChanged(); OnChanged(nameof(Players)); } }

    [JsonIgnore]
    public int Ping { get => _ping; set { _ping = value; OnChanged(); OnChanged(nameof(PingText)); } }

    [JsonIgnore]
    public string Status { get => _status; set { _status = value; OnChanged(); } }

    /// <summary>"online / max" for the grid, or "—" if not yet pinged.</summary>
    [JsonIgnore]
    public string Players => _online < 0 ? "—" : $"{_online} / {_max}";

    [JsonIgnore]
    public string PingText => _ping < 0 ? "—" : $"{_ping} ms";

    public event PropertyChangedEventHandler? PropertyChanged;

    private void OnChanged([CallerMemberName] string? name = null) =>
        PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(name));
}
