package gaia.cu9.ari.gaiaorbit.interfce;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.python.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.python.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.python.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.python.apache.commons.compress.utils.IOUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Cursor.SystemCursor;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import gaia.cu9.ari.gaiaorbit.desktop.util.SysUtils;
import gaia.cu9.ari.gaiaorbit.util.ChecksumRunnable;
import gaia.cu9.ari.gaiaorbit.util.DownloadHelper;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.ProgressRunnable;
import gaia.cu9.ari.gaiaorbit.util.Trio;
import gaia.cu9.ari.gaiaorbit.util.format.INumberFormat;
import gaia.cu9.ari.gaiaorbit.util.format.NumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.scene2d.FileChooser;
import gaia.cu9.ari.gaiaorbit.util.scene2d.FileChooser.ResultListener;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnCheckBox;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnImageButton;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnLabel;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnScrollPane;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnTextButton;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnTextTooltip;

public class DownloadDataWindow extends GenericDialog {
    private static final Log logger = Logger.getLogger(DownloadDataWindow.class);

    private static final Map<String, String> iconMap;
    static {
        iconMap = new HashMap<String, String>();
        iconMap.put("other", "icon-elem-others");
        iconMap.put("data-pack", "icon-elem-others");
        iconMap.put("catalog-lod", "icon-elem-stars");
        iconMap.put("catalog", "icon-elem-stars");
        iconMap.put("mesh", "icon-elem-meshes");
        iconMap.put("texture-pack", "icon-elem-moons");
    }

    private static String getIcon(String type) {
        if (type != null && iconMap.containsKey(type))
            return iconMap.get(type);
        return "icon-elements-other";
    }

    private OwnTextButton downloadButton;
    private OwnLabel currentDownloadFile;

    private INumberFormat nf;
    private JsonReader reader;
    private List<Trio<JsonValue, OwnCheckBox, OwnLabel>> choiceList;
    private Array<Trio<JsonValue, OwnCheckBox, OwnLabel>> toDownload;
    private int current = -1;

    public DownloadDataWindow(Stage stage, Skin skin) {
        super(txt("gui.download.title"), skin, stage);
        this.nf = NumberFormatFactory.getFormatter("##0.0");
        this.reader = new JsonReader();
        this.choiceList = new LinkedList<Trio<JsonValue, OwnCheckBox, OwnLabel>>();

        setCancelText(txt("gui.exit"));
        setAcceptText(txt("gui.start"));

        // Build
        buildSuper();
    }

    @Override
    protected void build() {
        float pad = 2 * GlobalConf.SCALE_FACTOR;
        float padl = 9 * GlobalConf.SCALE_FACTOR;

        float buttonpad = 1 * GlobalConf.SCALE_FACTOR;

        Cell<Actor> topCell = content.add((Actor) null);
        topCell.row();

        // Offer downloads
        Table downloadTable = new Table(skin);

        OwnLabel catalogsLocLabel = new OwnLabel(txt("gui.download.location") + ":", skin);

        HorizontalGroup hg = new HorizontalGroup();
        hg.space(15 * GlobalConf.SCALE_FACTOR);
        Image system = new Image(skin.getDrawable("tooltip-icon"));
        OwnLabel downloadInfo = new OwnLabel(txt("gui.download.info"), skin);
        hg.addActor(system);
        hg.addActor(downloadInfo);

        downloadTable.add(hg).left().colspan(2).padBottom(padl).row();
        downloadTable.add(catalogsLocLabel).left().padBottom(padl);

        SysUtils.getDefaultDataDir().mkdirs();
        String catLoc = GlobalConf.data.DATA_LOCATION;
        OwnTextButton catalogsLoc = new OwnTextButton(catLoc, skin);
        catalogsLoc.pad(buttonpad * 4);
        catalogsLoc.setMinWidth(GlobalConf.SCALE_FACTOR == 1 ? 450 : 650);
        downloadTable.add(catalogsLoc).left().padLeft(pad).padBottom(padl).row();
        Cell<Actor> notice = downloadTable.add((Actor) null).colspan(2).padBottom(padl);
        notice.row();

        // Parse available files
        JsonValue dataDesc = reader.parse(Gdx.files.absolute(catLoc + "/gaiasky-data.json"));

        Table datasetsTable = new Table(skin);
        datasetsTable.add(new OwnLabel("To download", skin, "header")).left().padRight(padl).padBottom(pad);
        datasetsTable.add(new OwnLabel("Description", skin, "header")).left().padRight(padl).padBottom(pad);
        datasetsTable.add(new OwnLabel("Type", skin, "header")).left().padRight(padl).padBottom(pad);
        datasetsTable.add(new OwnLabel("Size", skin, "header")).left().padRight(padl).padBottom(pad);
        datasetsTable.add(new OwnLabel("Status", skin, "header")).center().padRight(padl).padBottom(pad).row();

        JsonValue dataset = dataDesc.child().child();
        while (dataset != null) {
            // Check if we have it
            Path check = Paths.get(GlobalConf.data.DATA_LOCATION, dataset.getString("check"));
            boolean exists = Files.exists(check) && Files.isReadable(check);

            String name = dataset.getString("name");
            // Add dataset to desc table
            OwnCheckBox cb = new OwnCheckBox(name, skin, pad);
            boolean baseData = name.equals("default-data");
            boolean defaultDataset = name.contains("default");
            cb.setChecked(!exists && (baseData || defaultDataset));
            cb.setDisabled(baseData || exists);
            OwnLabel haveit = new OwnLabel(exists ? "Found" : "Not found", skin);
            if (exists) {
                haveit.setColor(0, 1, 0, 1);
            } else {
                haveit.setColor(1, 0, 0, 1);
            }

            // Can't proceed without base data - force download
            if (baseData && !exists) {
                me.acceptButton.setDisabled(true);
            }

            String description = dataset.getString("description");
            String shortDescription;
            HorizontalGroup descGroup = new HorizontalGroup();
            descGroup.space(padl);
            if (description.contains("-")) {
                shortDescription = description.substring(0, description.indexOf("-"));
            } else {
                shortDescription = description;
            }
            OwnLabel desc = new OwnLabel(shortDescription, skin);
            // Info
            OwnImageButton imgTooltip = new OwnImageButton(skin, "tooltip");
            imgTooltip.addListener(new OwnTextTooltip(description, skin, 10));
            descGroup.addActor(imgTooltip);
            descGroup.addActor(desc);

            // Type icon
            String typeimg = getIcon(dataset.getString("type"));
            Image type = new Image(skin.getDrawable(typeimg));
            type.addListener(new OwnTextTooltip(dataset.getString("type"), skin, 10));

            // Size
            String size = "";
            try {
                long bytes = dataset.getLong("size");
                size = GlobalResources.humanReadableByteCount(bytes, true);
            } catch (IllegalArgumentException e) {
                size = "?";
            }

            datasetsTable.add(cb).left().padRight(padl).padBottom(pad);
            datasetsTable.add(descGroup).left().padRight(padl).padBottom(pad);
            datasetsTable.add(type).left().padRight(padl).padBottom(pad);
            datasetsTable.add(size).left().padRight(padl).padBottom(pad);
            datasetsTable.add(haveit).center().padBottom(pad);

            datasetsTable.row();

            choiceList.add(new Trio<JsonValue, OwnCheckBox, OwnLabel>(dataset, cb, haveit));

            dataset = dataset.next();

        }

        OwnScrollPane datasetsScroll = new OwnScrollPane(datasetsTable, skin, "minimalist-nobg");
        datasetsScroll.setScrollingDisabled(true, false);
        datasetsScroll.setForceScroll(false, false);
        datasetsScroll.setSmoothScrolling(true);
        datasetsScroll.setFadeScrollBars(false);
        datasetsScroll.setHeight(Math.min(Gdx.graphics.getHeight() * 0.38f, 350 * GlobalConf.SCALE_FACTOR));
        datasetsScroll.setWidth(550 * GlobalConf.SCALE_FACTOR);

        downloadTable.add(datasetsScroll).center().padBottom(padl).colspan(2).row();

        downloadButton = new OwnTextButton(txt("gui.download.download").toUpperCase(), skin, "download");
        downloadButton.pad(buttonpad * 4);
        downloadButton.setMinWidth(catalogsLoc.getWidth());
        downloadButton.setMinHeight(50 * GlobalConf.SCALE_FACTOR);
        downloadTable.add(downloadButton).center().colspan(2).padBottom(padl).row();

        downloadButton.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                downloadAndExtractFiles(choiceList);
            }
            return true;
        });

        currentDownloadFile = new OwnLabel("", skin);
        downloadTable.add(currentDownloadFile).center().colspan(2);

        catalogsLoc.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                FileChooser fc = FileChooser.createPickDialog(txt("gui.download.pickloc"), skin, Gdx.files.absolute(GlobalConf.data.DATA_LOCATION));
                fc.setResultListener(new ResultListener() {
                    @Override
                    public boolean result(boolean success, FileHandle result) {
                        if (success) {
                            if (result.file().canRead() && result.file().canWrite()) {
                                // do stuff with result
                                catalogsLoc.setText(result.path());
                                GlobalConf.data.DATA_LOCATION = result.path();
                                me.pack();
                            } else {
                                Label warn = new OwnLabel(txt("gui.download.pickloc.permissions"), skin);
                                warn.setColor(1f, .4f, .4f, 1f);
                                notice.setActor(warn);
                                return false;
                            }
                        }
                        notice.clearActor();
                        return true;
                    }
                });
                fc.setFilter(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return pathname.isDirectory();
                    }
                });
                fc.show(stage);

                return true;
            }
            return false;
        });

        topCell.setActor(downloadTable);

    }

    private synchronized void downloadAndExtractFiles(List<Trio<JsonValue, OwnCheckBox, OwnLabel>> choices) {
        toDownload = new Array<Trio<JsonValue, OwnCheckBox, OwnLabel>>();

        for (Trio<JsonValue, OwnCheckBox, OwnLabel> entry : choices) {
            if (entry.getSecond().isChecked())
                toDownload.add(entry);

            // Disable all
            entry.getSecond().setDisabled(true);
        }

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
            Trio<JsonValue, OwnCheckBox, OwnLabel> trio = toDownload.get(current);
            JsonValue currentJson = trio.getFirst();
            String name = currentJson.getString("name");
            String url = currentJson.getString("file");
            String type = currentJson.getString("type");

            FileHandle tempDownload = Gdx.files.absolute(GlobalConf.data.DATA_LOCATION + "/temp.tar.gz");

            ProgressRunnable pr = (progress) -> {
                final String progressString = progress >= 100 ? txt("gui.done") : txt("gui.download.downloading", nf.format(progress));

                // Since we are downloading on a background thread, post a runnable to touch UI
                Gdx.app.postRunnable(() -> {
                    if (progress == 100) {
                        downloadButton.setDisabled(true);
                    }
                    downloadButton.setText(progressString);
                });
            };

            ChecksumRunnable finish = (md5sum) -> {
                // Unpack
                int errors = 0;
                logger.info("Extracting: " + tempDownload.path());
                String dataLocation = GlobalConf.data.DATA_LOCATION + File.separatorChar;
                // Checksum
                if (currentJson.has("md5")) {
                    String serverMd5 = currentJson.getString("md5");
                    try {
                        boolean ok = serverMd5.equals(md5sum);
                        if (ok) {
                            logger.info("MD5 checksum ok: " + name);
                        } else {
                            logger.error("MD5 checkum failed: " + name);
                            errors++;
                        }
                    } catch (Exception e) {
                        logger.info("Error checking MD5: " + name);
                        errors++;
                    }
                } else {
                    logger.info("No checksum found for dataset: " + name);
                }

                if (errors == 0)
                    try {
                        decompress(tempDownload.path(), new File(dataLocation), downloadButton);
                        // Remove archive
                        cleanupTempFiles();
                    } catch (Exception e) {
                        logger.error(e, "Error decompressing: " + name);
                        errors++;
                    }

                if (errors == 0) {
                    // Done
                    Gdx.app.postRunnable(() -> {
                        downloadButton.setText(txt("gui.done"));
                    });

                    // Select dataset if needed
                    if (type.startsWith("catalog-")) {
                        // Descriptor file
                        FileHandle descFile = Gdx.files.absolute(GlobalConf.data.DATA_LOCATION + File.separator + currentJson.getString("check"));
                        GlobalConf.data.CATALOG_JSON_FILES = descFile.path();
                    }

                    me.acceptButton.setDisabled(false);
                    currentDownloadFile.setText("");

                    trio.getThird().setText("Found");
                    trio.getThird().setColor(0, 1, 0, 1);

                    Gdx.app.postRunnable(() -> {
                        downloadNext();
                    });
                } else {
                    logger.info("Error getting dataset: " + name);
                    trio.getThird().setText("Failed");
                    trio.getThird().setColor(1, 1, 0, 1);
                }
            };

            // Download
            me.acceptButton.setDisabled(true);
            currentDownloadFile.setText("Current dataset: " + currentJson.getString("name"));
            DownloadHelper.downloadFile(url, tempDownload, pr, finish, null, null);
        } else {
            // Finished all downloads!
        }

    }

    private void decompress(String in, File out, OwnTextButton b) throws Exception {
        try (TarArchiveInputStream fin = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(in)))) {
            String bytes = nf.format(uncompressedSize(in) / 1000d);
            TarArchiveEntry entry;
            while ((entry = fin.getNextTarEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                File curfile = new File(out, entry.getName());
                File parent = curfile.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }
                IOUtils.copy(fin, new FileOutputStream(curfile));
                Gdx.app.postRunnable(() -> {
                    b.setText(txt("gui.download.extracting", nf.format(fin.getBytesRead() / 1000d) + "/" + bytes + " Kb"));
                });
            }
        }
    }

    private int uncompressedSize(String inputFilePath) throws Exception {
        RandomAccessFile raf = new RandomAccessFile(inputFilePath, "r");
        raf.seek(raf.length() - 4);
        byte[] bytes = new byte[4];
        raf.read(bytes);
        int fileSize = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
        if (fileSize < 0)
            fileSize += (1L << 32);
        raf.close();
        return fileSize;
    }

    private void cleanupTempFiles() {
        Path tempDownload = Paths.get(GlobalConf.data.DATA_LOCATION, "temp.tar.gz");
        if (Files.exists(tempDownload)) {
            try {
                Files.delete(tempDownload);
            } catch (IOException e) {
                logger.error(e, "Failed cleaning up file: " + tempDownload.toString());
            }
        }
    }

    @Override
    protected void accept() {
        // No change to execute exit event, manually restore cursor to default
        Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
        cleanupTempFiles();
    }

    @Override
    protected void cancel() {
        cleanupTempFiles();
        Gdx.app.exit();
    }

}
