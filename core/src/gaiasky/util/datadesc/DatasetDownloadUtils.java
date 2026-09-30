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
import gaiasky.util.SysUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.io.FileInfoInputStream;
import org.kamranzafar.jtar.TarEntry;
import org.kamranzafar.jtar.TarInputStream;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

/**
 * Utilities to fetch and uncompress datasets.
 */
public class DatasetDownloadUtils {
    private static final Logger.Log logger = Logger.getLogger(DatasetDownloadUtils.class);

    public static final String mirrorKeyword = "@mirror-url@";

    /**
     * Reentrant lock to synchronize the latter part of the dataset download process: checksum,
     * extraction, and dataset enable+UI update.
     */
    public static final ReentrantLock EXTRACTION_LOCK = new ReentrantLock();

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
            final Path tempDir = SysUtils.getDataTempDir(GaiaSky.settings().data.location);
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
            Path gsDownload = SysUtils.getDataTempDir(GaiaSky.settings().data.location).resolve("gaiasky-data.json");
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

    public static void decompress(String in,
                                  File out,
                                  Dataset dataset) throws Exception {
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
                var source = entry;
                GaiaSky.postRunnable(() -> {
                    float val = (float) ((fIs.getBytesRead() / 1000d) / sizeKb) * 100f;
                    String progressString = I18n.msg("gui.download.extracting", nf.format(fIs.getBytesRead() / 1000d) + "/" + sizeKbStr + " Kb");
                    EventManager.publish(Event.DATASET_DOWNLOAD_PROGRESS_INFO, source, dataset.key, val, progressString, null);
                });
                last = current;
            }

        }

        if (error) {
            String msg = I18n.msg("gui.download.extracting.error", errorException);
            logger.error(errorException, msg);
            EventManager.publish(Event.POST_POPUP_NOTIFICATION, entry, msg, -1f);
            // Delete uncompressed files.
            for (File f : processedFiles) {
                DatasetDownloadUtils.deleteFile(f.toPath());
            }

        }
    }

    /**
     * Snapshots the paths of all {@code dataset.json} descriptors currently
     * present in the given data location (at depth 2, i.e.
     * {@code <data location>/<dataset dir>/dataset.json}). Used to detect
     * descriptors added by an extraction, regardless of the directory names
     * inside the archive.
     *
     * @param dataLocation The data location directory.
     *
     * @return The set of existing descriptor paths.
     */
    public static Set<Path> snapshotDescriptors(Path dataLocation) {
        Set<Path> descriptors = new HashSet<>();
        if (dataLocation != null && Files.isDirectory(dataLocation)) {
            try (Stream<Path> stream = Files.find(dataLocation, 2, (path, attrs) -> path.getFileName() != null && path.getFileName().toString().equals("dataset.json"))) {
                stream.forEach(descriptors::add);
            } catch (IOException e) {
                logger.error(e, "Error snapshotting dataset descriptors in: " + dataLocation);
            }
        }
        return descriptors;
    }

    /**
     * Finds {@code dataset.json} descriptors in the given data location that
     * are not contained in the given snapshot. Meant to be called right after
     * an extraction, with the snapshot taken right before.
     *
     * @param before       The snapshot taken before the extraction.
     * @param dataLocation The data location directory.
     *
     * @return The list of new descriptor paths, possibly empty.
     */
    public static List<Path> findNewDescriptors(Set<Path> before,
                                                Path dataLocation) {
        List<Path> newDescriptors = new ArrayList<>();
        for (Path current : snapshotDescriptors(dataLocation)) {
            if (!before.contains(current)) {
                newDescriptors.add(current);
            }
        }
        return newDescriptors;
    }

    /**
     * Builds a {@link Dataset} from a {@code dataset.json} descriptor file.
     *
     * @param descriptorFile The path to the descriptor file.
     *
     * @return The dataset, or null if it could not be built.
     */
    public static Dataset datasetFromDescriptor(Path descriptorFile) {
        try {
            var reader = new com.badlogic.gdx.utils.JsonReader();
            var val = reader.parse(new com.badlogic.gdx.files.FileHandle(descriptorFile.toFile()));
            var catalogFile = new com.badlogic.gdx.files.FileHandle(descriptorFile.toFile());
            return new Dataset(reader, val, catalogFile);
        } catch (Exception e) {
            logger.error(e, "Error building dataset from descriptor: " + descriptorFile);
            return null;
        }
    }

    /**
     * Copies the installation-related fields of the source dataset into the
     * target dataset, leaving identity fields (key, file URL) untouched. Used
     * to update a URL-derived stub dataset in place once the real descriptor
     * has been resolved after extraction.
     *
     * @param target The dataset to update.
     * @param source The dataset to copy the fields from.
     */
    public static void copyInstallFields(Dataset target,
                                         Dataset source) {
        target.name = source.name;
        target.description = source.description;
        target.type = source.type;
        target.checkStr = source.checkStr;
        target.checkPath = source.checkPath;
        target.catalogFile = source.catalogFile;
        target.exists = source.exists;
        target.status = source.status;
        target.myVersion = source.myVersion;
    }

    /**
     * Scans the given data location for {@code dataset.json} descriptors and
     * builds the datasets they describe.
     *
     * @param dataLocation The data location directory.
     *
     * @return The list of datasets described by local descriptors, possibly empty.
     */
    public static List<Dataset> localDatasets(Path dataLocation) {
        List<Dataset> datasets = new ArrayList<>();
        if (dataLocation != null && Files.isDirectory(dataLocation)) {
            try (Stream<Path> stream = Files.find(dataLocation, 2, (path, attrs) -> path.getFileName() != null && path.getFileName().toString().equals("dataset.json"))) {
                stream.forEach(path -> {
                    var ds = datasetFromDescriptor(path);
                    if (ds != null) {
                        datasets.add(ds);
                    }
                });
            } catch (IOException e) {
                logger.error(e, "Error scanning dataset descriptors in: " + dataLocation);
            }
        }
        return datasets;
    }

    /**
     * Finds a locally installed dataset matching the given key or name, by
     * scanning the {@code dataset.json} descriptors in the data location.
     *
     * @param dataLocation The data location directory.
     * @param key          The dataset key. May be null.
     * @param name         The dataset name. May be null.
     *
     * @return The matching dataset, or null if none is found.
     */
    public static Dataset findLocalDataset(Path dataLocation,
                                           String key,
                                           String name) {
        for (var ds : localDatasets(dataLocation)) {
            if ((key != null && key.equalsIgnoreCase(ds.key)) || (name != null && name.equalsIgnoreCase(ds.name))) {
                return ds;
            }
        }
        return null;
    }

    public static boolean isEnabled(Dataset dataset) {
        return isPathIn(GaiaSky.settings().data.dataFile(dataset.checkStr), GaiaSky.settings().data.dataFiles);
    }

    public static boolean isPathIn(String path,
                                   List<String> setting) {
        for (String candidate : setting) {
            var candidatePath = GaiaSky.settings().data.dataPath(candidate);
            try {
                if (Path.of(path).toRealPath().equals(candidatePath.toRealPath())) {
                    return true;
                }
            } catch (NoSuchFileException e) {
                // This candidate is temporarily unavailable; check the others.
                // Just continue.
            } catch (IOException e) {
                logger.error(e);
                return false;
            }
        }
        return false;
    }
}
