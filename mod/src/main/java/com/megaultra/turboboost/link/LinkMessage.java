/*
 * MEGA Ultra Booster
 * Copyright (c) 2026 IAmCreatingSomeCheats. All Rights Reserved.
 * Proprietary software — unauthorized use, copying, modification, or
 * distribution of this source code is prohibited. See LICENSE for terms.
 */

package com.megaultra.turboboost.link;

import com.google.gson.JsonObject;

/**
 * Tiny helper for the {@code { "type": ..., "data": {...} }} envelope used on the
 * newline-delimited JSON link. See shared/LINK_PROTOCOL.md.
 */
public final class LinkMessage {
    public final String type;
    public final JsonObject data;

    public LinkMessage(String type, JsonObject data) {
        this.type = type;
        this.data = data;
    }

    /** Build an outgoing envelope object ready to be serialized to one line. */
    public static JsonObject envelope(String type, JsonObject data) {
        JsonObject o = new JsonObject();
        o.addProperty("type", type);
        o.add("data", data != null ? data : new JsonObject());
        return o;
    }
}
