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
import py4j.ClientServer;
import py4j.DefaultGatewayServerListener;
import py4j.GatewayServerListener;
import py4j.Py4JServerConnection;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scripting server manager.
 */
public class ScriptingServer {
    private static final Logger.Log logger = Logger.getLogger(ScriptingServer.class);
    private static final AtomicInteger connections = new AtomicInteger(0);
    private static ClientServer gatewayServer;
    private static GatewayServerListener listener;
    private static IScriptingInterface scriptingInterface;

    public static void initialize(IScriptingInterface scriptingInterface) {
        initialize(scriptingInterface, false);
    }

    public static void initialize(IScriptingInterface scriptingInterface, boolean force) {
        if (!Settings.settings.program.net.slave.active) {
            ScriptingServer.scriptingInterface = scriptingInterface;
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
                    gatewayServer = new ClientServer(scriptingInterface);
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
                        public void serverPostShutdown() {
                            logger.debug("Post shutdown");
                        }

                        @Override
                        public void serverPreShutdown() {
                            logger.debug("Pre shutdown");
                        }

                        @Override
                        public void serverStarted() {
                            logger.info("Server started on port " + gatewayServer.getJavaServer().getListeningPort());
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
                    gatewayServer.getJavaServer().addListener(listener);
                } catch (Exception e) {
                    logger.error("Could not initialize the Py4J gateway server, is there another instance of Gaia Sky running? Proceeding without scripting...");
                    logger.error(e);
                }
            }
            try {
                gatewayServer.startServer();
            } catch (Exception e) {
                logger.error("Could not initialize the Py4J gateway server, is there another instance of Gaia Sky running? Proceeding without scripting...");
                logger.error(e);
            }
        }
    }

    public static void dispose() {
        if (gatewayServer != null) {
            if (listener != null) {
                gatewayServer.getJavaServer().removeListener(listener);
                listener = null;
            }
            gatewayServer.shutdown();
            gatewayServer = null;
        }
    }
}
