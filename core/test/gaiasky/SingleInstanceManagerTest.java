/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import gaiasky.util.SingleInstanceManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SingleInstanceManagerTest {
    private static final long TIMEOUT_SECONDS = 10;

    private SingleInstanceManager manager;

    @Before
    public void setUp() {
        manager = new SingleInstanceManager();
    }

    @After
    public void tearDown() {
        manager.stopServer();
    }

    @Test
    public void testForwardToRunningInstance() throws InterruptedException {
        manager.startServer();

        List<String> received = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        manager.addListener(url -> {
            synchronized (received) {
                received.add(url);
            }
            latch.countDown();
        });

        final String url = "gaiasky://load?dataset=test-key";
        Assert.assertTrue("URL should be forwarded to the running instance",
                          SingleInstanceManager.forwardToRunningInstance(url));

        Assert.assertTrue("URL should be received by the listener within " + TIMEOUT_SECONDS + " s",
                          latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        synchronized (received) {
            Assert.assertEquals(1, received.size());
            Assert.assertEquals(url, received.getFirst());
        }
    }

    @Test
    public void testForwardWithNoRunningInstance() {
        // No server started — forwarding must fail gracefully.
        Assert.assertFalse(SingleInstanceManager.forwardToRunningInstance("gaiasky://load?dataset=key"));
    }

    @Test
    public void testPendingUrlDeliveredOnListenerRegistration() throws InterruptedException {
        manager.startServer();

        // Forward before any listener is registered.
        final String url = "gaiasky://load?dataset=pending-key";
        Assert.assertTrue(SingleInstanceManager.forwardToRunningInstance(url));

        // Give the server time to receive and queue the URL.
        Thread.sleep(500);

        // Register the listener — the pending URL must be delivered immediately.
        List<String> received = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        manager.addListener(url1 -> {
            synchronized (received) {
                received.add(url1);
            }
            latch.countDown();
        });

        Assert.assertTrue("Pending URL should be delivered on listener registration",
                          latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        synchronized (received) {
            Assert.assertEquals(1, received.size());
            Assert.assertEquals(url, received.getFirst());
        }
    }

    @Test
    public void testForwardInvalidUrlIsRejected() throws InterruptedException {
        manager.startServer();

        List<String> received = new ArrayList<>();
        manager.addListener(received::add);

        // A URL that is not a gaiasky:// URL must be rejected by the server.
        Assert.assertFalse(SingleInstanceManager.forwardToRunningInstance("http://example.com"));

        Thread.sleep(500);
        Assert.assertTrue("Invalid URL must not be dispatched to listeners", received.isEmpty());
    }

    @Test
    public void testStartServerIsIdempotent() {
        manager.startServer();
        // Starting again must not throw nor reset the state.
        manager.startServer();
    }

    @Test
    public void testStopServer() throws InterruptedException {
        manager.startServer();
        manager.stopServer();

        // After stopping, forwarding must fail (no instance listening).
        Thread.sleep(200);
        Assert.assertFalse(SingleInstanceManager.forwardToRunningInstance("gaiasky://load?dataset=key"));

        // Stopping again must be safe.
        manager.stopServer();
    }
}
