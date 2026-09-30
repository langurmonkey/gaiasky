/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.datasets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.util.DownloadHelper;
import gaiasky.util.GuiUtils;
import gaiasky.util.Logger;
import gaiasky.util.Pair;
import gaiasky.util.ProgressRunnable;
import gaiasky.util.SysUtils;
import gaiasky.util.datadesc.Dataset;
import gaiasky.util.datadesc.DatasetDownloadUtils;
import gaiasky.util.i18n.I18n;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Headless dataset download service. Contains the full download-install
 * pipeline (disk space check, download with progress, checksum verification,
 * extraction), decoupled from any UI. It publishes the
 * <code>Event#DATASET_DOWNLOAD_*</code> events, consumed by the dataset manager
 * window, and additionally {@link Event#UPDATE_LOAD_PROGRESS} events, consumed
 * by the load progress interface, so that downloads started outside the
 * dataset manager window (e.g. through a {@code gaiasky://} URL) still show
 * progress feedback.
 * <p>
 * UI-specific reactions (status updates in the dataset table, etc.) are
 * delegated to a {@link DownloadCallback} provided by the caller.
 */
public class DatasetDownloadService {
    private static final Logger.Log logger = Logger.getLogger(DatasetDownloadService.class);

    /** Downloads in progress, keyed by dataset key. **/
    private final Map<String, Pair<Dataset, Net.HttpRequest>> currentDownloads;

    /** Number formatter for progress strings. **/
    private final DecimalFormat nf = new DecimalFormat("##0.0");

    /**
     * Callback for UI-specific reactions to the download lifecycle. All methods
     * are invoked on the main thread.
     */
    public interface DownloadCallback {
        /**
         * The download has started.
         *
         * @param dataset The dataset.
         * @param request The HTTP request handling the download.
         */
        default void onStarted(Dataset dataset, Net.HttpRequest request) {
        }

        /**
         * The download, installation and enablement finished successfully.
         *
         * @param dataset The dataset.
         */
        default void onSuccess(Dataset dataset) {
        }

        /**
         * The download or installation failed.
         *
         * @param dataset The dataset.
         * @param message The error message, may be null.
         */
        default void onError(Dataset dataset, String message) {
        }

        /**
         * The download was cancelled.
         *
         * @param dataset The dataset.
         */
        default void onCancelled(Dataset dataset) {
        }
    }

    /**
     * Creates a new dataset download service.
     *
     * @param currentDownloads Map of downloads in progress, keyed by dataset
     *                         key. Owned by the caller (typically the dataset
     *                         manager window), and updated by this service.
     */
    public DatasetDownloadService(Map<String, Pair<Dataset, Net.HttpRequest>> currentDownloads) {
        this.currentDownloads = currentDownloads;
    }

    /**
     * Downloads and installs the given dataset. This is the full pipeline:
     * disk space check, download with progress reporting, checksum
     * verification and extraction. Progress is published through the
     * {@link Event#DATASET_DOWNLOAD_PROGRESS_INFO} and
     * {@link Event#UPDATE_LOAD_PROGRESS} events. UI-specific reactions are
     * delegated to the given callback.
     *
     * @param dataset         The dataset to download and install.
     * @param skin            The UI skin, for error dialogs. May be null.
     * @param stage           The UI stage, for error dialogs. May be null.
     * @param successRunnable Runnable to run after a successful install, before
     *                        the callback. May be null.
     * @param callback        The callback for UI-specific reactions. May be null.
     */
    public void downloadDataset(Dataset dataset,
                                Skin skin,
                                Stage stage,
                                Runnable successRunnable,
                                DownloadCallback callback) {
        // Local files (file:// URLs) don't need downloading: extract directly.
        if (dataset.file != null && dataset.file.toLowerCase(java.util.Locale.ROOT).startsWith("file://")) {
            installLocalArchive(dataset, skin, stage, successRunnable, callback);
            return;
        }
        var tempDir = SysUtils.getDataTempDir(GaiaSky.settings().data.location);

        try {
            var fileStore = Files.getFileStore(tempDir);

            // Check for space. We need enough space for the compressed tar.gz package, plus the
            // extracted data, so we do s + s * 1.5, with a base compression ratio of 0.666.
            if (dataset.sizeBytes > 0 && dataset.sizeBytes + dataset.sizeBytes * 1.5 >= fileStore.getUsableSpace()) {
                var title = I18n.msg("gui.download.space.error.title");
                var msg = I18n.msg("gui.download.space.error", fileStore.toString());
                logger.error(msg);
                if (skin != null && stage != null) {
                    GuiUtils.addNotificationWindow(title, msg, skin, stage, null);
                } else {
                    EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, msg, -1f);
                }
                return;
            }
        } catch (IOException e) {
            logger.warn("Error getting file store for temp dir: " + tempDir);
        }

        String name = dataset.name;
        String url = dataset.file.replace(DatasetDownloadUtils.mirrorKeyword, GaiaSky.settings().program.url.getCurrentDataMirror());

        String filename = FilenameUtils.getName(url);
        FileHandle tempDownload = Gdx.files.absolute(tempDir + "/" + filename + ".part");

        ProgressRunnable progressDownload = (read, total, progress, speed) -> {
            try {
                double readMb = (double) read / 1e6d;
                double totalMb = (double) total / 1e6d;
                String progressString = progress >= 100 ? I18n.msg("gui.done") : I18n.msg("gui.download.downloading", nf.format(progress));
                double mbPerSecond = speed / 1000d;
                String speedString = nf.format(readMb) + "/" + nf.format(totalMb) + " MB (" + nf.format(mbPerSecond) + " MB/s)";
                // Since we are downloading on a background thread, post a runnable to touch UI.
                GaiaSky.postRunnable(() -> {
                    EventManager.publish(Event.DATASET_DOWNLOAD_PROGRESS_INFO,
                                         this,
                                         dataset.key,
                                         (float) progress,
                                         progressString,
                                         speedString);
                    // Also publish to the generic load progress interface.
                    EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, name, (float) progress / 100f);
                });
            } catch (Exception e) {
                logger.warn(I18n.msg("gui.download.error.progress"));
            }
        };
        ProgressRunnable progressHashResume = (read, total, progress, speed) -> {
            double readMb = (double) read / 1e6d;
            double totalMb = (double) total / 1e6d;
            String progressString = progress >= 100 ? I18n.msg("gui.done") : I18n.msg("gui.download.checksum.check", nf.format(progress));
            double mbPerSecond = speed / 1000d;
            String speedString = nf.format(readMb) + "/" + nf.format(totalMb) + " MB (" + nf.format(mbPerSecond) + " MB/s)";
            // Since we are downloading on a background thread, post a runnable to touch UI.
            GaiaSky.postRunnable(() -> {
                EventManager.publish(Event.DATASET_DOWNLOAD_PROGRESS_INFO,
                                     this,
                                     dataset.key,
                                     (float) progress,
                                     progressString,
                                     speedString);
                // Also publish to the generic load progress interface.
                EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, name, (float) progress / 100f);
            });
        };

        // The whole finish process runs in serial mode thanks to the extraction lock.
        // Prevents sync issues with file extraction and UI update.
        Consumer<String> finish = (digest) -> {
            DatasetDownloadUtils.EXTRACTION_LOCK.lock();
            try {
                String errorMsg = null;
                // Unpack.
                int errors = 0;
                logger.info(I18n.msg("gui.download.extracting", tempDownload.path()));
                String dataLocation = GaiaSky.settings().data.location + File.separatorChar;
                // Checksum.
                if (digest != null && dataset.sha256 != null) {
                    String serverDigest = dataset.sha256;
                    try {
                        boolean ok = serverDigest.equals(digest);
                        if (ok) {
                            logger.info(I18n.msg("gui.download.checksum.ok", name));
                        } else {
                            logger.error(I18n.msg("gui.download.checksum.fail", name));
                            errorMsg = I18n.msg("gui.download.checksum.fail.msg");
                            errors++;
                            EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.download.checksum.error", name), -1f);
                        }
                    } catch (Exception e) {
                        logger.info(I18n.msg("gui.download.checksum.error", name));
                        errorMsg = I18n.msg("gui.download.checksum.fail.msg");
                        errors++;
                        EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.download.checksum.error", name), -1f);
                    }
                } else {
                    logger.info(I18n.msg("gui.download.checksum.notfound", name));
                    EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.download.checksum.notfound", name), -1f);
                }

                if (errors == 0) {
                    // Snapshot the dataset.json descriptors before extraction,
                    // so that we can find the ones added by this archive.
                    var dataLocationPath = Path.of(dataLocation);
                    var descriptorsBefore = DatasetDownloadUtils.snapshotDescriptors(dataLocationPath);
                    try {
                        // Extract.
                        DatasetDownloadUtils.decompress(tempDownload.path(), new File(dataLocation), dataset);
                        // Resolve the descriptor(s) added by the extraction and
                        // update the dataset in place, so that it becomes a
                        // real, installed dataset (check path, type, etc.).
                        var newDescriptors = DatasetDownloadUtils.findNewDescriptors(descriptorsBefore, dataLocationPath);
                        if (!newDescriptors.isEmpty()) {
                            var resolved = DatasetDownloadUtils.datasetFromDescriptor(newDescriptors.get(0));
                            if (resolved != null) {
                                DatasetDownloadUtils.copyInstallFields(dataset, resolved);
                                logger.info("Resolved installed dataset from descriptor: " + dataset.key + " -> " + dataset.checkStr);
                            }
                        }
                    } catch (Exception e) {
                        logger.error(e, I18n.msg("gui.download.decompress.error", name));
                        errorMsg = I18n.msg("gui.download.decompress.error.msg");
                        errors++;
                    } finally {
                        // Remove archive.
                        DatasetDownloadUtils.cleanupTempFile(tempDownload.path());
                    }
                }

                String errorMessage = errorMsg;
                int numErrors = errors;
                // Done.
                GaiaSky.postRunnable(() -> {
                    currentDownloads.remove(dataset.key);

                    if (numErrors == 0) {
                        // Ok message.
                        EventManager.publish(Event.DATASET_DOWNLOAD_FINISH_INFO, this, dataset.key, 0);
                        dataset.exists = true;
                        if (successRunnable != null) {
                            successRunnable.run();
                        }
                        if (callback != null) {
                            callback.onSuccess(dataset);
                        }
                        EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.download.finished", name), -1f);
                    } else {
                        logger.error(I18n.msg("gui.download.failed", name + " - " + url));
                        tempDownload.delete();
                        if (callback != null) {
                            callback.onError(dataset, errorMessage);
                        }
                        EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.download.failed", name), -1f);
                    }
                    // Remove the progress bar.
                    EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, name, 2f);
                });
            } finally {
                DatasetDownloadUtils.EXTRACTION_LOCK.unlock();
            }

        };

        Runnable fail = () -> {
            logger.error(I18n.msg("gui.download.failed", name + " - " + url));
            tempDownload.delete();
            GaiaSky.postRunnable(() -> {
                currentDownloads.remove(dataset.key);
                if (callback != null) {
                    callback.onError(dataset, null);
                }
                EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.download.failed", name), -1f);
                EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, name, 2f);
            });
        };

        Runnable cancel = () -> {
            logger.error(I18n.msg("gui.download.cancelled", name + " - " + url));
            GaiaSky.postRunnable(() -> {
                currentDownloads.remove(dataset.key);
                if (callback != null) {
                    callback.onCancelled(dataset);
                }
                EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.download.cancelled", name), 10f);
                EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, name, 2f);
            });
        };

        // Download.
        Net.HttpRequest request = DownloadHelper.downloadFile(url,
                                                              tempDownload,
                                                              GaiaSky.settings().program.offlineMode,
                                                              progressDownload,
                                                              progressHashResume,
                                                              finish,
                                                              fail,
                                                              cancel);
        GaiaSky.postRunnable(() -> {
            EventManager.publish(Event.DATASET_DOWNLOAD_START_INFO, this, dataset.key, request);
            if (callback != null) {
                callback.onStarted(dataset, request);
            }
        });
        currentDownloads.put(dataset.key, new Pair<>(dataset, request));

    }

    /**
     * Installs a dataset from a local archive (a {@code file://} URL). Skips
     * the download and checksum steps, and goes straight to extraction under
     * the extraction lock. After extraction, the dataset descriptor added by
     * the archive is resolved and the dataset is updated in place.
     *
     * @param dataset         The dataset, with {@code file} pointing to a
     *                        {@code file://} URL of a gzipped tarball.
     * @param skin            The UI skin, for error dialogs. May be null.
     * @param stage           The UI stage, for error dialogs. May be null.
     * @param successRunnable Runnable to run after a successful install. May be null.
     * @param callback        The callback for UI-specific reactions. May be null.
     */
    private void installLocalArchive(Dataset dataset,
                                     Skin skin,
                                     Stage stage,
                                     Runnable successRunnable,
                                     DownloadCallback callback) {
        logger.info("Installing dataset from local archive: " + dataset.file);
        var dataLocation = Path.of(GaiaSky.settings().data.location);
        // file:// URLs are typically absolute paths.
        var rawPath = dataset.file.substring("file://".length());
        var archivePathTmp = Path.of(rawPath);
        final var archivePath = archivePathTmp.isAbsolute() ? archivePathTmp : dataLocation.resolve(rawPath);
        if (!Files.exists(archivePath)) {
            var msg = I18n.msg("gui.download.failed", dataset.name) + " (" + archivePath + ")";
            logger.error(msg);
            EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, msg, -1f);
            if (callback != null) {
                callback.onError(dataset, msg);
            }
            return;
        }

        GaiaSky.postRunnable(() -> EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, dataset.name, 0.5f));

        new Thread(() -> {
            DatasetDownloadUtils.EXTRACTION_LOCK.lock();
            try {
                var descriptorsBefore = DatasetDownloadUtils.snapshotDescriptors(dataLocation);
                String errorMsg = null;
                String errorDetail = null;
                try {
                    DatasetDownloadUtils.decompress(archivePath.toAbsolutePath().toString(), dataLocation.toFile(), dataset);
                    var newDescriptors = DatasetDownloadUtils.findNewDescriptors(descriptorsBefore, dataLocation);
                    if (!newDescriptors.isEmpty()) {
                        var resolved = DatasetDownloadUtils.datasetFromDescriptor(newDescriptors.get(0));
                        if (resolved != null) {
                            DatasetDownloadUtils.copyInstallFields(dataset, resolved);
                            logger.info("Resolved installed dataset from descriptor: " + dataset.key + " -> " + dataset.checkStr);
                        }
                    }
                } catch (Exception e) {
                    logger.error(e, I18n.msg("gui.download.decompress.error", dataset.name));
                    errorMsg = I18n.msg("gui.download.decompress.error.msg");
                    errorDetail = e.getMessage();
                }

                String errorMessage = errorMsg;
                String errorDetailMessage = errorDetail;
                GaiaSky.postRunnable(() -> {
                    currentDownloads.remove(dataset.key);
                    if (errorMessage == null) {
                        dataset.exists = true;
                        EventManager.publish(Event.DATASET_DOWNLOAD_FINISH_INFO, this, dataset.key, 0);
                        if (successRunnable != null) {
                            successRunnable.run();
                        }
                        if (callback != null) {
                            callback.onSuccess(dataset);
                        }
                        EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.download.finished", dataset.name), -1f);
                    } else {
                        if (callback != null) {
                            callback.onError(dataset, errorDetailMessage);
                        }
                        EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.download.failed", dataset.name), -1f);
                    }
                    EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, dataset.name, 2f);
                });
            } finally {
                DatasetDownloadUtils.EXTRACTION_LOCK.unlock();
            }
        }, "gaiasky-local-dataset-install").start();
    }
}
