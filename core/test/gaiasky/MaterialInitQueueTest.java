/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import gaiasky.scene.record.MaterialInitQueue;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Deferred attribute contributions to lazily initialised materials. With lazy texture initialisation
 * on, a volume's material does not exist at set-up time, so generated volumes register in a
 * {@link MaterialInitQueue} instead of bailing out.
 */
public class MaterialInitQueueTest {

    @Test
    public void callbackWaitsForTheMaterialAndRunsOnce() {
        var queue = new MaterialInitQueue();
        var runs = new AtomicInteger();
        queue.whenReady(runs::incrementAndGet);

        Assert.assertFalse(queue.isReady());
        Assert.assertEquals("must not run before the material exists", 0, runs.get());
        Assert.assertEquals(1, queue.pending());

        queue.markReady();
        Assert.assertEquals("must run once the material exists", 1, runs.get());
        Assert.assertEquals(0, queue.pending());

        // A second material initialisation must not re-run it: generation would bind the same
        // textures again, with stale bounds.
        queue.markReady();
        Assert.assertEquals(1, runs.get());
    }

    @Test
    public void callbackRunsImmediatelyWhenTheMaterialIsAlreadyThere() {
        var queue = new MaterialInitQueue();
        queue.markReady();

        var runs = new AtomicInteger();
        queue.whenReady(runs::incrementAndGet);
        Assert.assertEquals(1, runs.get());
        Assert.assertEquals(0, queue.pending());
    }

    @Test
    public void nullCallbackIsIgnored() {
        var queue = new MaterialInitQueue();
        queue.whenReady(null);
        Assert.assertEquals(0, queue.pending());
        queue.markReady();
    }

    @Test
    public void aCallbackRegisteredFromAnotherCallbackRuns() {
        // Generation may itself defer more work; markReady() must not deadlock on it.
        var queue = new MaterialInitQueue();
        var order = new StringBuilder();
        queue.whenReady(() -> {
            order.append("a");
            queue.whenReady(() -> order.append("b"));
        });
        queue.markReady();
        Assert.assertEquals("ab", order.toString());
    }
}
