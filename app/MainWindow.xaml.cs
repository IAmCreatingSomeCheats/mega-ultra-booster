/*
 * MEGA Ultra Booster
 * Copyright (c) 2026 IAmCreatingSomeCheats. All Rights Reserved.
 * Proprietary software — unauthorized use, copying, modification, or
 * distribution of this source code is prohibited. See LICENSE for terms.
 */

using System.Collections.ObjectModel;
using System.Windows;
using System.Windows.Media;
using System.Windows.Shapes;
using MegaUltraBooster.Models;
using MegaUltraBooster.Services;

namespace MegaUltraBooster;

public partial class MainWindow : Window
{
    private readonly SystemOptimizer _system = new();
    private readonly JvmTuner _jvm = new();
    private readonly MinecraftLauncher _launcher = new();
    private readonly ServerStore _store = new();
    private readonly LinkServer _link = new();

    private readonly ObservableCollection<ServerEntry> _servers = new();
    private long _totalRamMb;

    // Live FPS history for the sparkline + session stats.
    private const int FpsHistoryMax = 60;
    private readonly List<int> _fpsHistory = new();
    private int _fpsMin, _fpsMax, _fpsCount;
    private long _fpsSum;

    public MainWindow()
    {
        InitializeComponent();
        Loaded += OnLoaded;
        Closed += OnClosed;
    }

    private void OnLoaded(object? sender, RoutedEventArgs e)
    {
        // Memory + RAM slider
        var (total, avail) = _system.GetMemory();
        _totalRamMb = total;
        MemText.Text = total > 0
            ? $"Installed RAM: {total / 1024.0:0.0} GB    ·    Free now: {avail / 1024.0:0.0} GB"
            : "Could not read system memory.";

        int recommended = _jvm.RecommendHeapMb(total);
        RamSlider.Minimum = 2048;
        RamSlider.Maximum = Math.Max(2048, Math.Min(total - 1024, 16384));
        RamSlider.Value = Math.Clamp(recommended, (int)RamSlider.Minimum, (int)RamSlider.Maximum);
        RamSlider.ValueChanged += (_, _) => UpdateRamUi();
        UpdateRamUi();

        // Boost intensity selector (matches the mod's profiles)
        IntensityCombo.ItemsSource = new[] { "Quality (light)", "Balanced", "Potato (max FPS)" };
        IntensityCombo.SelectedIndex = 2; // Potato — same as the old one-shot BOOST

        // Servers
        _store.Load();
        foreach (var s in _store.Servers) _servers.Add(s);
        ServersGrid.ItemsSource = _servers;
        RefreshCategories();
        LauncherStatus.Text = _launcher.MinecraftInstalled
            ? $".minecraft found: {_launcher.MinecraftDir}"
            : "⚠ .minecraft not found — run the official launcher once first.";

        // Live link
        _link.Log += msg => Dispatch(() => Log(msg));
        _link.ModHello += (mc, mod) => Dispatch(() => SetLinked(true, mc));
        _link.TelemetryReceived += t => Dispatch(() => UpdateTelemetry(t));
        _link.QuietestPicker = cat => _store.PickQuietestAsync(cat);
        try { _link.Start(); }
        catch (Exception ex) { Log("✖ Live link failed to start: " + ex.Message); }

        Log("Ready. Launch Minecraft with the TurboBoost mod — it connects automatically.");
    }

    private void OnClosed(object? sender, EventArgs e)
    {
        _store.Servers.Clear();
        _store.Servers.AddRange(_servers);
        _store.Save();
        _link.Dispose();
    }

    // ── helpers ──
    private void Dispatch(Action a) => Dispatcher.Invoke(a);

    private void Log(string msg)
    {
        LogBox.AppendText($"[{DateTime.Now:HH:mm:ss}] {msg}\n");
        LogBox.ScrollToEnd();
    }

    private int HeapMb => (int)RamSlider.Value;

    private void UpdateRamUi()
    {
        RamLabel.Text = $"{HeapMb / 1024.0:0.0} GB";
        FlagsText.Text = _jvm.BuildAikarFlags(HeapMb);
    }

    private void RefreshCategories()
    {
        var cats = _servers.Select(s => s.Category)
            .Where(c => !string.IsNullOrWhiteSpace(c))
            .Distinct(StringComparer.OrdinalIgnoreCase)
            .ToList();
        if (!cats.Contains("Casual/Beginner", StringComparer.OrdinalIgnoreCase))
            cats.Insert(0, "Casual/Beginner");

        string? prev = CategoryCombo.SelectedItem as string;
        CategoryCombo.ItemsSource = cats;
        CategoryCombo.SelectedItem = prev != null && cats.Contains(prev)
            ? prev
            : cats.FirstOrDefault(c => c.Equals("Casual/Beginner", StringComparison.OrdinalIgnoreCase)) ?? cats.FirstOrDefault();
    }

    private void SetLinked(bool linked, string mc = "")
    {
        LinkDot.Fill = (Brush)FindResource(linked ? "Good" : "Subtle");
        LinkText.Text = linked ? (mc.Length > 0 ? $"Mod linked · MC {mc}" : "Mod linked") : "Waiting for mod…";
    }

    private void UpdateTelemetry(Telemetry t)
    {
        FpsText.Text = t.Fps.ToString();
        FpsText.Foreground = (Brush)FindResource(t.Fps >= 90 ? "Good" : t.Fps >= 45 ? "Warn" : "Danger");
        FrameTimeText.Text = $"Frame time: {t.FrameTimeMs:0.0} ms";
        RamUsageText.Text = $"Heap: {t.MemUsedMb} / {t.MemMaxMb} MB";
        RamBar.Value = t.MemMaxMb > 0 ? Math.Clamp(t.MemUsedMb * 100.0 / t.MemMaxMb, 0, 100) : 0;
        ServerText.Text = $"Server: {t.Server}";
        DimText.Text = $"Dimension: {t.Dimension}";
        RecordFps(t.Fps);
        DrawFpsGraph();
        SetLinked(true);
    }

    private void RecordFps(int fps)
    {
        if (fps <= 0) return;
        _fpsHistory.Add(fps);
        if (_fpsHistory.Count > FpsHistoryMax) _fpsHistory.RemoveAt(0);

        _fpsSum += fps;
        _fpsCount++;
        _fpsMin = _fpsCount == 1 ? fps : Math.Min(_fpsMin, fps);
        _fpsMax = Math.Max(_fpsMax, fps);
        FpsMinAvgMax.Text = $"min {_fpsMin}  ·  avg {(int)(_fpsSum / _fpsCount)}  ·  max {_fpsMax}";
    }

    private void DrawFpsGraph()
    {
        FpsGraph.Children.Clear();
        if (_fpsHistory.Count < 2) return;

        double w = FpsGraph.ActualWidth, h = FpsGraph.ActualHeight;
        if (w <= 0 || h <= 0) return; // not laid out yet — next sample will draw

        double scaleMax = Math.Max(_fpsMax, 1);
        double dx = w / (FpsHistoryMax - 1);
        int start = FpsHistoryMax - _fpsHistory.Count; // right-align the newest sample

        var points = new PointCollection();
        for (int i = 0; i < _fpsHistory.Count; i++)
        {
            double x = (start + i) * dx;
            double y = h - (_fpsHistory[i] / scaleMax) * (h - 2) - 1;
            points.Add(new Point(x, y));
        }

        FpsGraph.Children.Add(new Polyline
        {
            Points = points,
            Stroke = (Brush)FindResource("Accent"),
            StrokeThickness = 2,
            StrokeLineJoin = PenLineJoin.Round,
        });
    }

    // ── button handlers ──
    private void BoostButton_Click(object sender, RoutedEventArgs e)
    {
        Log("──── Boosting ────");
        if (PowerToggle.IsChecked == true) Log(_system.SetHighPerformancePowerPlan());
        if (PriorityToggle.IsChecked == true) Log(_system.PrioritizeMinecraft());
        if (TrimToggle.IsChecked == true) Log(_system.FreeStandbyMemory());
        if (GameModeToggle.IsChecked == true) Log(_system.SetGameMode(true));

        var profile = IntensityCombo.SelectedIndex switch
        {
            0 => BoostProfile.Quality(),
            1 => BoostProfile.Balanced(),
            _ => BoostProfile.Potato(),
        };
        profile.DynamicFps = DynamicFpsToggle.IsChecked == true;
        string intensity = (IntensityCombo.SelectedItem as string) ?? "Potato";
        if (_link.ModConnected)
        {
            _link.ApplyProfile(profile);
            Log($"✔ Sent '{intensity}' boost profile to the running game.");
        }
        else
        {
            Log("• Game not linked yet — system boosted. The in-game profile applies once the mod connects.");
        }
    }

    private void ApplyProfileButton_Click(object sender, RoutedEventArgs e)
    {
        Log(_launcher.PatchOptimizedProfile(HeapMb, _jvm.BuildAikarFlags(HeapMb)));
    }

    private void LaunchButton_Click(object sender, RoutedEventArgs e) => Log(_launcher.OpenLauncher());

    private async void PingAllButton_Click(object sender, RoutedEventArgs e)
    {
        Log("Pinging all servers…");
        PingAllButton.IsEnabled = false;
        try { await Task.WhenAll(_servers.Select(_store.PingOneAsync)); }
        finally { PingAllButton.IsEnabled = true; }
        Log("✔ Ping complete.");
    }

    private async void QuietestButton_Click(object sender, RoutedEventArgs e)
    {
        if (CategoryCombo.SelectedItem is not string cat || string.IsNullOrWhiteSpace(cat))
        {
            Log("Pick a category first.");
            return;
        }
        Log($"Finding the quietest server in '{cat}'…");
        QuietestButton.IsEnabled = false;
        try
        {
            var best = await _store.PickQuietestAsync(cat);
            if (best == null) { Log($"No servers tagged '{cat}'."); return; }

            string online = best.Online < 0 ? "?" : best.Online.ToString();
            string ping = best.Ping < 0 ? "?" : best.Ping + " ms";
            Log($"🎯 Quietest: {best.Name} — {online} online, {ping}.");

            if (_link.ModConnected)
            {
                _link.SwitchServer(best.Name, best.Address);
                Log("✔ Told the game to switch.");
            }
            else
            {
                Log("• Game not linked — join a server then press the in-game quick-switch key (K).");
            }
        }
        finally { QuietestButton.IsEnabled = true; }
    }

    private void AddButton_Click(object sender, RoutedEventArgs e)
    {
        string name = AddName.Text.Trim();
        string addr = AddAddress.Text.Trim();
        string cat = AddCategory.Text.Trim();
        if (name.Length == 0 || addr.Length == 0)
        {
            Log("Enter a name and an address to add a server.");
            return;
        }
        _servers.Add(new ServerEntry
        {
            Name = name,
            Address = addr,
            Category = cat.Length > 0 ? cat : "Uncategorized",
        });
        AddName.Text = "";
        AddAddress.Text = "";
        RefreshCategories();
        Log($"Added '{name}'.");
    }

    private void RemoveButton_Click(object sender, RoutedEventArgs e)
    {
        if (ServersGrid.SelectedItem is ServerEntry s)
        {
            _servers.Remove(s);
            RefreshCategories();
            Log($"Removed '{s.Name}'.");
        }
        else
        {
            Log("Select a server row to remove.");
        }
    }

    private void PauseButton_Click(object sender, RoutedEventArgs e)
        => Log(_system.SuspendByNames(SplitNames(PauseNamesBox.Text)));

    private void ResumeButton_Click(object sender, RoutedEventArgs e)
        => Log(_system.ResumeByNames(SplitNames(PauseNamesBox.Text)));

    private static string[] SplitNames(string s)
        => s.Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries);
}
