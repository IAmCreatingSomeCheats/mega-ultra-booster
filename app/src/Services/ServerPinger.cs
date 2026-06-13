/*
 * MEGA Ultra Booster
 * Copyright (c) 2026 IAmCreatingSomeCheats. All Rights Reserved.
 * Proprietary software — unauthorized use, copying, modification, or
 * distribution of this source code is prohibited. See LICENSE for terms.
 */

using System.Net.Sockets;
using System.Diagnostics;
using System.IO;
using System.Text;
using System.Text.Json;

namespace MegaUltraBooster.Services;

public sealed record PingResult(bool Ok, int Online, int Max, string Motd, string Version, int LatencyMs, string? Error = null);

/// <summary>
/// Implements the Minecraft "Server List Ping" (status) protocol over TCP so the
/// booster can read a server's live player count + latency without joining.
/// Note: no SRV-record resolution — use host:port directly for SRV-only servers.
/// </summary>
public sealed class ServerPinger
{
    public Task<PingResult> PingAsync(string address, int timeoutMs = 2500)
        => Task.Run(() => PingSync(address, timeoutMs));

    private static PingResult PingSync(string address, int timeoutMs)
    {
        var (host, port) = ParseAddress(address);
        try
        {
            using var client = new TcpClient();
            var ar = client.BeginConnect(host, port, null, null);
            if (!ar.AsyncWaitHandle.WaitOne(timeoutMs))
                return new PingResult(false, -1, -1, "", "", -1, "timeout");
            client.EndConnect(ar);
            client.ReceiveTimeout = timeoutMs;
            client.SendTimeout = timeoutMs;
            using var stream = client.GetStream();

            // Handshake (next state = 1 / status)
            var hs = new List<byte>();
            WriteVarInt(hs, 0x00);
            WriteVarInt(hs, -1);                       // protocol version (-1 = unspecified)
            WriteString(hs, host);
            hs.Add((byte)(port >> 8));
            hs.Add((byte)(port & 0xFF));               // unsigned short, big-endian
            WriteVarInt(hs, 1);
            SendPacket(stream, hs);

            // Status request
            var sr = new List<byte>();
            WriteVarInt(sr, 0x00);
            SendPacket(stream, sr);

            // Status response
            ReadVarInt(stream);                        // overall length (ignored)
            ReadVarInt(stream);                        // packet id (0x00)
            int jsonLen = ReadVarInt(stream);
            string text = Encoding.UTF8.GetString(ReadExact(stream, jsonLen));

            // Optional latency probe
            int latency = -1;
            try
            {
                var sw = Stopwatch.StartNew();
                var ping = new List<byte> { };
                WriteVarInt(ping, 0x01);
                long token = DateTime.UtcNow.Ticks;
                for (int i = 7; i >= 0; i--) ping.Add((byte)(token >> (i * 8)));
                SendPacket(stream, ping);
                ReadVarInt(stream);                    // length
                ReadVarInt(stream);                    // packet id (0x01)
                ReadExact(stream, 8);                  // echoed payload
                sw.Stop();
                latency = (int)sw.ElapsedMilliseconds;
            }
            catch { /* latency is best-effort */ }

            return ParseStatus(text, latency);
        }
        catch (Exception e)
        {
            return new PingResult(false, -1, -1, "", "", -1, e.Message);
        }
    }

    private static PingResult ParseStatus(string json, int latency)
    {
        try
        {
            using var doc = JsonDocument.Parse(json);
            var root = doc.RootElement;
            int online = -1, max = -1;
            if (root.TryGetProperty("players", out var players))
            {
                if (players.TryGetProperty("online", out var on)) online = on.GetInt32();
                if (players.TryGetProperty("max", out var mx)) max = mx.GetInt32();
            }
            string version = "";
            if (root.TryGetProperty("version", out var ver) &&
                ver.TryGetProperty("name", out var vn))
                version = vn.GetString() ?? "";
            string motd = ExtractMotd(root);
            return new PingResult(true, online, max, motd, version, latency);
        }
        catch (Exception e)
        {
            return new PingResult(false, -1, -1, "", "", latency, "parse: " + e.Message);
        }
    }

    // ── MOTD flattening (description may be a string or chat-component tree) ──
    private static string ExtractMotd(JsonElement root)
    {
        if (!root.TryGetProperty("description", out var d)) return "";
        return StripFormatting(FlattenComponent(d)).Trim();
    }

    private static string FlattenComponent(JsonElement e)
    {
        switch (e.ValueKind)
        {
            case JsonValueKind.String:
                return e.GetString() ?? "";
            case JsonValueKind.Object:
                var sb = new StringBuilder();
                if (e.TryGetProperty("text", out var t) && t.ValueKind == JsonValueKind.String)
                    sb.Append(t.GetString());
                if (e.TryGetProperty("extra", out var ex) && ex.ValueKind == JsonValueKind.Array)
                    foreach (var c in ex.EnumerateArray()) sb.Append(FlattenComponent(c));
                return sb.ToString();
            case JsonValueKind.Array:
                var sb2 = new StringBuilder();
                foreach (var c in e.EnumerateArray()) sb2.Append(FlattenComponent(c));
                return sb2.ToString();
            default:
                return "";
        }
    }

    private static string StripFormatting(string s)
    {
        var sb = new StringBuilder(s.Length);
        for (int i = 0; i < s.Length; i++)
        {
            if (s[i] == '§' && i + 1 < s.Length) { i++; continue; }
            sb.Append(s[i] == '\n' ? ' ' : s[i]);
        }
        return sb.ToString();
    }

    // ── VarInt / packet framing ──
    private static void WriteVarInt(List<byte> buf, int value)
    {
        uint v = (uint)value;
        do
        {
            byte temp = (byte)(v & 0x7F);
            v >>= 7;
            if (v != 0) temp |= 0x80;
            buf.Add(temp);
        } while (v != 0);
    }

    private static void WriteString(List<byte> buf, string s)
    {
        var bytes = Encoding.UTF8.GetBytes(s);
        WriteVarInt(buf, bytes.Length);
        buf.AddRange(bytes);
    }

    private static void SendPacket(NetworkStream stream, List<byte> data)
    {
        var framed = new List<byte>();
        WriteVarInt(framed, data.Count);
        framed.AddRange(data);
        stream.Write(framed.ToArray(), 0, framed.Count);
        stream.Flush();
    }

    private static int ReadVarInt(NetworkStream stream)
    {
        int numRead = 0, result = 0;
        byte read;
        do
        {
            int b = stream.ReadByte();
            if (b == -1) throw new EndOfStreamException();
            read = (byte)b;
            result |= (read & 0x7F) << (7 * numRead);
            numRead++;
            if (numRead > 5) throw new InvalidDataException("VarInt too big");
        } while ((read & 0x80) != 0);
        return result;
    }

    private static byte[] ReadExact(NetworkStream stream, int count)
    {
        var buf = new byte[count];
        int off = 0;
        while (off < count)
        {
            int r = stream.Read(buf, off, count - off);
            if (r <= 0) throw new EndOfStreamException();
            off += r;
        }
        return buf;
    }

    private static (string host, int port) ParseAddress(string address)
    {
        address = address.Trim();
        int idx = address.LastIndexOf(':');
        if (idx > 0 && int.TryParse(address[(idx + 1)..], out int p))
            return (address[..idx], p);
        return (address, 25565);
    }
}
