package gaia.cu9.ari.gaiaorbit.interfce;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.python.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.python.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.python.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.python.apache.commons.compress.utils.IOUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net.HttpMethods;
import com.badlogic.gdx.Net.HttpRequest;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.Net.HttpResponseListener;
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

import gaia.cu9.ari.gaiaorbit.desktop.util.SysUtils;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.format.INumberFormat;
import gaia.cu9.ari.gaiaorbit.util.format.NumberFormatFactory;
import gaia.cu9.ari.gaiaorbit.util.scene2d.FileChooser;
import gaia.cu9.ari.gaiaorbit.util.scene2d.FileChooser.ResultListener;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnLabel;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnTextButton;

public class DownloadCatalogWindow extends GenericDialog {
    private static final Log logger = Logger.getLogger(DownloadCatalogWindow.class);
    private INumberFormat nf;

    public DownloadCatalogWindow(Stage stage, Skin skin) {
        super(txt("gui.dsdownload.title"), skin, stage);
        this.nf = NumberFormatFactory.getFormatter("##0.0");

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

        OwnLabel catalogsLocLabel = new OwnLabel(txt("gui.dsdownload.location") + ":", skin);
        

        HorizontalGroup hg = new HorizontalGroup();
        hg.space(15 * GlobalConf.SCALE_FACTOR);
        Image system = new Image(skin.getDrawable("tooltip-icon"));    
        OwnLabel downloadInfo = new OwnLabel(txt("gui.dsdownload.info"), skin);
        hg.addActor(system);
        hg.addActor(downloadInfo);
        
        downloadTable.add(hg).left().colspan(2).padBottom(pad).row();
        downloadTable.add(catalogsLocLabel).left().padBottom(pad);

        SysUtils.getDefaultCatalogsDir().mkdirs();
        String catLoc;
        if (GlobalConf.data.CATALOG_LOCATIONS == null || GlobalConf.data.CATALOG_LOCATIONS.length == 0) {
            catLoc = SysUtils.getDefaultCatalogsDir().getAbsolutePath();
            GlobalConf.data.CATALOG_LOCATIONS = new String[] { catLoc };
        } else {
            catLoc = GlobalConf.data.CATALOG_LOCATIONS[0];
        }
        OwnTextButton catalogsLoc = new OwnTextButton(catLoc, skin);
        catalogsLoc.pad(buttonpad * 4);
        catalogsLoc.setMinWidth(GlobalConf.SCALE_FACTOR == 1 ? 450 : 650);
        downloadTable.add(catalogsLoc).left().padLeft(pad).padBottom(pad).row();
        Cell<Actor> notice = downloadTable.add((Actor) null).colspan(2).padBottom(pad);
        notice.row();

        OwnTextButton downloadNow = new OwnTextButton(txt("gui.dsdownload.download").toUpperCase(), skin, "download");
        downloadNow.pad(buttonpad * 4);
        downloadNow.setMinWidth(catalogsLoc.getWidth());
        downloadNow.setMinHeight(50 * GlobalConf.SCALE_FACTOR);
        downloadTable.add(downloadNow).center().colspan(2);

        catalogsLoc.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                FileChooser fc = FileChooser.createPickDialog(txt("gui.dsdownload.pickloc"), skin, Gdx.files.absolute(GlobalConf.data.CATALOG_LOCATIONS[0]));
                fc.setResultListener(new ResultListener() {
                    @Override
                    public boolean result(boolean success, FileHandle result) {
                        if (success) {
                            if (result.file().canRead() && result.file().canWrite()) {
                                // do stuff with result
                                catalogsLoc.setText(result.path());
                                GlobalConf.data.CATALOG_LOCATIONS = new String[] { result.path() };
                                me.pack();
                            } else {
                                Label warn = new OwnLabel(txt("gui.dsdownload.pickloc.permissions"), skin);
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

        downloadNow.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                if (!downloadNow.isDisabled()) {
                    downloadNow.setDisabled(true);
                    // Make a GET request
                    HttpRequest request = new HttpRequest(HttpMethods.GET);
                    request.setTimeOut(2500);
                    request.setUrl(GlobalConf.program.DEFAULT_CATALOG_URL);

                    FileHandle archiveFile = Gdx.files.absolute(GlobalConf.data.CATALOG_LOCATIONS[0] + "/temp.tar.gz");

                    // Send the request, listen for the response
                    Gdx.net.sendHttpRequest(request, new HttpResponseListener() {
                        @Override
                        public void handleHttpResponse(HttpResponse httpResponse) {
                            // Determine how much we have to download
                            long length = Long.parseLong(httpResponse.getHeader("Content-Length"));

                            // We're going to download the file to external storage, create the streams
                            InputStream is = httpResponse.getResultAsStream();
                            OutputStream os = archiveFile.write(false);

                            byte[] bytes = new byte[1024];
                            int count = -1;
                            long read = 0;
                            try {
                                me.acceptButton.setDisabled(true);
                                logger.info("Started download of file : " + GlobalConf.program.DEFAULT_CATALOG_URL);
                                // Keep reading bytes and storing them until there are no more.
                                while ((count = is.read(bytes, 0, bytes.length)) != -1) {
                                    os.write(bytes, 0, count);
                                    read += count;

                                    // Update the UI with the download progress
                                    final double progress = ((double) read / (double) length) * 100;
                                    final String progressString = progress >= 100 ? txt("gui.done") : txt("gui.dsdownload.downloading", nf.format(progress));

                                    // Since we are downloading on a background thread, post a runnable to touch UI
                                    Gdx.app.postRunnable(() -> {
                                        if (progress == 100) {
                                            downloadNow.setDisabled(true);
                                        }
                                        downloadNow.setText(progressString);
                                    });
                                }
                                is.close();
                                os.close();
                                logger.info(txt("gui.dsdownload.finished", archiveFile.path()));

                                // Unpack
                                decompress(archiveFile.path(), new File(GlobalConf.data.CATALOG_LOCATIONS[0]), downloadNow);
                                // Remove archive
                                archiveFile.file().delete();

                                // Descriptor file
                                FileHandle descFile = Gdx.files.absolute(GlobalConf.data.CATALOG_LOCATIONS[0] + "/catalog-dr2-default.json");
                                String descStr = "{\n" +
                                        "\"name\" : \"DR2 - default\",\n" +
                                        "\"description\" : \"Gaia DR2 (20%/0.5% bright/faint error). 7.5M stars.\",\n" +
                                        "\"data\" : [\n" +
                                        "{\n" +
                                        "\"loader\": \"gaia.cu9.ari.gaiaorbit.data.group.OctreeGroupLoader\",\n" +
                                        "\"files\": [ \"@PATH@/66-mag-100000-0.2-0.005/particles/\", \"@PATH@/66-mag-100000-0.2-0.005/metadata.bin\" ]\n" +
                                        "}\n" +
                                        "]}";
                                descStr = descStr.replaceAll("@PATH@", GlobalConf.data.CATALOG_LOCATIONS[0]);
                                descFile.writeString(descStr, false);

                                // Done
                                Gdx.app.postRunnable(() -> {
                                    downloadNow.setText(txt("gui.done"));
                                });

                                // Select dataset
                                GlobalConf.data.CATALOG_JSON_FILES = descFile.path();
                                me.acceptButton.setDisabled(false);
                            } catch (Exception e) {
                                logger.error(e, txt("gui.dsdownload.wrong"));
                                downloadNow.setText(txt("gui.dsdownload.wrong"));
                            }
                        }

                        @Override
                        public void failed(Throwable t) {
                            logger.error(t, txt("gui.dsdownload.fail"));
                            Gdx.app.postRunnable(new Runnable() {
                                @Override
                                public void run() {
                                    downloadNow.setText(txt("gui.dsdownload.fail"));
                                }
                            });
                        }

                        @Override
                        public void cancelled() {
                            Gdx.app.postRunnable(new Runnable() {
                                @Override
                                public void run() {
                                    downloadNow.setText(txt("gui.dsdownload.cancel"));
                                }
                            });
                        }
                    });
                    return true;
                }
            }
            return false;
        });

        topCell.setActor(downloadTable);

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
                    b.setText(txt("gui.dsdownload.extracting", nf.format(fin.getBytesRead() / 1000d) + "/" + bytes + " Kb"));
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
