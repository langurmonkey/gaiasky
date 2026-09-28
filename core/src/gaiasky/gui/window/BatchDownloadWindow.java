/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.window;

import com.badlogic.gdx.Net;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.gui.datasets.DatasetDownloadService;
import gaiasky.gui.datasets.DatasetWatcher;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Pair;
import gaiasky.util.TextUtils;
import gaiasky.util.datadesc.Dataset;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnProgressBar;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A window that downloads a list of datasets ({@link Dataset}) sequentially.
 * The actual download pipeline is delegated to {@link DatasetDownloadService};
 * this window only provides the per-dataset progress UI.
 */
public class BatchDownloadWindow extends GenericDialog {
    private static final Logger.Log logger = Logger.getLogger(BatchDownloadWindow.class);

    private final String infoString;
    private final List<Dataset> datasets;
    private final Set<DatasetWatcher> watchers;
    /** Runs when all downloads are successful. **/
    private Runnable success;
    /** Runs when at least one of the downloads fail. **/
    private Runnable error;

    private final Map<String, Pair<Dataset, Net.HttpRequest>> currentDownloads;
    /** The download service, which contains the actual download pipeline. **/
    private DatasetDownloadService downloadService;

    public BatchDownloadWindow(String title,
                               String info,
                               Skin skin,
                               Stage stage,
                               List<Dataset> datasets,
                               Runnable success,
                               Runnable error) {
        super(title, skin, stage);
        this.currentDownloads = Collections.synchronizedMap(new HashMap<>());
        this.watchers = new HashSet<>();

        this.infoString = info;
        this.datasets = datasets;
        this.success = success;
        this.error = error;

        setModal(true);

        buildSuper();
    }

    public BatchDownloadWindow(String title,
                               String info,
                               Skin skin,
                               Stage stage,
                               List<Dataset> datasets) {
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

        var status = new OwnLabel("idle", skin);

        for (var d : datasets) {
            var name = new OwnLabel(d.name, skin, "header-s");
            content.add(name).left().padRight(pad20).padBottom(pad10);

            var progress = new OwnProgressBar(0f, 100f, 0.1f, false, skin, "small-horizontal");
            progress.setPrefWidth(850f);
            progress.setValue(0f);
            progress.setVisible(true);

            var cell = content.add(progress);
            cell.left().padRight(pad20).padBottom(pad10).row();

            Runnable success = () -> {
                cell.clearActor();
                cell.setActor(new OwnLabel(I18n.msg("gui.done"), skin, "default-blue"));
            };

            var watcher = new DatasetWatcher(d, progress, null, status, null, success);
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

    private void downloadDataset(Dataset dataset,
                                 Runnable successRunnable) {
        if (downloadService == null) {
            downloadService = new DatasetDownloadService(currentDownloads);
        }
        downloadService.downloadDataset(dataset, skin, stage, successRunnable, new DatasetDownloadService.DownloadCallback() {
            @Override
            public void onSuccess(Dataset dataset) {
                actionEnableDataset(dataset);
            }

            @Override
            public void onError(Dataset dataset,
                                String message) {
                setStatusError(dataset, message);
                // Main error runnable.
                if (error != null) {
                    error.run();
                }
            }
        });
    }

    private void setStatusError(Dataset ds,
                                String message) {
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
    private void actionEnableDataset(Dataset dataset) {
        // Texture packs can't be enabled here.
        if (dataset.type.equals("texture-pack"))
            return;

        String filePath = null;
        if (dataset.checkStr != null) {
            filePath = TextUtils.ensureStartsWith(dataset.checkStr, Constants.DATA_LOCATION_TOKEN);
        }
        if (filePath != null && !filePath.isBlank()) {
            GaiaSky.settings().data.dataFiles.add(filePath);
        }
    }

    /**
     * Disable a given dataset, so that it is not loaded during startup.
     *
     * @param dataset The dataset to disable.
     */
    private void actionDisableDataset(Dataset dataset) {
        // Base data can't be disabled
        if (!dataset.baseData) {
            String filePath = null;
            if (dataset.checkStr != null) {
                filePath = TextUtils.ensureStartsWith(dataset.checkStr, Constants.DATA_LOCATION_TOKEN);
            }
            if (filePath != null && !filePath.isBlank()) {
                GaiaSky.settings().data.dataFiles.remove(filePath);
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
