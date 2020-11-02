/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.desktop.util.SysUtils;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.datadesc.DataDescriptor;
import gaiasky.util.datadesc.DataDescriptorUtils;
import gaiasky.util.datadesc.DatasetDesc;
import gaiasky.util.datadesc.DatasetType;
import gaiasky.util.format.INumberFormat;
import gaiasky.util.format.NumberFormatFactory;
import gaiasky.util.io.FileInfoInputStream;
import gaiasky.util.scene2d.*;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;

/**
 * Download manager. It gets a descriptor file from the server containing all
 * available datasets, detects them in the current system and offers and manages
 * their downloads.
 *
 * @author tsagrista
 */
public class DownloadDataWindow extends GenericDialog {
    private static final Log logger = Logger.getLogger(DownloadDataWindow.class);

    private static final Map<String, String> iconMap;

    static {
        iconMap = new HashMap<>();
        iconMap.put("other", "icon-elem-others");
        iconMap.put("data-pack", "icon-elem-others");
        iconMap.put("catalog-lod", "icon-elem-stars");
        iconMap.put("catalog-gaia", "icon-elem-stars");
        iconMap.put("catalog", "icon-elem-stars");
        iconMap.put("mesh", "icon-elem-meshes");
        iconMap.put("texture-pack", "icon-elem-moons");
    }

    private static String getIcon(String type) {
        if (type != null && iconMap.containsKey(type))
            return iconMap.get(type);
        return "icon-elements-other";
    }

    private DataDescriptor dd;
    private OwnTextButton downloadButton;
    private OwnProgressBar downloadProgress;
    private OwnLabel currentDownloadFile, downloadSpeed;
    private OwnScrollPane datasetsScroll;
    private Cell<OwnTextButton> cancelCell;
    private float scrollX = 0f, scrollY = 0f;

    private Color highlight;

    // Whether to show the data location chooser
    private boolean dataLocation;

    private INumberFormat nf;
    private List<Trio<DatasetDesc, OwnCheckBox, OwnLabel>> choiceList;
    private Array<Trio<DatasetDesc, OwnCheckBox, OwnLabel>> toDownload;
    private Array<OwnImageButton> rubbishes;
    private int current = -1;

    public DownloadDataWindow(Stage stage, Skin skin, DataDescriptor dd) {
        this(stage, skin, dd, true, I18n.txt("gui.ok"));
    }

    public DownloadDataWindow(Stage stage, Skin skin, DataDescriptor dd, boolean dataLocation, String acceptText) {
        super(I18n.txt("gui.download.title") + (dd.updatesAvailable ? " - " + I18n.txt("gui.download.updates", dd.numUpdates) : ""), skin, stage);
        this.nf = NumberFormatFactory.getFormatter("##0.0");
        this.dd = dd;
        this.choiceList = new LinkedList<>();
        this.rubbishes = new Array<>();
        this.highlight = ColorUtils.gYellowC;

        this.dataLocation = dataLocation;

        setAcceptText(acceptText);

        // Build
        buildSuper();
    }

    @Override
    protected void build() {
        me.acceptButton.setDisabled(false);
        float pad = 2f * GlobalConf.UI_SCALE_FACTOR;
        float padLarge = 9f * GlobalConf.UI_SCALE_FACTOR;
        float minW = !GlobalConf.isHiDPI() ? 570f : 650f;

        float buttonPad = 1f * GlobalConf.UI_SCALE_FACTOR;
        Cell<Actor> topCell = content.add((Actor) null).left().top();
        topCell.row();

        // Offer downloads
        Table downloadTable = new Table(skin);
        downloadTable.setFillParent(true);

        OwnLabel catalogsLocLabel = new OwnLabel(I18n.txt("gui.download.location") + ":", skin);

        HorizontalGroup hg = new HorizontalGroup();
        hg.space(15f * GlobalConf.UI_SCALE_FACTOR);
        Image system = new Image(skin.getDrawable("tooltip-icon"));
        OwnLabel downloadInfo = new OwnLabel(I18n.txt("gui.download.info"), skin);
        hg.addActor(system);
        hg.addActor(downloadInfo);

        downloadTable.add(hg).left().colspan(2).padBottom(padLarge).row();

        try {
            Files.createDirectories(SysUtils.getLocalDataDir());
        } catch (FileAlreadyExistsException e) {
            // Good
        } catch (IOException e) {
            logger.error(e);
            return;
        }
        String catLoc = GlobalConf.data.DATA_LOCATION;

        if (dataLocation) {
            OwnTextButton catalogsLoc = new OwnTextButton(catLoc, skin);
            catalogsLoc.pad(buttonPad * 4f);
            catalogsLoc.setMinWidth(minW);
            downloadTable.add(catalogsLocLabel).left().padBottom(padLarge);
            downloadTable.add(catalogsLoc).left().padLeft(pad).padBottom(padLarge).row();
            Cell<Actor> notice = downloadTable.add((Actor) null).colspan(2).padBottom(padLarge);
            notice.row();

            catalogsLoc.addListener((event) -> {
                if (event instanceof ChangeEvent) {
                    FileChooser fc = new FileChooser(I18n.txt("gui.download.pickloc"), skin, stage, Path.of(GlobalConf.data.DATA_LOCATION), FileChooser.FileChooserTarget.DIRECTORIES);
                    fc.setResultListener((success, result) -> {
                        if (success) {
                            if (Files.isReadable(result) && Files.isWritable(result)) {
                                // do stuff with result
                                catalogsLoc.setText(result.toAbsolutePath().toString());
                                GlobalConf.data.DATA_LOCATION = result.toAbsolutePath().toString().replaceAll("\\\\", "/");
                                me.pack();
                                GaiaSky.postRunnable(() -> {
                                    // Reset datasets
                                    GlobalConf.data.CATALOG_JSON_FILES.clear();
                                    reloadAll();
                                });
                            } else {
                                Label warn = new OwnLabel(I18n.txt("gui.download.pickloc.permissions"), skin);
                                warn.setColor(ColorUtils.gRedC);
                                notice.setActor(warn);
                                return false;
                            }
                        }
                        notice.clearActor();
                        return true;
                    });
                    fc.show(stage);

                    return true;
                }
                return false;
            });
        }

        // Uploads available?
        if (dd.updatesAvailable) {
            OwnLabel updates = new OwnLabel(I18n.txt("gui.download.updates", dd.numUpdates), skin, "hud-big");
            updates.setColor(highlight);
            downloadTable.add(updates).colspan(2).center().padBottom(padLarge).row();
        }

        // Build datasets table
        Table datasetsTable = new Table(skin);

        for (DatasetType type : dd.types) {
            List<DatasetDesc> datasets = type.datasets;
            OwnLabel dsType = new OwnLabel(I18n.txt("gui.download.type." + type.typeStr), skin, "hud-header");
            OwnImageButton expandIcon = new OwnImageButton(skin, "expand-collapse");
            expandIcon.setChecked(true);
            HorizontalGroup titleGroup = new HorizontalGroup();
            titleGroup.space(padLarge);
            titleGroup.addActor(expandIcon);
            titleGroup.addActor(dsType);
            datasetsTable.add(titleGroup).left().padBottom(pad * 3f).padTop(padLarge * 2f).row();

            Table t = new Table(skin);
            Cell c = datasetsTable.add(t).colspan(2).left();
            c.row();

            expandIcon.addListener((event) -> {
                if (event instanceof ChangeEvent) {
                    if (c.getActor() == null) {
                        c.setActor(t);
                    } else {
                        c.setActor(null);
                    }
                    datasetsTable.pack();
                    me.pack();
                    return true;
                }
                return false;
            });

            boolean haveAll = true;
            for (DatasetDesc dataset : datasets) {
                haveAll = haveAll && dataset.exists && !dataset.outdated;
                // Check if dataset requires a minimum version of Gaia Sky

                // Add dataset to desc table
                OwnCheckBox cb = new OwnCheckBox(dataset.name, skin, "title", pad * 2f);
                cb.left();
                cb.setMinWidth(!GlobalConf.isHiDPI() ? 240f * GlobalConf.UI_SCALE_FACTOR : 200f * GlobalConf.UI_SCALE_FACTOR);
                cb.setChecked(dataset.mustDownload);
                cb.setDisabled(dataset.cbDisabled);
                cb.addListener((event) -> {
                    if (event instanceof ChangeEvent) {
                        updateDatasetsSelected();
                        return true;
                    }
                    return false;
                });
                OwnLabel haveit = new OwnLabel("", skin);
                haveit.setWidth(65f * GlobalConf.UI_SCALE_FACTOR);
                if (dataset.exists) {
                    if (dataset.outdated) {
                        setStatusOutdated(dataset, haveit);
                    } else {
                        setStatusFound(dataset, haveit);
                    }
                } else {
                    setStatusNotFound(dataset, haveit);
                }

                // Can't proceed without base data - force download
                if (dataset.baseData && !dataset.exists) {
                    me.acceptButton.setDisabled(true);
                }

                // Description
                HorizontalGroup descGroup = new HorizontalGroup();
                descGroup.space(padLarge);
                OwnLabel desc = new OwnLabel(dataset.shortDescription, skin);
                desc.setWidth(!GlobalConf.isHiDPI() ? 250f * GlobalConf.UI_SCALE_FACTOR : 210f * GlobalConf.UI_SCALE_FACTOR);
                // Info
                OwnImageButton imgTooltip = new OwnImageButton(skin, "tooltip");
                imgTooltip.addListener(new OwnTextTooltip(dataset.description, skin, 10));
                descGroup.addActor(imgTooltip);
                descGroup.addActor(desc);
                // Link
                if (dataset.link != null) {
                    LinkButton imgLink = new LinkButton(dataset.link, skin);
                    descGroup.addActor(imgLink);
                } else {
                    Image emptyImg = new Image(skin, "iconic-empty");
                    descGroup.addActor(emptyImg);

                }

                // Version
                String vstring;
                if (!dataset.exists) {
                    vstring = "-";
                } else if (dataset.outdated) {
                    vstring = dataset.myVersion + " -> v-" + dataset.serverVersion;
                } else {
                    vstring = "v-" + dataset.myVersion;
                }

                OwnLabel vers = new OwnLabel(vstring, skin);
                vers.setWidth(80f * GlobalConf.UI_SCALE_FACTOR);
                if (!dataset.exists) {
                    vers.addListener(new OwnTextTooltip(I18n.txt("gui.download.version.server", Integer.toString(dataset.serverVersion)), skin, 10));
                } else if (dataset.outdated) {
                    // New version!
                    vers.setColor(highlight);
                    vers.addListener(new OwnTextTooltip(I18n.txt("gui.download.version.new", Integer.toString(dataset.serverVersion), Integer.toString(dataset.myVersion)), skin, 10));
                } else {
                    vers.addListener(new OwnTextTooltip(I18n.txt("gui.download.version.ok"), skin, 10));
                }

                // Type icon
                Image typeImage = new OwnImage(skin.getDrawable(getIcon(dataset.type)));
                float scl = 0.7f;
                float iw = typeImage.getWidth();
                float ih = typeImage.getHeight();
                typeImage.setSize(iw * scl, ih * scl);
                typeImage.addListener(new OwnTextTooltip(dataset.type, skin, 10));

                // Size
                OwnLabel size = new OwnLabel(dataset.size, skin);
                size.setWidth(85f * GlobalConf.UI_SCALE_FACTOR);

                // Delete
                OwnImageButton rubbish = null;
                if (dataset.exists) {
                    rubbish = new OwnImageButton(skin, "rubbish-bin");
                    rubbish.addListener(new OwnTextTooltip(I18n.txt("gui.tooltip.dataset.remove"), skin));
                    rubbish.addListener((event) -> {
                        if (event instanceof ChangeEvent) {
                            // Remove dataset
                            if (dataset.filesToDelete != null) {
                                for (String fileToDelete : dataset.filesToDelete) {
                                    try {
                                        if (fileToDelete.endsWith("/")) {
                                            fileToDelete = fileToDelete.substring(0, fileToDelete.length() - 1);
                                        }
                                        // Expand possible wildcards
                                        String basePath = "";
                                        String baseName = fileToDelete;
                                        if (fileToDelete.contains("/")) {
                                            basePath = fileToDelete.substring(0, fileToDelete.lastIndexOf('/'));
                                            baseName = fileToDelete.substring(fileToDelete.lastIndexOf('/') + 1);
                                        }
                                        File dataLoc = new File(GlobalConf.data.DATA_LOCATION);
                                        File directory = new File(dataLoc, basePath);
                                        Collection<File> files = FileUtils.listFilesAndDirs(directory, new WildcardFileFilter(baseName), new WildcardFileFilter(baseName));
                                        for (File file : files) {
                                            if (!file.equals(directory) && file.exists()) {
                                                FileUtils.forceDelete(file);
                                            }
                                        }
                                    } catch (Exception e) {
                                        logger.error(e);
                                    }
                                }
                            } else {
                                // Only remove "check"
                                try {
                                    FileUtils.forceDelete(dataset.check.toFile());
                                } catch (IOException e) {
                                    logger.error(e);
                                }
                            }
                            // RELOAD DATASETS VIEW
                            GaiaSky.postRunnable(() -> {
                                reloadAll();
                            });

                            return true;
                        }
                        return false;
                    });
                    rubbishes.add(rubbish);
                }
                t.add(cb).left().padRight(padLarge).padBottom(pad);
                t.add(descGroup).left().padRight(padLarge).padBottom(pad);
                t.add(vers).center().padRight(padLarge).padBottom(pad);
                t.add(typeImage).center().padRight(padLarge).padBottom(pad);
                t.add(size).left().padRight(padLarge).padBottom(pad);
                t.add(haveit).center().padBottom(pad);
                if (dataset.exists) {
                    t.add(rubbish).center().padLeft(padLarge * 2.5f);
                }
                t.row();

                choiceList.add(new Trio<>(dataset, cb, haveit));
            }
            expandIcon.setChecked(!haveAll);

        }

        datasetsTable.align(Align.top | Align.center);
        datasetsScroll = new OwnScrollPane(datasetsTable, skin, "minimalist-nobg");
        datasetsScroll.setScrollingDisabled(true, false);
        datasetsScroll.setForceScroll(false, false);
        datasetsScroll.setSmoothScrolling(false);
        datasetsScroll.setFadeScrollBars(false);
        datasetsScroll.setHeight(Math.min(Gdx.graphics.getHeight() * 0.5f, 760f * GlobalConf.UI_SCALE_FACTOR));
        datasetsScroll.setWidth(Math.min(Gdx.graphics.getWidth() * 0.9f, GlobalConf.isHiDPI() ? 800f * GlobalConf.UI_SCALE_FACTOR : 900f * GlobalConf.UI_SCALE_FACTOR));

        downloadTable.add(datasetsScroll).top().center().padBottom(padLarge).colspan(2).row();

        // Current dataset info
        currentDownloadFile = new OwnLabel("", skin, "hotkey");
        downloadTable.add(currentDownloadFile).center().colspan(2).padBottom(padLarge).row();

        // Download button
        downloadButton = new OwnTextButton(I18n.txt("gui.download.download"), skin, "download");
        downloadButton.pad(buttonPad * 4f);
        downloadButton.setMinWidth(minW);
        downloadButton.setMinHeight(50f * GlobalConf.UI_SCALE_FACTOR);
        downloadTable.add(downloadButton).center().colspan(2).padBottom(0f).row();

        // Progress bar
        downloadProgress = new OwnProgressBar(0, 100, 0.1f, false, skin, "default-horizontal");
        downloadProgress.setValue(0);
        downloadProgress.setVisible(false);
        downloadProgress.setPrefWidth(minW);
        downloadTable.add(downloadProgress).center().colspan(2).padBottom(padLarge).row();

        // Download info and cancel
        Table infoCancel = new Table(skin);
        downloadSpeed = new OwnLabel("", skin);
        downloadSpeed.setVisible(false);
        infoCancel.add(downloadSpeed).padRight(padLarge * 2f);
        cancelCell = infoCancel.add();
        downloadTable.add(infoCancel).colspan(2).padBottom(padLarge).row();

        downloadButton.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                downloadAndExtractFiles(choiceList);
            }
            return true;
        });

        // Progress

        // External download link
        Link manualDownload = new Link("Manual download", skin, "link", "http://gaia.ari.uni-heidelberg.de/gaiasky/files/autodownload");
        downloadTable.add(manualDownload).center().colspan(2);

        topCell.setActor(downloadTable);

        // Update selected
        updateDatasetsSelected();
    }

    private void updateDatasetsSelected() {
        int nSelected = 0;
        for (Trio<DatasetDesc, OwnCheckBox, OwnLabel> choice : choiceList) {
            if (choice.getSecond().isChecked()) {
                nSelected++;
            }
        }

        if (nSelected <= 0) {
            currentDownloadFile.setText(I18n.txt("gui.download.selected.none"));
            downloadButton.setDisabled(true);
        } else if (nSelected == 1) {
            currentDownloadFile.setText(I18n.txt("gui.download.selected.one"));
            downloadButton.setDisabled(false);
        } else {
            currentDownloadFile.setText(I18n.txt("gui.download.selected.more", nSelected));
            downloadButton.setDisabled(false);
        }
    }

    private synchronized void downloadAndExtractFiles(List<Trio<DatasetDesc, OwnCheckBox, OwnLabel>> choices) {
        toDownload = new Array<>();

        for (Trio<DatasetDesc, OwnCheckBox, OwnLabel> entry : choices) {
            if (entry.getSecond().isChecked())
                toDownload.add(entry);
        }

        // Disable all checkboxes
        setDisabled(choices, true);
        // Disable all rubbishes
        setDisabled(rubbishes, true);

        logger.info(toDownload.size + " new data files selected to download");

        current = -1;
        downloadNext();

    }

    private void downloadNext() {
        current++;
        downloadCurrent();
    }

    private void downloadCurrent() {
        if (current >= 0 && current < toDownload.size) {
            // Download next
            Trio<DatasetDesc, OwnCheckBox, OwnLabel> trio = toDownload.get(current);
            DatasetDesc currentDataset = trio.getFirst();
            String name = currentDataset.name;
            String url = currentDataset.file.replace("@mirror-url@", GlobalConf.program.DATA_MIRROR_URL);
            String type = currentDataset.type;

            FileHandle tempDownload = Gdx.files.absolute(GlobalConf.data.DATA_LOCATION + "/temp.tar.gz");

            ProgressRunnable pr = (read, total, progress, speed) -> {
                double readMb = (double) read / 1e6d;
                double totalMb = (double) total / 1e6d;
                final String progressString = progress >= 100 ? I18n.txt("gui.done") : I18n.txt("gui.download.downloading", nf.format(progress));
                double mbPerSecond = speed / 1000d;
                final String speedString = nf.format(readMb) + "/" + nf.format(totalMb) + " MB   -   " + nf.format(mbPerSecond) + " MB/s";
                // Since we are downloading on a background thread, post a runnable to touch UI
                GaiaSky.postRunnable(() -> {
                    downloadButton.setText(progressString);
                    downloadProgress.setValue((float) progress);
                    downloadSpeed.setText(speedString);
                });
            };

            ChecksumRunnable finish = (digest) -> {
                // Unpack
                int errors = 0;
                logger.info("Extracting: " + tempDownload.path());
                String dataLocation = GlobalConf.data.DATA_LOCATION + File.separatorChar;
                // Checksum
                if (digest != null && currentDataset.sha256 != null) {
                    String serverDigest = currentDataset.sha256;
                    try {
                        boolean ok = serverDigest.equals(digest);
                        if (ok) {
                            logger.info("SHA256 ok: " + name);
                        } else {
                            logger.error("SHA256 check failed: " + name);
                            errors++;
                        }
                    } catch (Exception e) {
                        logger.info("Error checking SHA256: " + name);
                        errors++;
                    }
                } else {
                    logger.info("No digest found for dataset: " + name);
                }

                if (errors == 0)
                    try {
                        // Extract
                        decompress(tempDownload.path(), new File(dataLocation), downloadButton, downloadProgress);
                        // Remove archive
                        cleanupTempFiles();
                    } catch (Exception e) {
                        logger.error(e, "Error decompressing: " + name);
                        errors++;
                    }

                // Done
                GaiaSky.postRunnable(() -> {
                    downloadButton.setText(I18n.txt("gui.download.download"));
                    downloadProgress.setValue(0);
                    downloadProgress.setVisible(false);
                    downloadSpeed.setText("");
                    downloadSpeed.setVisible(false);
                    cancelCell.setActor(null);
                    me.acceptButton.setDisabled(false);
                    // Enable all
                    setDisabled(choiceList, false);
                    // Enable rubbishes
                    setDisabled(rubbishes, false);
                });

                if (errors == 0) {
                    // Select dataset if needed
                    if (type.startsWith("catalog-")) {
                        // Descriptor file
                        GlobalConf.data.CATALOG_JSON_FILES.add(currentDataset.check.toString());
                    }

                    setMessageOk(I18n.txt("gui.download.idle"));
                    setStatusFound(currentDataset, trio.getThird());

                    GaiaSky.postRunnable(() -> downloadNext());
                } else {
                    logger.info("Error getting dataset: " + name);
                    setStatusError(currentDataset, trio.getThird());
                    setMessageError(I18n.txt("gui.download.failed", name));
                }

            };

            Runnable fail = () -> {
                logger.error("Download failed: " + name);
                setStatusError(currentDataset, trio.getThird());
                setMessageError(I18n.txt("gui.download.failed", name));
                me.acceptButton.setDisabled(false);
                downloadProgress.setVisible(false);
                downloadSpeed.setVisible(false);
                cancelCell.setActor(null);
                GaiaSky.postRunnable(() -> downloadNext());
            };

            Runnable cancel = () -> {
                logger.error(I18n.txt("gui.download.cancelled", name));
                setStatusCancelled(currentDataset, trio.getThird());
                setMessageError(I18n.txt("gui.download.cancelled", name));
                me.acceptButton.setDisabled(false);
                downloadProgress.setVisible(false);
                downloadSpeed.setVisible(false);
                cancelCell.setActor(null);
                GaiaSky.postRunnable(() -> downloadNext());
            };

            // Download
            downloadButton.setDisabled(true);
            me.acceptButton.setDisabled(true);
            downloadProgress.setVisible(true);
            downloadSpeed.setVisible(true);
            setStatusProgress(trio.getThird());
            setMessageOk(I18n.txt("gui.download.downloading.info", (current + 1), toDownload.size, currentDataset.name));
            final Net.HttpRequest request = DownloadHelper.downloadFile(url, tempDownload, pr, finish, fail, cancel);

            // Cancel button
            OwnTextButton cancelDownloadButton = new OwnTextButton(I18n.txt("gui.download.cancel"), skin);
            cancelDownloadButton.pad(9f * GlobalConf.UI_SCALE_FACTOR);
            cancelDownloadButton.getLabel().setColor(1, 0, 0, 1);
            cancelDownloadButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (request != null) {
                        GaiaSky.postRunnable(() -> Gdx.net.cancelHttpRequest(request));
                    }
                }
            });
            cancelCell.setActor(cancelDownloadButton);
        } else {
            // Finished all downloads!
            // RELOAD DATASETS VIEW
            GaiaSky.postRunnable(() -> reloadAll());
        }

    }

    private void setDisabled(Array<OwnImageButton> l, boolean disabled) {
        for (OwnImageButton b : l)
            b.setDisabled(disabled);
    }

    private void setDisabled(List<Trio<DatasetDesc, OwnCheckBox, OwnLabel>> choices, boolean disabled) {
        for (Trio<DatasetDesc, OwnCheckBox, OwnLabel> t : choices) {
            // Uncheck all if enabling again
            if (!disabled)
                t.getSecond().setChecked(false);
            // Only enable datasets which we don't have
            if (disabled || (!t.getThird().getText().toString().equals(I18n.txt("gui.download.status.found"))))
                t.getSecond().setDisabled(disabled);
        }
    }

    private void decompress(String in, File out, OwnTextButton b, ProgressBar p) throws Exception {
        FileInfoInputStream fin = new FileInfoInputStream(in);
        GzipCompressorInputStream gzin = new GzipCompressorInputStream(fin);
        TarArchiveInputStream tarin = new TarArchiveInputStream(gzin);
        double sizeKb = fileSize(in) / 1000d;
        String sizeKbStr = nf.format(sizeKb);
        TarArchiveEntry entry;
        long last = 0;
        while ((entry = tarin.getNextTarEntry()) != null) {
            if (entry.isDirectory()) {
                continue;
            }
            File curFile = new File(out, entry.getName());
            File parent = curFile.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }

            IOUtils.copy(tarin, new FileOutputStream(curFile));

            // Every 250 ms we update the view
            long current = System.currentTimeMillis();
            long elapsed = current - last;
            if (elapsed > 250) {
                GaiaSky.postRunnable(() -> {
                    float val = (float) ((fin.getBytesRead() / 1000d) / sizeKb);
                    b.setText(I18n.txt("gui.download.extracting", nf.format(fin.getBytesRead() / 1000d) + "/" + sizeKbStr + " Kb"));
                    p.setValue(val * 100);
                });
                last = current;
            }

        }
    }

    /**
     * Returns the file size
     *
     * @param inputFilePath A file
     * @return The size in bytes
     */
    private long fileSize(String inputFilePath) {
        return new File(inputFilePath).length();
    }

    /**
     * Returns the GZ uncompressed size
     *
     * @param inputFilePath A gzipped file
     * @return The uncompressed size in bytes
     * @throws Exception
     */
    private long fileSizeGZUncompressed(String inputFilePath) throws Exception {
        RandomAccessFile raf = new RandomAccessFile(inputFilePath, "r");
        raf.seek(raf.length() - 4);
        byte[] bytes = new byte[4];
        raf.read(bytes);
        long fileSize = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder()).getLong();
        if (fileSize < 0)
            fileSize += (1L << 32);
        raf.close();
        return fileSize;
    }

    private void cleanupTempFiles() {
        Path tempDownload = Paths.get(GlobalConf.data.DATA_LOCATION, "temp.tar.gz");
        Path gsDownload = Paths.get(GlobalConf.data.DATA_LOCATION, "gaiasky_data.tar.gz");

        deleteFile(tempDownload);
        deleteFile(gsDownload);
    }

    private void deleteFile(Path p) {
        if (Files.exists(p)) {
            try {
                Files.delete(p);
            } catch (IOException e) {
                logger.error(e, "Failed cleaning up file: " + p.toString());
            }
        }

    }

    private void setStatusOutdated(DatasetDesc ds, OwnLabel label) {
        label.setText(I18n.txt("gui.download.status.outdated"));
        label.setColor(ColorUtils.gYellowC);
        if (ds.releaseNotes != null && !ds.releaseNotes.isEmpty()) {
            label.setText(label.getText());
            label.addListener(new OwnTextTooltip(I18n.txt("gui.download.releasenotes", ds.releaseNotes), skin, 10));
        }
    }

    private void setStatusFound(DatasetDesc ds, OwnLabel label) {
        label.setText(I18n.txt("gui.download.status.found"));
        label.setColor(ColorUtils.gGreenC);
    }

    private void setStatusNotFound(DatasetDesc ds, OwnLabel label) {
        label.setText(I18n.txt("gui.download.status.notfound"));
        label.setColor(ColorUtils.gRedC);
    }

    private void setStatusError(DatasetDesc ds, OwnLabel label) {
        label.setText("Failed");
        label.setText(I18n.txt("gui.download.status.failed"));
        label.setColor(ColorUtils.gRedC);
    }

    private void setStatusCancelled(DatasetDesc ds, OwnLabel label) {
        label.setText(I18n.txt("gui.download.status.cancelled"));
        label.setColor(ColorUtils.gRedC);
    }

    private void setStatusProgress(OwnLabel label) {
        label.setText(I18n.txt("gui.download.status.working"));
        label.setColor(ColorUtils.gBlueC);
    }

    private void setMessageOk(String text) {
        currentDownloadFile.setText(text);
        currentDownloadFile.setColor(currentDownloadFile.getStyle().fontColor);
    }

    private void setMessageError(String text) {
        currentDownloadFile.setText(text);
        currentDownloadFile.setColor(ColorUtils.gRedC);
    }

    @Override
    protected void accept() {
        cleanupTempFiles();
    }

    @Override
    protected void cancel() {
        cleanupTempFiles();
    }

    private void backupScrollValues() {
        if (datasetsScroll != null) {
            this.scrollY = datasetsScroll.getScrollY();
            this.scrollX = datasetsScroll.getScrollX();
        }
    }

    private void restoreScrollValues() {
        if (datasetsScroll != null) {
            GaiaSky.postRunnable(() -> {
                datasetsScroll.setScrollX(scrollX);
                datasetsScroll.setScrollY(scrollY);
            });

        }
    }

    /**
     * Drops the current view and regenerates all window content
     */
    private void reloadAll() {
        if (choiceList != null)
            choiceList.clear();
        if (toDownload != null)
            toDownload.clear();
        dd = DataDescriptorUtils.instance().buildDatasetsDescriptor(null);
        backupScrollValues();
        content.clear();
        build();
        pack();
        restoreScrollValues();
    }

}
