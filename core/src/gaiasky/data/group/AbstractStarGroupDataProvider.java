/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.LongMap;
import gaiasky.data.api.IStarGroupDataProvider;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.util.Constants;
import gaiasky.util.Logger.Log;
import gaiasky.util.TextUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.io.ByteBufferInputStream;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3d;
import gaiasky.util.parse.Parser;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.zip.GZIPInputStream;

public abstract class AbstractStarGroupDataProvider implements IStarGroupDataProvider {
    public static double NEGATIVE_DIST = 1 * Constants.M_TO_U;
    protected static Log logger;
    /**
     * Parallelism value
     */
    protected final int parallelism;
    protected Map<String, Object> params;
    protected Map<ColId, Integer> indexMap;
    protected List<IParticleRecord> list;
    protected LongMap<double[]> sphericalPositions;
    protected LongMap<float[]> colors;
    protected long[] countsPerMag;
    protected Matrix4d transform;
    protected Set<Long> mustLoadIds = null;
    protected List<AdditionalCols> additional;
    /**
     * Files or folders with optionally gzipped CSVs containing additional columns to be matched by sourceId with the main catalog. Column names must comport
     * to {@link ColId}.
     */
    protected String[] additionalFiles = null;
    /**
     * RUWE cap value. Will accept all stars with star_ruwe &le; ruwe
     */
    protected Double ruwe = Double.NaN;
    /**
     * Distance cap in parsecs
     */
    protected double distCap = Double.MAX_VALUE;
    /**
     * <p>
     * The loader will only load stars for which the parallax error is
     * at most the percentage given here, in [0..1]. Faint stars (gmag &ge; 13.1)
     * More specifically, the following must be met:
     * </p>
     * <code>pllx_err &lt; pllx * pllxErrFactor</code>
     **/
    protected double parallaxErrorFactorFaint = 0.125;
    /**
     * <p>
     * The loader will only load stars for which the parallax error is
     * at most the percentage given here, in [0..1]. Bright stars (gmag &lt; 13.1)
     * More specifically, the following must be met:
     * </p>
     * <code>pllx_err &lt; pllx * pllxErrFactor</code>
     **/
    protected double parallaxErrorFactorBright = 0.25;
    /**
     * Whether to use an adaptive threshold which lets more
     * bright stars in to avoid artifacts.
     */
    protected boolean adaptiveParallax = true;
    /**
     * The zero point for the parallaxes in mas. Gets added to all loaded
     * parallax values
     */
    protected double parallaxZeroPoint = 0;
    /**
     * Apply magnitude/color corrections for extinction/reddening
     */
    protected boolean magCorrections = false;
    /**
     * Maximum number of files to load. Negative for unlimited
     */
    protected int fileNumberCap = -1;
    /**
     * Maximum number of files to load per file
     */
    protected int starNumberCap = -1;

    public AbstractStarGroupDataProvider() {
        super();
        try (var p = ForkJoinPool.commonPool()) {
            parallelism = p.getParallelism();
        }
    }

    public ColId colIdFromStr(final String name) {
        return switch (name) {
            case "source_id", "sourceid" -> ColId.sourceid;
            case "hip" -> ColId.hip;
            case "names", "name" -> ColId.names;
            case "ra" -> ColId.ra;
            case "dec", "de" -> ColId.dec;
            case "plx", "pllx", "parallax" -> ColId.pllx;
            case "ra_e", "ra_err", "ra_error" -> ColId.ra_err;
            case "dec_e", "dec_err", "dec_error", "de_e", "de_err", "de_error" -> ColId.dec_err;
            case "plx_e", "plx_err", "plx_error", "pllx_e", "pllx_err", "pllx_error" -> ColId.pllx_err;
            case "pmra" -> ColId.pmra;
            case "pmdec", "pmde" -> ColId.pmdec;
            case "radvel", "rv" -> ColId.radvel;
            case "radvel_err", "radvel_e", "rv_err", "rv_e" -> ColId.radvel_err;
            case "gmag", "appmag" -> ColId.gmag;
            case "bpmag", "bp" -> ColId.bpmag;
            case "rpmag", "rp" -> ColId.rpmag;
            case "bp-rp", "bp_rp" -> ColId.bp_rp;
            case "col_idx", "b_v", "b-v" -> ColId.col_idx;
            case "ref_epoch" -> ColId.ref_epoch;
            case "ruwe" -> ColId.ruwe;
            case "teff", "t_eff", "T_eff" -> ColId.teff;
            case "ag" -> ColId.ag;
            case "ebp_min_rp" -> ColId.ebp_min_rp;
            case "geodist" -> ColId.geodist;
            default -> null;
        };
    }

    protected int idx(ColId colId) {
        if (indexMap != null && indexMap.containsKey(colId)) return indexMap.get(colId);
        else return -1;
    }

    protected boolean hasCol(ColId colId) {
        return indexMap != null && indexMap.containsKey(colId) && indexMap.get(colId) >= 0;
    }

    protected boolean hasAdditional(ColId col, Long sourceId) {
        Double val = getAdditionalValue(col, sourceId);
        return val != null && !Double.isNaN(val) && !Double.isInfinite(val);
    }

    protected boolean hasAdditionalColumn(ColId col) {
        if (additional == null) return false;
        for (AdditionalCols add : additional) {
            if (add != null && add.hasCol(col)) return true;
        }
        return false;
    }

    protected Double getAdditionalValue(ColId col, Long sourceId) {
        if (additional == null) return null;
        for (AdditionalCols add : additional) {
            if (add != null && add.hasCol(col)) {
                Double d = add.get(col, sourceId);
                if (d != null) return d;
            }
        }
        return null;
    }

    /**
     * Initialises the lists and structures given a file by counting the number
     * of lines
     *
     * @param f The file handle to count the lines
     */
    protected void initLists(FileHandle f) {
        try {
            int lines = countLines(f);
            initLists(lines - 1);
        } catch (Exception e) {
            logger.error(e);
        }
    }

    /**
     * Initialises the lists and structures given number of elements
     */
    protected void initLists(int elems) {
        if (parallelism > 1) list = Collections.synchronizedList(new ArrayList<>(elems));
        else list = new ArrayList<>(elems);
    }

    protected void initLists() {
        initLists(1000);
        sphericalPositions = new LongMap<>();
        colors = new LongMap<>();
    }

    @Override
    public List<IParticleRecord> loadData(String file) {
        return loadData(file, 1.0f);
    }

    /**
     * Returns whether the star must be loaded or not
     *
     * @param id The star ID
     *
     * @return Whether the star with the given ID must be loaded
     */
    protected boolean mustLoad(long id) {
        return mustLoadIds != null && mustLoadIds.contains(id);
    }

    /**
     * Checks whether the parallax is accepted or not.
     * <b>If adaptive is not enabled:</b>
     * <pre>
     * accepted = pllx &gt; 0 &amp;&amp; pllx_err &lt; pllx * pllx_err_factor &amp;&amp; pllx_err &le; 1
     * </pre>
     *
     * <p>
     * <b>If adaptive is enabled:</b>
     * </p>
     * <pre>
     * accepted = pllx &gt; 0 &amp;&amp; pllx_err &lt; pllx * max(0.5, pllx_err_factor) &amp;&amp; pllx_err &le; 1, if apparent_magnitude &lt; 13.2
     * accepted = pllx &gt; 0 &amp;&amp; pllx_err &lt; pllx * pllx_err_factor &amp;&amp; pllx_err &le; 1, otherwise
     * </pre>
     *
     * @param appMag        Apparent magnitude of star.
     * @param parallax      Parallax of star.
     * @param parallaxError Parallax error of star.
     *
     * @return True if parallax is accepted, false otherwise
     */
    protected boolean acceptParallax(double appMag, double parallax, double parallaxError) {
        // If geometric distances are present, always accept, we use distances directly
        if (hasAdditionalColumn(ColId.geodist)) return true;
        if (!Double.isFinite(appMag)) {
            return false;
        } else if (adaptiveParallax && appMag < 13.1) {
            return parallax >= 0 && parallaxError < parallax * parallaxErrorFactorBright && parallaxError <= 1;
        } else {
            return parallax >= 0 && parallaxError < parallax * parallaxErrorFactorFaint && parallaxError <= 1;
        }
    }

    protected float getRuweValue(long sourceId, String[] tokens) {
        if (hasCol(ColId.ruwe)) {
            return Parser.parseFloat(tokens[idx(ColId.ruwe)]);
        } else {
            Double ruwe = getAdditionalValue(ColId.ruwe, sourceId);
            if (ruwe == null || ruwe.isInfinite() || ruwe.isNaN()) {
                return Float.NaN;
            }
            return ruwe.floatValue();
        }
    }

    /**
     * Gets the distance in parsecs to the star from the additional columns
     * map, if it exists. Otherwise, it returns a negative value.
     *
     * @param sourceId The source id of the source
     *
     * @return The geometric distance in parsecs if it exists, -1 otherwise.
     */
    protected double getGeoDistance(long sourceId) {
        Double geodist = getAdditionalValue(ColId.geodist, sourceId);
        if (geodist == null || geodist.isInfinite() || geodist.isNaN()) return -1;
        return geodist;
    }

    /**
     * Checks whether to accept the distance
     *
     * @param distance Distance in parsecs
     *
     * @return Whether to accept the distance or not
     */
    protected boolean acceptDistance(double distance) {
        return distance <= distCap;
    }

    protected int countLines(FileHandle f) throws IOException {
        InputStream is = new BufferedInputStream(f.read());
        return countLines(is);
    }

    public void setColumns(String columns) {

    }

    @Override
    public void setMustLoadIds(Set<Long> ids) {
        this.mustLoadIds = ids;
    }

    /**
     * Counts the lines on this input stream
     *
     * @param is The input stream
     *
     * @return The number of lines
     */
    protected int countLines(InputStream is) throws IOException {
        byte[] c = new byte[1024];
        int count = 0;
        int readChars = 0;
        boolean empty = true;
        while ((readChars = is.read(c)) != -1) {
            empty = false;
            for (int i = 0; i < readChars; ++i) {
                if (c[i] == '\n') {
                    ++count;
                }
            }
        }
        is.close();

        return (count == 0 && !empty) ? 1 : count;
    }

    protected void dumpToDisk(List<IParticleRecord> data, String filename, String format) {
        if (format.equals("bin")) dumpToDiskBin(data, filename);
        else if (format.equals("csv")) dumpToDiskCsv(data, filename);
    }

    protected void dumpToDiskBin(List<IParticleRecord> data, String filename) {
            // Use own binary format
            BinaryDataProvider io = new BinaryDataProvider();
            try {
                int n = data.size();
                io.writeData(data, new FileOutputStream(filename));
                logger.info("File " + filename + " written with " + n + " stars");
            } catch (Exception e) {
                logger.error(e);
            }
    }

    protected void dumpToDiskCsv(List<IParticleRecord> data, String filename) {
        String sep = "' ";
        try {
            PrintWriter writer = new PrintWriter(filename, StandardCharsets.UTF_8);
            writer.println("name(s), x[km], y[km], z[km], absmag, appmag, r, g, b");
            Vector3d gal = new Vector3d();
            int n = 0;
            for (IParticleRecord star : data) {
                float[] col = colors.get(star.id());
                double x = star.z();
                double y = -star.x();
                double z = star.y();
                gal.set(x, y, z).scl(Constants.U_TO_KM);
                gal.mul(Coordinates.equatorialToGalactic());
                writer.println(TextUtils.concatenate(Constants.nameSeparator, star.names()) + sep + x + sep + y + sep + z + sep + star.absMag() + sep + star.appMag() + sep + col[0] + sep + col[1] + sep + col[2]);
                n++;
            }
            writer.close();
            logger.info("File " + filename + " written with " + n + " stars");
        } catch (Exception e) {
            logger.error(e);
        }
    }

    @Override
    public LongMap<float[]> getColors() {
        return colors;
    }

    public void setParallaxErrorFactorFaint(double parallaxErrorFactor) {
        this.parallaxErrorFactorFaint = parallaxErrorFactor;
    }

    public void setParallaxErrorFactorBright(double parallaxErrorFactor) {
        this.parallaxErrorFactorBright = parallaxErrorFactor;
    }

    public void setAdaptiveParallax(boolean adaptive) {
        this.adaptiveParallax = adaptive;
    }

    public void setParallaxZeroPoint(double parallaxZeroPoint) {
        this.parallaxZeroPoint = parallaxZeroPoint;
    }

    public void setMagCorrections(boolean magCorrections) {
        this.magCorrections = magCorrections;
    }

    public long[] getCountsPerMag() {
        return this.countsPerMag;
    }

    @Override
    public void setAdditionalFiles(String additionalFiles) {
        if (additionalFiles != null && !additionalFiles.isBlank()) {
            this.additionalFiles = additionalFiles.split(",");
            this.additional = new ArrayList<>();
            if (this.additionalFiles.length > 0) loadAdditional();
        }
    }

    @Override
    public void setDistanceCap(double distCap) {
        this.distCap = distCap;
    }

    @Override
    public void setRUWECap(double RUWE) {
        this.ruwe = RUWE;
    }

    private void loadAdditional() {
        for (String additionalFile : additionalFiles) {
            AdditionalCols addit = new AdditionalCols();
            addit.indices = new HashMap<>();
            addit.values = new TreeMap<>();

            logger.info("Loading additional columns from " + additionalFile);

            Path f = Paths.get(additionalFile);
            loadAdditional(f, addit);
            additional.add(addit);

            logger.info(addit.indices.size() + " additional columns loaded for " + addit.values.size() + " stars");
        }
    }

    private void loadAdditional(Path f, AdditionalCols addit) {
        if (Files.isDirectory(f, LinkOption.NOFOLLOW_LINKS)) {
            File[] files = f.toFile().listFiles();
            assert files != null;
            int nFiles = files.length;
            int mod = nFiles / 20;
            int i = 1;
            for (File file : files) {
                if (nFiles > 60 && i % mod == 0) {
                    logger.info("Loading file " + i + "/" + nFiles);
                }
                loadAdditional(file.toPath(), addit);
                i++;
            }
        } else {
            try {
                loadAdditionalFile(f, addit);
            } catch (Exception e) {
                logger.error(e);
            }
        }
    }

    /**
     * Loads a single file, optionally gzipped into the given {@link AdditionalCols}
     *
     * @param f              The path.
     * @param additionalCols The {@link AdditionalCols} instance.
     *
     * @throws RuntimeException If the format of <code>additionalCols</code> is not correct.
     */
    private void loadAdditionalFile(Path f, AdditionalCols additionalCols) throws RuntimeException {
        InputStream data;
        try (var raf = new RandomAccessFile(f.toFile(), "r")) {
            FileChannel fc = raf.getChannel();
            MappedByteBuffer mem = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            data = new ByteBufferInputStream(mem);
            if (f.toString().endsWith(".gz")) data = new GZIPInputStream(data);
            BufferedReader br = new BufferedReader(new InputStreamReader(data));
            // Read header
            String additionalSplit = ",|\\s+";
            String[] header = br.readLine().strip().split(additionalSplit);
            int i = 0;
            for (String col : header) {
                col = col.strip();
                if (i == 0 && !col.equals(ColId.sourceid.name())) {
                    logger.error("First column: " + col + ", should be: " + ColId.sourceid.name());
                    throw new RuntimeException("Additional columns file must contain a sourceid in the first column");
                }
                if (i > 0 && !additionalCols.indices.containsKey(col)) {
                    additionalCols.indices.put(col, i - 1);
                }
                i++;
            }
            int nCols = header.length - 1;
            String line;
            i = 0;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(additionalSplit);
                Long sourceId = Parser.parseLong(tokens[0].trim());
                double[] vals = new double[nCols];
                for (int j = 1; j <= nCols; j++) {
                    if (tokens[j] != null && !tokens[j].strip().isBlank()) {
                        double val = Parser.parseDouble(tokens[j].strip());
                        vals[j - 1] = val;
                    } else {
                        // No value
                        vals[j - 1] = Double.NaN;
                    }
                }
                additionalCols.values.put(sourceId, vals);
                i++;
            }
            br.close();
        } catch (Exception e) {
            logger.error(e);
        }
    }

    @Override
    public void setFileNumberCap(int cap) {
        this.fileNumberCap = cap;
    }

    @Override
    public void setStarNumberCap(int starNumberCap) {
        this.starNumberCap = starNumberCap;
    }

    @Override
    public void setOutputFormatVersion(int version) {
    }

    @Override
    public void setProviderParams(Map<String, Object> params) {
        this.params = params;
    }

    @Override
    public void setTransformMatrix(Matrix4d transform) {
        this.transform = transform;
    }

    /**
     * Represents a column type.
     */
    public enum ColId {
        sourceid, hip, names, ra, dec, pllx, ra_err, dec_err, pllx_err, pmra, pmdec, radvel, pmra_err, pmdec_err, radvel_err, gmag, bpmag, rpmag, bp_rp, col_idx, ref_epoch, teff, radius, ag, ebp_min_rp, ruwe, geodist
    }

    public static class AdditionalCols {
        // Column name -> index
        Map<String, Integer> indices;
        // Sourceid -> values
        TreeMap<Long, double[]> values;

        public boolean hasCol(ColId col) {
            return indices != null && indices.containsKey(col.name());
        }

        public Double get(ColId col, Long sourceid) {
            try {
                if (hasCol(col)) {
                    return values.get(sourceid)[indices.get(col.name())];
                } else {
                    return null;
                }
            } catch (Exception e) {
                return null;
            }
        }
    }
}
