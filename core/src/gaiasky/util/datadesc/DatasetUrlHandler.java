/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.datadesc;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.urlprotocol.ActionApiCall;
import gaiasky.util.urlprotocol.ActionFocus;
import gaiasky.util.urlprotocol.ActionGoto;
import gaiasky.util.urlprotocol.ActionHandler;
import gaiasky.util.urlprotocol.ActionLoad;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Handles <code>gaiasky://</code> URLs, such as those produced by the protocol handler
 * registered with the operating system.
 * <p>
 * Only a few actions are explicitly supported, see {@link DatasetUrlHandler#ACTIONS}.
 * Any other action is dispatched reflectively to the APIv2 scripting subsystem by
 * {@link ActionApiCall}, using the form <code>gaiasky://&lt;module&gt;/&lt;method&gt;?params</code>.
 * <p>
 * Example URLs:
 * <ul>
 * <li><code>gaiasky://load?dataset=gaia-dr3-nss</code></li>
 * <li><code>gaiasky://load?dataset=https%3A%2F%2Fgaia.ari.uni-heidelberg.de%2Fgaiasky%2Fdata%2Fcatalog.tar.gz</code></li>
 * <li><code>gaiasky://camera/focus_mode?name=Earth</code></li>
 * </ul>
 */
public class DatasetUrlHandler {
    private static final Log logger = Logger.getLogger(DatasetUrlHandler.class);

    /** The URL scheme handled by this class. **/
    public static final String URL_SCHEME = "gaiasky";

    public static final Map<String, ActionHandler> ACTIONS = Map.of(
            "load", new ActionLoad(),
            "focus", new ActionFocus(),
            "goto", new ActionGoto()
    );

    /** The last URL handled. Used to avoid handling the same URL twice. **/
    private static String lastHandledUrl = null;

    /**
     * URL received through a platform-specific channel (e.g. the macOS
     * open-URL Apple event) before the UI is ready. Consumed by the startup
     * path once the welcome GUI is built.
     **/
    private static String pendingUrl = null;

    private DatasetUrlHandler() {
    }

    /**
     * Returns true if the given URL has already been handled.
     *
     * @param url The URL.
     *
     * @return True if the URL has already been handled.
     */
    private static boolean isHandled(String url) {
        return url == null || url.equals(lastHandledUrl);
    }

    /**
     * Returns true if the given CLI argument is a {@link DatasetUrlHandler} URL.
     *
     * @param arg The CLI argument.
     *
     * @return True if the argument is a <code>gaiasky://</code> URL.
     */
    public static boolean isDatasetUrl(String arg) {
        return arg != null && arg.toLowerCase(Locale.ROOT).startsWith(URL_SCHEME + "://");
    }

    /**
     * Stores a URL received before the UI is ready (e.g. through the macOS
     * open-URL event). It will be processed by the startup path once the
     * welcome GUI is built.
     *
     * @param url The dataset URL.
     */
    public static void setPendingUrl(String url) {
        if (isDatasetUrl(url)) {
            pendingUrl = url;
        }
    }

    /**
     * Returns and clears the pending URL, if any.
     *
     * @return The pending dataset URL, or null if none is set.
     */
    public static String consumePendingUrl() {
        var url = pendingUrl;
        pendingUrl = null;
        return url;
    }

    /**
     * Handles the given URL with the default behavior (no hot-load). See
     * {@link #handle(String, Skin, Stage, boolean)}.
     *
     * @param url   The URL, as received in the CLI.
     * @param skin  The UI skin.
     * @param stage The UI stage.
     */
    public static void handle(String url,
                              Skin skin,
                              Stage stage) {
        handle(url, skin, stage, false);
    }

    /**
     * Handles the given URL, if it has not been handled yet. This must be called
     * from the GL thread, once a UI stage is available and the dataset metadata
     * has been fetched.
     * <p>
     * If {@code hotLoad} is false (startup path), an already-installed dataset is
     * only enabled — it will be loaded by the regular startup sequence. If
     * {@code hotLoad} is true (Gaia Sky already running), the dataset is loaded
     * immediately after installation/enabling through the hot-load API.
     * <p>
     * The deduplication guard only applies to the startup path ({@code hotLoad}
     * is false), where the same CLI URL may be processed more than once (e.g.
     * when the welcome GUI is rebuilt). URLs forwarded from a second instance
     * ({@code hotLoad} is true) are always processed, so that re-sending the
     * same URL works as expected.
     *
     * @param url     The URL, as received in the CLI.
     * @param skin    The UI skin.
     * @param stage   The UI stage.
     * @param hotLoad True to hot-load the dataset once installed/enabled.
     */
    public static void handle(String url,
                              Skin skin,
                              Stage stage,
                              boolean hotLoad) {
        // Deduplication only applies to the startup path. Forwarded URLs
        // (hot-load) are always processed.
        if (!hotLoad && (isHandled(url) || !isDatasetUrl(url))) {
            return;
        }
        if (!isDatasetUrl(url)) {
            return;
        }
        if (!hotLoad) {
            lastHandledUrl = url;
        }

        URI uri;
        try {
            uri = new URI(url);
        } catch (Exception e) {
            logger.error(e, "Malformed dataset URL: " + url);
            return;
        }

        // Parse action and parameters.
        String action = uri.getHost() != null ? uri.getHost() : uri.getSchemeSpecificPart();
        Map<String, String> params = parseQuery(uri.getRawQuery());

        // Run appropriate action.
        var handler = ACTIONS.get(action);
        if (handler == null) {
            // Not a built-in action. Fall back to the generic APIv2 dispatch,
            // which treats the host as an API module and the first path segment
            // as the method name, e.g. gaiasky://camera/focus_mode?name=Earth.
            handler = new ActionApiCall();
        }
        handler.process(url,
                        skin,
                        stage,
                        hotLoad,
                        params);

    }



    /**
     * Parses the raw query string of a URI into a map of key-value pairs.
     *
     * @param rawQuery The raw (still encoded) query string. May be null.
     *
     * @return A map with the decoded parameter names and values.
     */
    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> params = new HashMap<>();
        if (rawQuery != null && !rawQuery.isBlank()) {
            for (String pair : rawQuery.split("&")) {
                int eq = pair.indexOf('=');
                if (eq > 0) {
                    String key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
                    String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                    params.put(key, value);
                } else {
                    params.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
                }
            }
        }
        return params;
    }
}
