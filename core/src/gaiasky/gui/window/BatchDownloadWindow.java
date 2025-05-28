/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.window;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.gui.datasets.DatasetWatcher;
import gaiasky.util.*;
import gaiasky.util.datadesc.DatasetDesc;
import gaiasky.util.datadesc.DatasetDownloadUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnProgressBar;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Consumer;

/**
 * A window that downloads a list of datasets ({@link DatasetDesc}) sequentially.
 */
public class BatchDownloadWindow extends GenericDialog {
    private static final Logger.Log logger = Logger.getLogger(BatchDownloadWindow.class);

    private final String infoString;
    private final List<DatasetDesc> datasets;
    private final DecimalFormat nf;
    private final Set<DatasetWatcher> watchers;
    /** Runs when all downloads are successful. **/
    private Runnable success;
    /** Runs when at least one of the downloads fail. **/
    private Runnable error;

    private final Map<String, Pair<DatasetDesc, Net.HttpRequest>> currentDownloads;

    public BatchDownloadWindow(String title, String info, Skin skin, Stage stage, List<DatasetDesc> datasets, Runnable success, Runnable error) {
        super(title, skin, stage);
        this.nf = new DecimalFormat("##0.0");
        this.currentDownloads = Collections.synchronizedMap(new HashMap<>());
        this.watchers = new HashSet<>();

        this.infoString = info;
        this.datasets = datasets;
        this.success = success;
        this.error = error;

        setModal(true);

        buildSuper();
    }

    public BatchDownloadWindow(String title, String info, Skin skin, Stage stage, List<DatasetDesc> datasets) {
        this(title, info, skin, stage, datasets, null, null);
    }

    public void setErrorRunnable(Runnable r) {
        this.error = r;
    }

    public void setSuccessRunnable(Runnable r) {
        this.success = r;
    }

    @Override
    protected void build() {
        content.clear();
        var info = new OwnLabel(infoString, skin);
        info.setAlignment(Align.center);
        content.add(info).colspan(2).padBottom(pad34).row();

        final var status = new OwnLabel("idle", skin);

        for (var d : datasets) {
            var name = new OwnLabel(d.name, skin, "header-s");
            content.add(name).left().padRight(pad20).padBottom(pad10);

            var progress = new OwnProgressBar(0f, 100f, 0.1f, false, skin, "small-horizontal");
            progress.setPrefWidth(850f);
            progress.setValue(0f);
            progress.setVisible(true);

            content.add(progress).left().padRight(pad20).padBottom(pad10).row();

            var watcher = new DatasetWatcher(d, progress, null, status, null);
            watchers.add(watcher);
        }

        content.add(status).colspan(2).padTop(pad34).padBottom(pad20).center();
    }

    public void downloadDatasets() {
        var runnable = prepareDataset(0);
        runnable.run();
    }

    private Runnable prepareDataset(int i) {
        return i < datasets.size() - 1 ?
                () -> {
                    downloadDataset(datasets.get(i), prepareDataset(i + 1));
                }
                :
                () -> {
                    downloadDataset(datasets.get(i),
                                    success);
                };
    }

    private void downloadDataset(DatasetDesc dataset, Runnable successRunnable) {
        var tempDir = SysUtils.getDataTempDir(Settings.settings.data.location);

        try {
            var fileStore = Files.getFileStore(tempDir);

            // Check for space. We need enough space for the compressed tar.gz package, plus the
            // extracted data, so we do s + s * 1.5, with a base compression ratio of 0.666.
            if (dataset.sizeBytes > 0 && dataset.sizeBytes + dataset.sizeBytes * 1.5 >= fileStore.getUsableSpace()) {
                var title = I18n.msg("gui.download.space.error.title");
                var msg = I18n.msg("gui.download.space.error", fileStore.toString());
                logger.error(msg);
                GuiUtils.addNotificationWindow(title, msg, skin, stage, null);
                return;
            }
        } catch (IOException e) {
            logger.warn(I18n.msg("gui.batch.error.filestore", tempDir));
        }

        String name = dataset.name;
        String url = dataset.file.replace("@mirror-url@", Settings.settings.program.url.dataMirror);

        String filename = FilenameUtils.getName(url);
        FileHandle tempDownload = Gdx.files.absolute(tempDir + "/" + filename + ".part");

        ProgressRunnable progressDownload = (read, total, progress, speed) -> {
            try {
                double readMb = (double) read / 1e6d;
                double totalMb = (double) total / 1e6d;
                final String progressString = progress >= 100 ? I18n.msg("gui.done") : I18n.msg("gui.download.downloading", nf.format(progress));
                double mbPerSecond = speed / 1000d;
                final String speedString = nf.format(readMb) + "/" + nf.format(totalMb) + " MB (" + nf.format(mbPerSecond) + " MB/s)";
                // Since we are downloading on a background thread, post a runnable to touch UI.
                GaiaSky.postRunnable(() -> {
                    EventManager.publish(Event.DATASET_DOWNLOAD_PROGRESS_INFO, this, dataset.key, (float) progress, progressString, speedString);
                });
            } catch (Exception e) {
                logger.warn(I18n.msg("gui.download.error.progress"));
            }
        };
        ProgressRunnable progressHashResume = (read, total, progress, speed) -> {
            double readMb = (double) read / 1e6d;
            double totalMb = (double) total / 1e6d;
            final String progressString = progress >= 100 ? I18n.msg("gui.done") : I18n.msg("gui.download.checksum.check", nf.format(progress));
            double mbPerSecond = speed / 1000d;
            final String speedString = nf.format(readMb) + "/" + nf.format(totalMb) + " MB (" + nf.format(mbPerSecond) + " MB/s)";
            // Since we are downloading on a background thread, post a runnable to touch UI.
            GaiaSky.postRunnable(() -> {
                EventManager.publish(Event.DATASET_DOWNLOAD_PROGRESS_INFO, this, dataset.key, (float) progress, progressString, speedString);
            });
        };

        Consumer<String> finish = (digest) -> {
            String errorMsg = null;
            // Unpack.
            int errors = 0;
            logger.info(I18n.msg("gui.download.extracting", tempDownload.path()));
            String dataLocation = Settings.settings.data.location + File.separatorChar;
            // Checksum.
            if (digest != null && dataset.sha256 != null) {
                String serverDigest = dataset.sha256;
                try {
                    final var ok = serverDigest.equals(digest);
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
                try {
                    // Extract.
                    DatasetDownloadUtils.decompress(tempDownload.path(), new File(dataLocation), dataset);
                } catch (Exception e) {
                    logger.error(e, I18n.msg("gui.download.decompress.error", name));
                    errorMsg = I18n.msg("gui.download.decompress.error.msg");
                    errors++;
                } finally {
                    // Set to 100% completion.
                    EventManager.publish(Event.DATASET_DOWNLOAD_PROGRESS_INFO, this, dataset.key, (float) 100, "complete", "-");
                    // Remove archive.
                    DatasetDownloadUtils.cleanupTempFile(tempDownload.path());
                }
            }

            final String errorMessage = errorMsg;
            final int numErrors = errors;
            // Done.
            GaiaSky.postRunnable(() -> {
                currentDownloads.remove(dataset.key);

                if (numErrors == 0) {
                    // Ok message.
                    EventManager.publish(Event.DATASET_DOWNLOAD_FINISH_INFO, this, dataset.key, 0);
                    dataset.exists = true;
                    actionEnableDataset(dataset);
                    if (successRunnable != null) {
                        successRunnable.run();
                    }
                    EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.download.finished", name), -1f);
                } else {
                    logger.error(I18n.msg("gui.download.failed", name + " - " + url));
                    tempDownload.delete();
                    setStatusError(dataset, errorMessage);
                    currentDownloads.remove(dataset.key);
                    EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.download.failed", name), -1f);
                    // Main error runnable.
                    if (error != null) {
                        error.run();
                    }
                }
            });

        };

        Runnable fail = () -> {
            logger.error(I18n.msg("gui.download.failed", name + " - " + url));
            tempDownload.delete();
            setStatusError(dataset);
            currentDownloads.remove(dataset.key);
            EventManager.publish(Event.POST_POPUP_NOTIFICATION, this, I18n.msg("gui.download.failed", name), -1f);
            // Run main error runnable.
            if (error != null) {
                error.run();
            }
        };

        // Download.
        final Net.HttpRequest request = DownloadHelper.downloadFile(url,
                                                                    tempDownload,
                                                                    Settings.settings.program.offlineMode,
                                                                    progressDownload,
                                                                    progressHashResume,
                                                                    finish,
                                                                    fail,
                                                                    null);
        GaiaSky.postRunnable(() -> EventManager.publish(Event.DATASET_DOWNLOAD_START_INFO, this, dataset.key, request));
        currentDownloads.put(dataset.key, new Pair<>(dataset, request));

    }

    private void setStatusError(DatasetDesc ds) {
        setStatusError(ds, null);
    }

    private void setStatusError(DatasetDesc ds, String message) {
        if (message != null && !message.isEmpty()) {
            EventManager.publish(Event.DATASET_DOWNLOAD_FINISH_INFO, this, ds.key, 1, message);
        } else {
            EventManager.publish(Event.DATASET_DOWNLOAD_FINISH_INFO, this, ds.key, 1);
        }
    }

    /**
     * Enables a given dataset, so that it is loaded when Gaia Sky starts.
     *
     * @param dataset The dataset to enable.
     */
    private void actionEnableDataset(DatasetDesc dataset) {
        // Texture packs can't be enabled here.
        if (dataset.type.equals("texture-pack"))
            return;

        String filePath = null;
        if (dataset.checkStr != null) {
            filePath = TextUtils.ensureStartsWith(dataset.checkStr, Constants.DATA_LOCATION_TOKEN);
        }
        if (filePath != null && !filePath.isBlank()) {
            Settings.settings.data.dataFiles.add(filePath);
        }
    }

    /**
     * Disable a given dataset, so that it is not loaded during startup.
     *
     * @param dataset The dataset to disable.
     */
    private void actionDisableDataset(DatasetDesc dataset) {
        // Base data can't be disabled
        if (!dataset.baseData) {
            String filePath = null;
            if (dataset.checkStr != null) {
                filePath = TextUtils.ensureStartsWith(dataset.checkStr, Constants.DATA_LOCATION_TOKEN);
            }
            if (filePath != null && !filePath.isBlank()) {
                Settings.settings.data.dataFiles.remove(filePath);
            }
        }
    }


    @Override
    protected boolean accept() {
        return false;
    }

    @Override
    protected void cancel() {

    }

    @Override
    public void dispose() {
        watchers.forEach(EventManager.instance::removeAllSubscriptions);
        watchers.clear();
    }
}
