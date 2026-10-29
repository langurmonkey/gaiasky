/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds callbacks that have to run once a material exists, for cases where the material is built
 * later than the component that wants to contribute to it.
 * <p>
 * Gaia Sky's lazy texture initialization does exactly that: {@link MaterialComponent#initMaterial}
 * runs on the first frame the entity is drawn, not at set-up time, so anything that adds attributes
 * to the material has to wait. Callers register here, {@link MaterialComponent} marks the queue
 * ready at the end of {@code initMaterial}, and everything registered runs at that point - on GL
 * thread, before the material is first used to render.
 * <p>
 * Kept separate from {@link MaterialComponent} so it can be tested without a graphics context.
 */
public class MaterialInitQueue {
    private final List<Runnable> listeners = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean ready;

    /** @return Whether {@link #markReady()} has already been called. **/
    public boolean isReady() {
        return ready;
    }

    /**
     * Runs the callback as soon as the material is ready, or immediately if it already is.
     *
     * @param listener The callback. Ignored if null.
     */
    public void whenReady(Runnable listener) {
        if (listener == null) {
            return;
        }
        if (ready) {
            listener.run();
            return;
        }
        listeners.add(listener);
        // Re-check under the list's lock: markReady() may have drained the list between the read
        // above and the add, and the callback would then never run.
        synchronized (listeners) {
            if (ready && listeners.remove(listener)) {
                listener.run();
            }
        }
    }

    /**
     * Marks the material as ready, running and clearing every pending callback. Callbacks run outside
     * the lock, so they can register more work without deadlocking.
     */
    public void markReady() {
        ready = true;
        List<Runnable> pending;
        synchronized (listeners) {
            pending = new ArrayList<>(listeners);
            listeners.clear();
        }
        for (var listener : pending) {
            listener.run();
        }
    }

    /** @return The number of callbacks waiting. **/
    public int pending() {
        return listeners.size();
    }
}
