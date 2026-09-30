/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.datadesc;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.gui.datasets.DatasetDownloadService;
import gaiasky.gui.window.DatasetUrlConfirmWindow;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.SettingsManager;
import gaiasky.util.i18n.I18n;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Handles <code>gaiasky://</code> URLs, such as those produced by the protocol handler
 * registered with the operating system. The currently supported action is
 * <code>load</code>, which takes a <code>dataset</code> parameter that can be either:
 * <ul>
 * <li>A dataset <strong>key</strong> (ID), which is resolved against the local dataset
 * metadata (the data descriptor pulled from the servers at startup).</li>
 * <li>A full <strong>URL</strong> (<code>http</code>, <code>https</code> or <code>file</code>)
 * pointing to the dataset package (gzipped tarball), which is downloaded and installed
 * directly.</li>
 * </ul>
 * Example URLs:
 * <ul>
 * <li><code>gaiasky://load?dataset=gaia-dr3-nss</code></li>
 * <li><code>gaiasky://load?dataset=https%3A%2F%2Fgaia.ari.uni-heidelberg.de%2Fgaiasky%2Fdata%2Fcatalog.tar.gz</code></li>
 * </ul>
 */
public class DatasetUrlHandler {
    private static final Log logger = Logger.getLogger(DatasetUrlHandler.class);

    /** The URL scheme handled by this class. **/
    public static final String URL_SCHEME = "gaiasky";
    /** The 'load' action: download (if needed), install and enable a dataset. **/
    public static final String ACTION_LOAD = "load";
    /** Name of the parameter that contains the dataset key or URL. **/
    public static final String PARAM_DATASET = "dataset";

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

        String action = uri.getHost() != null ? uri.getHost() : uri.getSchemeSpecificPart();
        if (!ACTION_LOAD.equalsIgnoreCase(action)) {
            logger.warn("Unknown dataset URL action: " + action);
            return;
        }

        Map<String, String> params = parseQuery(uri.getRawQuery());
        String datasetParam = params.get(PARAM_DATASET);
        if (datasetParam == null || datasetParam.isBlank()) {
            logger.error("Dataset URL is missing the '" + PARAM_DATASET + "' parameter: " + url);
            return;
        }

        Dataset dataset = resolveDataset(datasetParam);
        if (dataset == null) {
            logger.error("Dataset not found in local metadata: " + datasetParam);
            postNotification(I18n.msg("gui.url.dataset.notfound", datasetParam));
            return;
        }

        // For URL datasets, the stub always reports exists=false. Check whether
        // a dataset with the same key or name is already installed in the data
        // location, and use that instead of re-downloading.
        if (isUrlDataset(datasetParam)) {
            var local = DatasetDownloadUtils.findLocalDataset(Path.of(GaiaSky.settings().data.location), dataset.key, dataset.name);
            if (local != null) {
                logger.info("Dataset from URL already installed locally: " + local.key);
                DatasetDownloadUtils.copyInstallFields(dataset, local);
            }
        }

        if (dataset.exists) {
            // Already installed. Enable it if it is not enabled yet, and
            // hot-load it if requested.
            enableDataset(dataset, hotLoad);
            return;
        }

        // Not installed. If it is a URL dataset (not a key resolved against the
        // server descriptor), ask the user for confirmation before downloading
        // and installing, unless it comes from one of the configured data
        // mirrors.
        if (isUrlDataset(datasetParam) && !DatasetUrlConfirmWindow.isFromDataMirror(dataset.file)) {
            var confirmDialog = new DatasetUrlConfirmWindow(dataset.file, skin, stage,
                                                            () -> Gdx.app.postRunnable(() -> downloadDataset(dataset, skin, stage, hotLoad)),
                                                            () -> logger.info("Dataset download from URL cancelled by user: " + dataset.file));
            confirmDialog.show(stage);
        } else {
            // Download and install through the dataset manager window, which
            // provides the progress UI and the install/enable pipeline. After
            // installation, the dataset is updated in place with the fields
            // from its descriptor (check path, type, etc.), so that it can be
            // enabled and hot-loaded.
            downloadDataset(dataset, skin, stage, hotLoad);
        }
    }

    /**
     * Enables the given dataset (adds its check string to the settings and
     * persists them) and hot-loads it if requested. Called after a successful
     * install of a URL-sourced dataset, once the descriptor has been resolved.
     *
     * @param dataset The installed dataset.
     * @param hotLoad True to hot-load the dataset after enabling it.
     */
    public static void enableAndHotLoad(Dataset dataset,
                                        boolean hotLoad) {
        enableDataset(dataset, hotLoad);
    }

    /**
     * Returns true if the given dataset parameter is a full URL (as opposed to
     * a dataset key resolved against the local metadata).
     *
     * @param datasetParam The dataset parameter.
     *
     * @return True if the parameter is a URL.
     */
    private static boolean isUrlDataset(String datasetParam) {
        String lower = datasetParam.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("file://");
    }

    /**
     * Resolves the dataset parameter, which can be either a dataset key or a full URL
     * to the dataset package.
     *
     * @param datasetParam The dataset key or URL.
     *
     * @return The dataset, or null if it could not be resolved.
     */
    private static Dataset resolveDataset(String datasetParam) {
        if (isUrlDataset(datasetParam)) {
            return datasetFromUrl(datasetParam);
        } else {
            // Dataset key. Look it up in the server descriptor first (it contains
            // the download URLs), then in the local descriptor.
            Dataset dd = null;
            if (DatasetGroup.serverDataDescriptor != null) {
                dd = DatasetGroup.serverDataDescriptor.findDatasetByKey(datasetParam);
            }
            if (dd == null && DatasetGroup.localDataDescriptor != null) {
                dd = DatasetGroup.localDataDescriptor.findDatasetByKey(datasetParam);
            }
            return dd;
        }
    }

    /**
     * Builds a minimal dataset descriptor from a full URL to a dataset package.
     *
     * @param url The URL to the dataset package (gzipped tarball).
     *
     * @return A dataset instance with the key, name and file fields set.
     */
    private static Dataset datasetFromUrl(String url) {
        Dataset dd = new Dataset();
        // Derive key and name from the file name, without extension.
        String filename = url;
        int slash = filename.lastIndexOf('/');
        if (slash >= 0) {
            filename = filename.substring(slash + 1);
        }
        int dot = filename.indexOf('.');
        if (dot > 0) {
            filename = filename.substring(0, dot);
        }
        dd.key = filename;
        dd.name = filename;
        dd.file = url;
        dd.type = "other";
        dd.exists = false;
        dd.status = Dataset.DatasetStatus.AVAILABLE;
        return dd;
    }

    /**
     * Enables an already-installed dataset, if it is not enabled yet, and persists
     * the settings. If {@code hotLoad} is true, the dataset is also loaded
     * immediately through the hot-load API.
     *
     * @param dataset The dataset to enable.
     * @param hotLoad True to hot-load the dataset after enabling it.
     */
    private static void enableDataset(Dataset dataset,
                                      boolean hotLoad) {
        if (dataset.checkStr != null
                && !dataset.type.equals("texture-pack")
                && !DatasetDownloadUtils.isEnabled(dataset)) {
            if (!GaiaSky.settings().data.dataFiles.contains(dataset.checkStr)) {
                GaiaSky.settings().data.dataFiles.add(dataset.checkStr);
            }
            var settingsManager = new SettingsManager();
            settingsManager.persist(GaiaSky.settings());
            logger.info("Dataset enabled via URL: " + dataset.key);
            postNotification(I18n.msg("gui.url.dataset.enabled", dataset.name));
            EventManager.publish(Event.ENABLED_DATASETS_RELOAD_CMD, dataset);
        } else {
            logger.info("Dataset already installed and enabled: " + dataset.key);
            postNotification(I18n.msg("gui.url.dataset.installed", dataset.name));
        }
        if (hotLoad) {
            hotLoadDataset(dataset);
        }
    }

    /**
     * Hot-loads an installed dataset through the scripting API, so that it is
     * available immediately, without a restart. This is the same mechanism used
     * by the dataset load dialog.
     *
     * @param dataset The dataset to load. It must be installed
     *                ({@code checkPath} must point to the dataset JSON file).
     */
    private static void hotLoadDataset(Dataset dataset) {
        if (dataset.checkPath == null || !Files.exists(dataset.checkPath)) {
            logger.error("Cannot hot-load dataset, check path does not exist: " + dataset.key);
            return;
        }
        logger.info("Hot-loading dataset via URL: " + dataset.key);
        GaiaSky.instance.getExecutorService().execute(() -> {
            var loaded = GaiaSky.instance.scripting()
                    .loadJsonCatalog(dataset.name, dataset.checkPath.toAbsolutePath().toString());
            if (loaded) {
                EventManager.publish(Event.POST_POPUP_NOTIFICATION,
                                     DatasetUrlHandler.class,
                                     I18n.msg("gui.url.dataset.loaded", dataset.name),
                                     10f);
            } else {
                EventManager.publish(Event.POST_POPUP_NOTIFICATION,
                                     DatasetUrlHandler.class,
                                     I18n.msg("gui.url.dataset.loadfail", dataset.name),
                                     -1f);
            }
        });
    }

    /**
     * Downloads and installs the given dataset using the headless dataset
     * download service. Progress is reported through the
     * {@link Event#UPDATE_LOAD_PROGRESS} events, which are picked up by the
     * load progress interface, so no window needs to be opened. If
     * {@code hotLoad} is true, the dataset is hot-loaded once the download and
     * installation finish successfully.
     *
     * @param dataset The dataset to download and install.
     * @param skin    The UI skin. May be null.
     * @param stage   The UI stage. May be null.
     * @param hotLoad True to hot-load the dataset after installing it.
     */
    private static void downloadDataset(Dataset dataset,
                                        Skin skin,
                                        Stage stage,
                                        boolean hotLoad) {
        logger.info("Downloading dataset from URL: " + dataset.file);
        var service = new DatasetDownloadService(new HashMap<>());
        service.downloadDataset(dataset, skin, stage, hotLoad ? () -> enableAndHotLoad(dataset, true) : null, null);
    }

    /**
     * Posts a popup notification.
     *
     * @param message The message.
     */
    private static void postNotification(String message) {
        EventManager.publish(Event.POST_POPUP_NOTIFICATION, DatasetUrlHandler.class, message, 10f);
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
