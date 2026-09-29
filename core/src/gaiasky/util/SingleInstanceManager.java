/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import gaiasky.util.datadesc.DatasetUrlHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements single-instance handling for Gaia Sky using a TCP loopback
 * server socket. This works on Linux, macOS and Windows.
 * <p>
 * When a second instance of Gaia Sky is started with a {@code gaiasky://} URL,
 * it connects to the loopback port of the already-running instance, forwards
 * the URL, and exits. The running instance receives the URL and processes it
 * (typically by downloading and hot-loading the dataset).
 * <p>
 * A simple handshake token is used so that the port is not confused with
 * other programs that may happen to occupy it.
 * <p>
 * The server side is stateful and is owned by the {@link gaiasky.GaiaSky}
 * instance, like the other managers. The client side is a one-shot static
 * utility ({@link #forwardToRunningInstance(String)}), as it runs before any
 * Gaia Sky instance exists.
 */
public class SingleInstanceManager {
    /**
     * Fixed port for the single-instance loopback server. It is bound to
     * {@code 127.0.0.1} only, so no external connections are possible.
     **/
    private static final int PORT = 54123;
    /** Handshake token sent by the client and expected by the server. **/
    private static final String TOKEN = "GAIASKY-SINGLE-INSTANCE";
    /** Connect/read timeout in milliseconds. **/
    private static final int TIMEOUT_MS = 3000;

    /** The server socket, if the server has been started. **/
    private ServerSocket serverSocket;
    /** Listeners to notify when a URL is received from another instance. **/
    private final List<UrlListener> listeners = new ArrayList<>();
    /** URLs received before any listener was registered. **/
    private final List<String> pendingUrls = new ArrayList<>();
    /** The server accept loop thread. **/
    private Thread serverThread;
    /** The active manager instance, if a server has been started. **/
    private static SingleInstanceManager activeInstance;

    /**
     * Attempts to forward the given URL to an already-running Gaia Sky instance.
     * This is a one-shot static utility, as it is called before any Gaia Sky
     * instance exists.
     *
     * @param url The URL to forward.
     *
     * @return True if the URL was successfully forwarded to a running instance,
     * false if no running instance was found.
     */
    public static boolean forwardToRunningInstance(String url) {
        try (Socket socket = new Socket()) {
            SocketAddress address = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT);
            socket.connect(address, TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);
            var out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            out.println(TOKEN);
            out.println(url);
            // Wait for the acknowledgment.
            var in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            return "OK".equals(in.readLine());
        } catch (ConnectException | SocketTimeoutException e) {
            // No instance running.
            return false;
        } catch (IOException e) {
            Logger.getLogger(SingleInstanceManager.class).error(e);
            return false;
        }
    }

    /**
     * Starts the single-instance server, if it is not running yet. The server
     * runs on a daemon thread and accepts connections from new instances.
     * If the port is already taken (by another Gaia Sky instance or by another
     * program), the server does not start — this is not an error, as the
     * forwarding attempt has usually already failed in that case.
     */
    public void startServer() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            return;
        }
        try {
            serverSocket = new ServerSocket(PORT, 1, InetAddress.getByName("127.0.0.1"));
        } catch (IOException e) {
            // Port in use. Not fatal — the instance just runs as usual.
            Logger.getLogger(SingleInstanceManager.class).info("Single-instance server port in use, not starting: " + e.getMessage());
            return;
        }
        activeInstance = this;
        serverThread = new Thread(this::acceptLoop, "gaiasky-single-instance");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    /**
     * Stops the single-instance server, if it is running.
     */
    public void stopServer() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
            serverSocket = null;
        }
        if (serverThread != null) {
            serverThread.interrupt();
            serverThread = null;
        }
        if (activeInstance == this) {
            activeInstance = null;
        }
    }

    /**
     * Returns the active manager instance, if a single-instance server is
     * running in this process. Used by the macOS open-URL handler to decide
     * whether to forward the URL to the running instance (another process)
     * or to store it as pending in this one.
     *
     * @return The active {@link SingleInstanceManager}, or null if the
     * server has not been started in this process.
     */
    public static SingleInstanceManager getActiveInstance() {
        return activeInstance;
    }

    /**
     * Registers a listener to be notified when a URL is received from another
     * instance. Any URLs received before this call are delivered immediately.
     *
     * @param listener The listener.
     */
    public void addListener(UrlListener listener) {
        List<String> pending;
        synchronized (listeners) {
            listeners.add(listener);
        }
        synchronized (pendingUrls) {
            if (pendingUrls.isEmpty()) {
                return;
            }
            pending = new ArrayList<>(pendingUrls);
            pendingUrls.clear();
        }
        for (String url : pending) {
            listener.handleUrl(url);
        }
    }

    private void acceptLoop() {
        while (serverSocket != null && !serverSocket.isClosed()) {
            try (Socket socket = serverSocket.accept()) {
                socket.setSoTimeout(TIMEOUT_MS);
                var in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                String token = in.readLine();
                if (!TOKEN.equals(token)) {
                    continue;
                }
                String url = in.readLine();
                var out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
                if (url != null && DatasetUrlHandler.isDatasetUrl(url)) {
                    out.println("OK");
                    dispatch(url);
                } else {
                    out.println("ERR");
                }
            } catch (IOException e) {
                // Socket closed or I/O error — exit the loop if the server is down.
                if (serverSocket == null || serverSocket.isClosed()) {
                    return;
                }
            }
        }
    }

    /**
     * Dispatches a URL received from another instance to the registered
     * listeners, or stores it as pending if no listener is registered yet.
     *
     * @param url The URL.
     */
    private void dispatch(String url) {
        synchronized (listeners) {
            if (!listeners.isEmpty()) {
                for (var listener : listeners) {
                    listener.handleUrl(url);
                }
                return;
            }
        }
        synchronized (pendingUrls) {
            pendingUrls.add(url);
        }
    }

    /**
     * Public entry point to dispatch a URL received locally in this process
     * (e.g. through the macOS open-URL handler when this instance is the
     * running one). Same behavior as {@link #dispatch(String)}.
     *
     * @param url The URL.
     */
    public void dispatchUrl(String url) {
        dispatch(url);
    }

    /**
     * Listener interface for URLs received from other instances.
     */
    @FunctionalInterface
    public interface UrlListener {
        /**
         * Called when a URL is received from another instance.
         *
         * @param url The URL.
         */
        void handleUrl(String url);
    }
}