/*
 * MEGA Ultra Booster
 * Copyright (c) 2026 IAmCreatingSomeCheats. All Rights Reserved.
 * Proprietary software — unauthorized use, copying, modification, or
 * distribution of this source code is prohibited. See LICENSE for terms.
 */

package com.megaultra.turboboost.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.megaultra.turboboost.TurboBoostClient;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The saved-server list used by the smart switcher, persisted to
 * {@code config/turboboost-servers.json}. Shares its shape with the app's list.
 */
public final class ServerStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final List<ServerEntry> servers = new ArrayList<>();
    private int cycleIndex = 0;

    public void load() {
        Path p = path();
        servers.clear();
        try {
            if (Files.exists(p)) {
                Root root = GSON.fromJson(Files.readString(p), Root.class);
                if (root != null && root.servers != null) servers.addAll(root.servers);
            } else {
                seedDefault();
                save();
            }
        } catch (Exception e) {
            TurboBoostClient.LOGGER.warn("[TurboBoost] Could not read servers list", e);
        }
    }

    public void save() {
        try {
            Root root = new Root();
            root.servers = servers;
            Files.writeString(path(), GSON.toJson(root));
        } catch (Exception e) {
            TurboBoostClient.LOGGER.warn("[TurboBoost] Could not save servers list", e);
        }
    }

    public List<ServerEntry> all() {
        return servers;
    }

    public List<ServerEntry> inCategory(String category) {
        List<ServerEntry> out = new ArrayList<>();
        for (ServerEntry s : servers) {
            if (s.category != null && s.category.equalsIgnoreCase(category)) out.add(s);
        }
        return out;
    }

    /** Offline fallback when the app isn't running: cycle to the next server in a category. */
    public ServerEntry nextInCategory(String category) {
        List<ServerEntry> list = inCategory(category);
        if (list.isEmpty()) return null;
        ServerEntry s = list.get(cycleIndex % list.size());
        cycleIndex = (cycleIndex + 1) % list.size();
        return s;
    }

    private void seedDefault() {
        servers.add(new ServerEntry("Quiet SMP", "play.example.net", "Casual/Beginner", "Relaxed survival"));
        servers.add(new ServerEntry("Local Test Server", "127.0.0.1:25565", "Practice", "My own server"));
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("turboboost-servers.json");
    }

    private static final class Root {
        int version = 1;
        List<ServerEntry> servers;
    }
}
