/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.datadesc;

import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.SysUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.io.FileInfoInputStream;
import org.kamranzafar.jtar.TarEntry;
import org.kamranzafar.jtar.TarInputStream;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public class DatasetDownloadUtils {
    private static final Logger.Log logger = Logger.getLogger(DatasetDownloadUtils.class);
    private static final DatasetDownloadUtils instance = new DatasetDownloadUtils();

    /**
     * Returns the file size.
     *
     * @param inputFilePath A file.
     *
     * @return The size in bytes.
     */
    public static long fileSize(String inputFilePath) {
        return new File(inputFilePath).length();
    }

    /**
     * Returns the GZ uncompressed size.
     *
     * @param inputFilePath A gzipped file.
     *
     * @return The uncompressed size in bytes.
     *
     * @throws IOException If the file failed to read.
     */
    public static long fileSizeGZUncompressed(String inputFilePath) throws IOException {
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

    /**
     * We should never need to call this, as the main {@link GaiaSky#dispose()} method
     * already cleans up the temp directory.
     * This way, we allow download resumes within the same session.
     */
    public static void cleanupTempFiles() {
        cleanupTempFiles(true, false);
    }

    /**
     * Remove a single file in the temp directory.
     *
     * @param file The file to remove.
     */
    public static void cleanupTempFile(String file) {
        deleteFile(Path.of(file));
    }

    @SuppressWarnings("all")
    public static void cleanupTempFiles(final boolean dataDownloads,
                                        final boolean dataDescriptor) {
        if (dataDownloads) {
            final Path tempDir = SysUtils.getDataTempDir(Settings.settings.data.location);
            // Clean up partial downloads.
            try (final Stream<Path> stream = Files.find(tempDir, 2, (path, basicFileAttributes) -> {
                final File file = path.toFile();
                return !file.isDirectory() && file.getName().endsWith("tar.gz.part");
            })) {
                stream.forEach(DatasetDownloadUtils::deleteFile);
            } catch (IOException e) {
                logger.error(e);
            }
        }

        if (dataDescriptor) {
            // Clean up data descriptor.
            Path gsDownload = SysUtils.getDataTempDir(Settings.settings.data.location).resolve("gaiasky-data.json");
            deleteFile(gsDownload);
        }
    }

    public static void deleteFile(Path p) {
        if (java.nio.file.Files.exists(p)) {
            try {
                java.nio.file.Files.delete(p);
            } catch (IOException e) {
                logger.error(e, "Failed cleaning up file: " + p);
            }
        }
    }

    private final static DecimalFormat nf = new DecimalFormat("##0.0");

    public static void decompress(String in, File out, DatasetDesc dataset) throws Exception {
        FileInfoInputStream fIs = new FileInfoInputStream(in);
        GZIPInputStream gzIs = new GZIPInputStream(fIs);
        TarInputStream tarIs = new TarInputStream(gzIs);
        double sizeKb = DatasetDownloadUtils.fileSize(in) / 1000d;
        String sizeKbStr = nf.format(sizeKb);
        TarEntry entry;
        long last = 0;
        boolean error = false;
        Exception errorException = null;
        Array<File> processedFiles = new Array<>();
        while (null != (entry = tarIs.getNextEntry())) {
            if (entry.isDirectory()) {
                continue;
            }
            File curFile = new File(out, entry.getName());
            File parent = curFile.getParentFile();
            if (!parent.exists()) {
                if (!parent.mkdirs()) {
                    logger.info("Parent directory not created, already exists: " + parent.toPath());
                }
            }

            try (FileOutputStream fos = new FileOutputStream(curFile); BufferedOutputStream dest = new BufferedOutputStream(fos)) {
                processedFiles.add(curFile);

                int count;
                byte[] data = new byte[2048];

                while ((count = tarIs.read(data)) != -1) {
                    dest.write(data, 0, count);
                }

            } catch (IOException e) {
                errorException = e;
                error = true;
                break;
            }

            // Every 250 ms we update the view.
            long current = System.currentTimeMillis();
            long elapsed = current - last;
            if (elapsed > 250) {
                GaiaSky.postRunnable(() -> {
                    float val = (float) ((fIs.getBytesRead() / 1000d) / sizeKb) * 100f;
                    String progressString = I18n.msg("gui.download.extracting", nf.format(fIs.getBytesRead() / 1000d) + "/" + sizeKbStr + " Kb");
                    EventManager.publish(Event.DATASET_DOWNLOAD_PROGRESS_INFO, instance, dataset.key, val, progressString, null);
                });
                last = current;
            }

        }

        if (error) {
            String msg = I18n.msg("gui.download.extracting.error", errorException);
            logger.error(errorException, msg);
            EventManager.publish(Event.POST_POPUP_NOTIFICATION, instance, msg, -1f);
            // Delete uncompressed files.
            for (File f : processedFiles) {
                DatasetDownloadUtils.deleteFile(f.toPath());
            }

        }
    }

    public static  boolean isEnabled(final DatasetDesc dataset) {
        return isPathIn(Settings.settings.data.dataFile(dataset.checkStr), Settings.settings.data.dataFiles);
    }

    public static boolean isPathIn(String path, List<String> setting) {
        for (String candidate : setting) {
            var candidatePath = Settings.settings.data.dataPath(candidate);
            try {
                if (Path.of(path).toRealPath().equals(candidatePath.toRealPath())) {
                    return true;
                }
            } catch (IOException e) {
                logger.error(e);
            }
        }
        return false;
    }
}
