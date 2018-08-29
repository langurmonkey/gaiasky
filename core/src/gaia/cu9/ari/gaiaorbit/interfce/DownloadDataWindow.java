package gaia.cu9.ari.gaiaorbit.interfce;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import gaia.cu9.ari.gaiaorbit.desktop.util.SysUtils;
import gaia.cu9.ari.gaiaorbit.util.DownloadHelper;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.ProgressRunnable;
import gaia.cu9.ari.gaiaorbit.util.format.INumberFormat;
import gaia.cu9.ari.gaiaorbit.util.format.NumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.scene2d.FileChooser;
import gaia.cu9.ari.gaiaorbit.util.scene2d.FileChooser.ResultListener;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnCheckBox;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnLabel;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnTextButton;

public class DownloadDataWindow extends GenericDialog {
    private static final Log logger = Logger.getLogger(DownloadDataWindow.class);
    private INumberFormat nf;
    private JsonReader reader;

    public DownloadDataWindow(Stage stage, Skin skin) {
        super(txt("gui.download.title"), skin, stage);
        this.nf = NumberFormatFactory.getFormatter("##0.0");
        this.reader = new JsonReader();

        setCancelText(txt("gui.exit"));
        setAcceptText(txt("gui.start"));

        // Build
        buildSuper();
    }

    @Override
    protected void build() {
        float pad = 10 * GlobalConf.SCALE_FACTOR;
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

        downloadTable.add(hg).left().colspan(2).padBottom(pad).row();
        downloadTable.add(catalogsLocLabel).left().padBottom(pad);

        SysUtils.getDefaultDataDir().mkdirs();
        String catLoc = GlobalConf.data.DATA_LOCATION;
        OwnTextButton catalogsLoc = new OwnTextButton(catLoc, skin);
        catalogsLoc.pad(buttonpad * 4);
        catalogsLoc.setMinWidth(GlobalConf.SCALE_FACTOR == 1 ? 450 : 650);
        downloadTable.add(catalogsLoc).left().padLeft(pad).padBottom(pad).row();
        Cell<Actor> notice = downloadTable.add((Actor) null).colspan(2).padBottom(pad);
        notice.row();

        // Parse available files
        JsonValue dataDesc = reader.parse(Gdx.files.absolute(catLoc + "/gaiasky-data.json"));

        Table datasetsTable = new Table(skin);
        datasetsTable.add(new OwnLabel("Name", skin, "header")).left().padRight(pad).padBottom(pad);
        datasetsTable.add(new OwnLabel("Description", skin, "header")).left().padRight(pad).padBottom(pad);
        datasetsTable.add(new OwnLabel("Type", skin, "header")).left().padRight(pad).padBottom(pad);
        datasetsTable.add(new OwnLabel("Have", skin, "header")).center().padRight(pad).padBottom(pad).row();

        JsonValue dataset = dataDesc.child().child();
        while (dataset != null) {
            // Check if we have it
            Path check = Paths.get(GlobalConf.data.DATA_LOCATION, dataset.getString("check"));
            boolean exists = Files.exists(check) && Files.isReadable(check);

            String name = dataset.getString("name");
            // Add dataset to desc table
            OwnCheckBox cb = new OwnCheckBox(name, skin, pad);
            boolean baseData = name.equals("default-data"); 
            cb.setChecked(!exists && baseData);
            cb.setDisabled(baseData);
            OwnLabel haveit = new OwnLabel(exists ? "V" : "X", skin);
            if (exists)
                haveit.setColor(0, 1, 0, 1);
            else
                haveit.setColor(1, 0, 0, 1);

            datasetsTable.add(cb).left().padRight(pad).padBottom(pad);
            datasetsTable.add(new OwnLabel(dataset.getString("description"), skin)).left().padRight(pad).padBottom(pad);
            datasetsTable.add(new OwnLabel(dataset.getString("type"), skin)).left().padRight(pad).padBottom(pad);
            datasetsTable.add(haveit).center().padBottom(pad);

            datasetsTable.row();

            dataset = dataset.next();
        }

        downloadTable.add(datasetsTable).center().padBottom(pad * 2).colspan(2).row();

        OwnTextButton downloadNow = new OwnTextButton(txt("gui.download.download").toUpperCase(), skin, "download");
        downloadNow.pad(buttonpad * 4);
        downloadNow.setMinWidth(catalogsLoc.getWidth());
        downloadNow.setMinHeight(50 * GlobalConf.SCALE_FACTOR);
        downloadTable.add(downloadNow).center().colspan(2);

        downloadNow.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                parseDataDesc(Gdx.files.absolute(catLoc + "/gaiasky-data.json"), downloadNow);
            }
            return true;
        });

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

    /**
     * Parses the data descriptor file and takes the necessary actions which may be
     * exposing downloads to the user, downloading default data package, etc.
     * @param fh The file where the descriptor is
     * @param downloadNow The download button in the interface
     */
    private void parseDataDesc(FileHandle fh, OwnTextButton downloadNow) {
        // Parse descriptor file
        JsonValue dataDesc = reader.parse(fh);
        JsonValue firstItemDesc = dataDesc.get("files").child();
        String url = firstItemDesc.getString("file");
        String name = firstItemDesc.getString("name");

        FileHandle archiveFile = Gdx.files.absolute(GlobalConf.data.DATA_LOCATION + "/temp.tar.gz");

        ProgressRunnable pr = (progress) -> {
            final String progressString = progress >= 100 ? txt("gui.done") : txt("gui.download.downloading", nf.format(progress));

            // Since we are downloading on a background thread, post a runnable to touch UI
            Gdx.app.postRunnable(() -> {
                if (progress == 100) {
                    downloadNow.setDisabled(true);
                }
                downloadNow.setText(progressString);
            });
        };

        Runnable finish = () -> {
            // Unpack
            logger.info("Extracting: " + archiveFile.path());
            String dataLocation = GlobalConf.data.DATA_LOCATION + File.separatorChar;
            try {
                decompress(archiveFile.path(), new File(dataLocation), downloadNow);
            } catch (Exception e) {
                logger.error(e, "Error decompressing: " + archiveFile.path());
            }

            // Remove archive
            archiveFile.file().delete();

            // Descriptor file
            FileHandle descFile = Gdx.files.absolute(GlobalConf.data.DATA_LOCATION + "/catalog-dr2-default.json");

            // Done
            Gdx.app.postRunnable(() -> {
                downloadNow.setText(txt("gui.done"));
            });

            // Select dataset
            GlobalConf.data.CATALOG_JSON_FILES = descFile.path();
            me.acceptButton.setDisabled(false);
        };

        // Download
        DownloadHelper.downloadFile(url, archiveFile, pr, finish, null, null);

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

    @Override
    protected void accept() {
        // No change to execute exit event, manually restore cursor to default
        Gdx.graphics.setSystemCursor(SystemCursor.Arrow);
    }

    @Override
    protected void cancel() {
        Gdx.app.exit();
    }

}
