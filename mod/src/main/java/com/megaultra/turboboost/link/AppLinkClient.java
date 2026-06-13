package com.megaultra.turboboost.link;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.megaultra.turboboost.TurboBoostClient;
import com.megaultra.turboboost.config.BoostConfig;
import com.megaultra.turboboost.perf.BoostProfile;
import com.megaultra.turboboost.perf.PerformanceManager;
import com.megaultra.turboboost.server.ServerSwitcher;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Connects to the desktop app's loopback TCP server and exchanges
 * newline-delimited JSON. Runs on its own daemon thread and auto-reconnects with
 * backoff, so the order you launch things in doesn't matter.
 * See shared/LINK_PROTOCOL.md.
 */
public final class AppLinkClient {
    private static final Gson GSON = new Gson();

    private final BoostConfig config;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Object writeLock = new Object();

    private volatile boolean connected = false;
    private volatile Socket socket;
    private volatile BufferedWriter writer;
    private Thread thread;

    public AppLinkClient(BoostConfig config) {
        this.config = config;
    }

    public boolean isConnected() {
        return connected;
    }

    public void start() {
        if (!config.linkEnabled) return;
        if (running.getAndSet(true)) return;
        thread = new Thread(this::runLoop, "TurboBoost-AppLink");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running.set(false);
        closeQuietly();
        if (thread != null) thread.interrupt();
    }

    private void runLoop() {
        int backoffMs = 1000;
        while (running.get()) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(config.linkHost, config.linkPort), 2000);
                s.setTcpNoDelay(true);
                this.socket = s;
                this.writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8));
                BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                connected = true;
                backoffMs = 1000;
                sendHello();
                TurboBoostClient.LOGGER.info("[TurboBoost] Linked to app at {}:{}", config.linkHost, config.linkPort);

                String line;
                while (running.get() && (line = reader.readLine()) != null) {
                    handleLine(line);
                }
            } catch (Exception ignored) {
                // app not running yet or connection dropped — retry below
            } finally {
                connected = false;
                closeQuietly();
            }
            if (!running.get()) break;
            try {
                Thread.sleep(backoffMs);
            } catch (InterruptedException ie) {
                break;
            }
            backoffMs = Math.min(backoffMs * 2, 8000);
        }
    }

    private void handleLine(String line) {
        if (line == null || line.isBlank()) return;
        try {
            JsonObject root = JsonParser.parseString(line).getAsJsonObject();
            String type = root.has("type") ? root.get("type").getAsString() : "";
            JsonObject data = root.has("data") && root.get("data").isJsonObject()
                    ? root.getAsJsonObject("data") : new JsonObject();
            MinecraftClient client = MinecraftClient.getInstance();
            switch (type) {
                case "apply_profile" -> client.execute(() -> {
                    PerformanceManager.applyProfile(BoostProfile.fromJson(data));
                    TurboBoostClient.actionBar("§a⚡ Boost applied from app");
                    send("boost_state", bool("active", true));
                });
                case "switch_server" -> {
                    String name = data.has("name") ? data.get("name").getAsString() : "Server";
                    String address = data.has("address") ? data.get("address").getAsString() : null;
                    if (address != null) ServerSwitcher.connect(name, address);
                }
                case "set_hud" -> client.execute(() -> {
                    config.hudEnabled = data.has("enabled") && data.get("enabled").getAsBoolean();
                    config.save();
                });
                case "ping" -> { /* keepalive */ }
                default -> { /* ignore unknown types (forward-compatible) */ }
            }
        } catch (Exception e) {
            TurboBoostClient.LOGGER.warn("[TurboBoost] Bad link message: {}", line);
        }
    }

    // ── outgoing ──────────────────────────────────────────────────────────
    public void send(String type, JsonObject data) {
        if (!connected) return;
        String line = GSON.toJson(LinkMessage.envelope(type, data));
        synchronized (writeLock) {
            BufferedWriter w = this.writer;
            if (w == null) return;
            try {
                w.write(line);
                w.write("\n");
                w.flush();
            } catch (Exception e) {
                connected = false;
            }
        }
    }

    private void sendHello() {
        JsonObject d = new JsonObject();
        d.addProperty("mcVersion", MinecraftClient.getInstance().getGameVersion());
        d.addProperty("modVersion", TurboBoostClient.MOD_VERSION);
        send("hello", d);
    }

    public void sendTelemetry() {
        if (!connected) return;
        MinecraftClient client = MinecraftClient.getInstance();
        int fps = client.getCurrentFps();
        Runtime rt = Runtime.getRuntime();
        long used = (rt.totalMemory() - rt.freeMemory()) / 1_048_576L;
        long max = rt.maxMemory() / 1_048_576L;

        JsonObject d = new JsonObject();
        d.addProperty("fps", fps);
        d.addProperty("frameTimeMs", fps > 0 ? 1000.0 / fps : 0.0);
        d.addProperty("memUsedMb", used);
        d.addProperty("memMaxMb", max);
        d.addProperty("dimension", client.world != null
                ? client.world.getRegistryKey().getValue().toString() : "none");
        ServerInfo si = client.getCurrentServerEntry();
        d.addProperty("server", si != null ? si.address
                : (client.isInSingleplayer() ? "singleplayer" : "-"));
        send("telemetry", d);
    }

    public void requestEasyServer(String category) {
        JsonObject d = new JsonObject();
        d.addProperty("category", category);
        send("request_easy_server", d);
    }

    private static JsonObject bool(String key, boolean val) {
        JsonObject o = new JsonObject();
        o.addProperty(key, val);
        return o;
    }

    private void closeQuietly() {
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {
        }
        socket = null;
        writer = null;
    }
}
