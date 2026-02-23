/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script;

import gaiasky.util.Logger;
import gaiasky.util.Settings;
import py4j.ClientServer;
import py4j.DefaultGatewayServerListener;
import py4j.Py4JServerConnection;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scripting server manager.
 */
public class ScriptingServer {
    private static final Logger.Log logger = Logger.getLogger(ScriptingServer.class);
    private static final AtomicInteger connections = new AtomicInteger(0);
    private static ClientServer gatewayServer;
    // We need to store this to re-initialize automatically
    private static IScriptingInterface cachedInterface;


    public static void initialize(IScriptingInterface scriptingInterface) {
        initialize(scriptingInterface, false);
    }

    /**
     * Initializes the gateway server and the default listener. This method does not start the server, only initializes it.
     *
     * @param scriptingInterface The scripting interface.
     * @param force              Force the initialization of the server.
     */
    public static void initialize(IScriptingInterface scriptingInterface, boolean force) {
        cachedInterface = scriptingInterface;

        if (Settings.settings.program.net.slave.active) return;

        if (force || gatewayServer != null) {
            dispose();
        }

        if (gatewayServer == null) {
            try {
                // ClientServer starts its own internal server automatically
                gatewayServer = new ClientServer(scriptingInterface);
                gatewayServer.getJavaServer().addListener(new DefaultGatewayServerListener() {

                    @Override
                    public void connectionStopped(Py4JServerConnection gatewayConnection) {
                        logger.info("Connection stopped. Remaining: " + connections.decrementAndGet());

                        // If no more scripts are connected, we "refresh" the server
                        // to ensure it's ready for a fresh handshake.
                        if (connections.get() <= 0) {
                            logger.info("Last connection closed. Resetting server for next script...");
                            restartAsync();
                        }
                    }

                    @Override
                    public void connectionStarted(Py4JServerConnection gatewayConnection) {
                        connections.incrementAndGet();
                        logger.info("Connection started: " + gatewayConnection.getSocket().toString());
                    }
                });
                logger.info("Py4J Server initialized and listening.");
            } catch (Exception e) {
                logger.error("Initialization failed: " + e.getMessage());
            }
        }
    }

    /**
     * Restart the server in a different thread.
     */
    private static void restartAsync() {
        new Thread(() -> {
            try {
                // Wait for socket to clear.
                Thread.sleep(500);
                // Initialize.
                initialize(cachedInterface, true);
            } catch (InterruptedException ignored) {}
        }).start();
    }

    /**
     * Disposes the current gateway server.
     */
    public static void dispose() {
        if (gatewayServer != null) {
            try {
                gatewayServer.shutdown();
            } catch (Exception e) {
                logger.error("Error during shutdown", e);
            }
            gatewayServer = null;
        }
    }

}
