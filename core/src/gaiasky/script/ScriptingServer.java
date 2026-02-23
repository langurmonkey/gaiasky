/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script;

import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import py4j.DefaultGatewayServerListener;
import py4j.GatewayServer;
import py4j.GatewayServerListener;
import py4j.Py4JServerConnection;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scripting server manager.
 */
public class ScriptingServer {
    private static final Logger.Log logger = Logger.getLogger(ScriptingServer.class);
    private static final AtomicInteger connections = new AtomicInteger(0);
    private static GatewayServer gatewayServer;
    private static GatewayServerListener listener;

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
        if (!Settings.settings.program.net.slave.active) {
            if (force && gatewayServer != null) {
                // Shutdown
                try {
                    dispose();
                } catch (Exception e) {
                    logger.error(e);
                }
            }
            if (gatewayServer == null) {
                try {
                    gatewayServer = new GatewayServer(scriptingInterface);
                    listener = new DefaultGatewayServerListener() {

                        @Override
                        public void connectionStarted(Py4JServerConnection gatewayConnection) {
                            logger.info("Connection started (" + connections.incrementAndGet() + "): " + gatewayConnection.getSocket().toString());
                        }

                        @Override
                        public void connectionStopped(Py4JServerConnection gatewayConnection) {
                            // Enable input, just in case
                            GaiaSky.postRunnable(() -> EventManager.publish(Event.INPUT_ENABLED_CMD, this, true));
                            logger.info("Connection stopped (" + connections.decrementAndGet() + "): " + gatewayConnection.getSocket().toString());
                        }

                        @Override
                        public void serverStarted() {
                            logger.info("Server started on port " + gatewayServer.getListeningPort());
                        }

                        @Override
                        public void serverStopped() {
                            logger.info("Server stopped");
                            initialize(scriptingInterface, true);
                        }

                        @Override
                        public void connectionError(Exception e) {
                            logger.error(e);
                        }

                        @Override
                        public void serverError(Exception e) {
                            logger.error(e);
                            initialize(scriptingInterface, force);
                        }
                    };
                    gatewayServer.addListener(listener);
                } catch (Exception e) {
                    logger.error(
                            "Could not initialize the Py4J gateway server. Proceeding without scripting.", e);
                }
            }
        }
    }

    /**
     * Actually starts the server. This must be called, or scripting won't work.
     */
    public static void startServer() {
        if (gatewayServer != null) {
            try {
                gatewayServer.start();
            } catch (Exception e) {
                logger.error(
                        "Could not start the scripting gateway server. Proceeding without scripting.", e);
            }
        }
    }

    public static void dispose() {
        if (gatewayServer != null) {
            if (listener != null) {
                gatewayServer.removeListener(listener);
                listener = null;
            }
            gatewayServer.shutdown();
            gatewayServer = null;
        }
    }
}
