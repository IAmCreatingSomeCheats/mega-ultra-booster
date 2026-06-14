/*
 * MEGA Ultra Booster
 * Copyright (c) 2026 IAmCreatingSomeCheats. All Rights Reserved.
 * Proprietary software — unauthorized use, copying, modification, or
 * distribution of this source code is prohibited. See LICENSE for terms.
 */

package com.megaultra.turboboost.perf;

import java.util.Arrays;

/**
 * Rolling FPS window for the overlay: tracks recent samples and reports the
 * average and the <b>1% low</b> (the fps at the 1st percentile — i.e. how bad the
 * worst moments are, the metric that actually reflects perceived smoothness).
 */
public final class FpsStats {

    private static final int WINDOW = 600; // ~30 s at 20 samples/s
    private final int[] samples = new int[WINDOW];
    private int count = 0;
    private int idx = 0;

    public void record(int fps) {
        if (fps <= 0) return;
        samples[idx] = fps;
        idx = (idx + 1) % WINDOW;
        if (count < WINDOW) count++;
    }

    public int avg() {
        if (count == 0) return 0;
        long sum = 0;
        for (int i = 0; i < count; i++) sum += samples[i];
        return (int) (sum / count);
    }

    /** The 1st-percentile fps (the "1% low" you see in benchmarks). */
    public int onePercentLow() {
        if (count == 0) return 0;
        int[] sorted = Arrays.copyOf(samples, count);
        Arrays.sort(sorted);
        int i = (int) (count * 0.01); // index into the low end
        return sorted[Math.min(i, count - 1)];
    }

    public boolean hasData() {
        return count > 0;
    }

    public void reset() {
        count = 0;
        idx = 0;
    }
}
