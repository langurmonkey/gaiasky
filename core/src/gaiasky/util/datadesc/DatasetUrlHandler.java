/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.datadesc;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.gui.datasets.DatasetManagerWindow;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.SettingsManager;
import gaiasky.util.i18n.I18n;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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

    /** Whether the CLI URL has already been handled. It must be handled only once. **/
    private static boolean handled = false;

    private DatasetUrlHandler() {
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
     * Handles the given URL, if it has not been handled yet. This must be called
     * from the GL thread, once a UI stage is available and the dataset metadata
     * has been fetched.
     *
     * @param url   The URL, as received in the CLI.
     * @param skin  The UI skin.
     * @param stage The UI stage.
     */
    public static void handle(String url,
                              Skin skin,
                              Stage stage) {
        if (handled || !isDatasetUrl(url)) {
            return;
        }
        handled = true;

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

        if (dataset.exists) {
            // Already installed. Enable it if it is not enabled yet.
            enableDataset(dataset);
        } else {
            // Not installed. Download and install through the dataset manager window,
            // which provides the progress UI and the install/enable pipeline.
            downloadDataset(dataset, skin, stage);
        }
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
        String lower = datasetParam.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("file://")) {
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
     * the settings.
     *
     * @param dataset The dataset to enable.
     */
    private static void enableDataset(Dataset dataset) {
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
        } else {
            logger.info("Dataset already installed and enabled: " + dataset.key);
            postNotification(I18n.msg("gui.url.dataset.installed", dataset.name));
        }
    }

    /**
     * Downloads and installs the given dataset using the dataset manager window,
     * which provides the progress UI and the full install pipeline.
     *
     * @param dataset The dataset to download and install.
     * @param skin    The UI skin.
     * @param stage   The UI stage.
     */
    private static void downloadDataset(Dataset dataset,
                                        Skin skin,
                                        Stage stage) {
        logger.info("Downloading dataset from URL: " + dataset.file);
        var dsw = new DatasetManagerWindow(stage, skin, DatasetGroup.serverDataDescriptor);
        dsw.show(stage);
        dsw.downloadDataset(dataset);
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
