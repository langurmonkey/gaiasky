package gaiasky.interafce;

import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.util.I18n;
import gaiasky.util.datadesc.DatasetDesc;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnProgressBar;
import gaiasky.util.scene2d.OwnTextIconButton;

/**
 * Listens to dataset manager events and relays them to the
 * interested UI elements.
 */
public class DatasetWatcher implements IObserver {
    private DatasetDesc dataset;
    private OwnProgressBar progress;
    private OwnTextIconButton button;
    private OwnLabel status;

    public DatasetWatcher(DatasetDesc dataset, OwnProgressBar progress, OwnTextIconButton button, OwnLabel status) {
        super();
        this.dataset = dataset;
        this.progress = progress;
        this.button = button;
        this.status = status;

        EventManager.instance.subscribe(this, Events.DATASET_DOWNLOAD_START_INFO, Events.DATASET_DOWNLOAD_FINISH_INFO, Events.DATASET_DOWNLOAD_PROGRESS_INFO);

    }

    public void dispose() {
        EventManager.instance.removeAllSubscriptions(this);
    }

    @Override
    public void notify(Events event, Object... data) {
        if (data.length > 0) {
            if (data[0] instanceof String) {
                String key = (String) data[0];
                if (dataset.key != null && dataset.key.equals(key)) {
                    // Only respond to my key
                    switch (event) {
                    case DATASET_DOWNLOAD_START_INFO -> {
                        if (this.progress != null) {
                            this.progress.setVisible(true);
                            this.progress.setValue(0f);
                        }
                        if (this.button != null) {
                            this.button.setDisabled(true);
                        }
                        if (this.status != null) {
                            this.status.setText(I18n.txt("gui.download.starting", dataset.name));
                        }
                    }
                    case DATASET_DOWNLOAD_FINISH_INFO -> {
                        final int status = (Integer) data[1];
                        String messageKey = switch (status) {
                            case 0 -> "gui.download.status.found";
                            case 1 -> "gui.download.status.failed";
                            case 2 -> "gui.download.status.cancelled";
                            case 3 -> "gui.download.status.notfound";
                            default -> "gui.download.status.working";

                        };
                        if (this.status != null) {
                            this.status.setText(I18n.txt(messageKey));
                        }
                    }
                    case DATASET_DOWNLOAD_PROGRESS_INFO -> {
                        float progress = (Float) data[1];
                        String status = (String) data[2];
                        String speed = (String) data[3];

                        if (this.progress != null) {
                            this.progress.setVisible(true);
                            this.progress.setValue(progress);
                        }
                        if (this.status != null) {
                            String statusStr = status;
                            if (speed != null) {
                                statusStr += "  " + speed;
                            }
                            this.status.setText(statusStr);
                        }
                    }
                    }
                }
            }
        }
    }
}
