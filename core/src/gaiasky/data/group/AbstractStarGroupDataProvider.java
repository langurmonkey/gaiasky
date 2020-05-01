/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.LongMap;
import gaiasky.scenegraph.ParticleGroup.ParticleBean;
import gaiasky.scenegraph.StarGroup.StarBean;
import gaiasky.util.Constants;
import gaiasky.util.LargeLongMap;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.TextUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.Vector3d;
import gaiasky.util.parse.Parser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.GZIPInputStream;

public abstract class AbstractStarGroupDataProvider implements IStarGroupDataProvider {
    protected static Log logger = Logger.getLogger(AbstractStarGroupDataProvider.class);
    public static double NEGATIVE_DIST = 1 * Constants.M_TO_U;

    public enum ColId {
        sourceid, ra, dec, pllx, ra_err, dec_err,
        pllx_err, pmra, pmdec, radvel, pmra_err,
        pmdec_err, radvel_err, gmag, bpmag, rpmag, bp_rp,
        ref_epoch, teff, radius, ag, ebp_min_rp, ruwe, geodist
    }

    protected Map<ColId, Integer> indexMap;

    protected int idx(ColId colId) {
        if (indexMap != null && indexMap.containsKey(colId))
            return indexMap.get(colId);
        else
            return -1;
    }

    protected boolean hasCol(ColId colId) {
        return indexMap != null && indexMap.containsKey(colId) && indexMap.get(colId) >= 0;
    }

    protected Array<ParticleBean> list;
    protected LongMap<double[]> sphericalPositions;
    protected LongMap<float[]> colors;
    protected long[] countsPerMag;
    protected Set<Long> mustLoadIds = null;

    public class AdditionalCols {
        // Column name -> index
        Map<String, Integer> indices;
        // Sourceid -> values
        LargeLongMap<double[]> values;

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

    protected List<AdditionalCols> additional;

    protected boolean hasAdditional(ColId col, Long sourceId) {
        return getAdditionalValue(col, sourceId) != null;
    }

    protected boolean hasAdditionalColumn(ColId col) {
        for (AdditionalCols add : additional) {
            if (add != null && add.hasCol(col))
                return true;
        }
        return false;
    }

    protected Double getAdditionalValue(ColId col, Long sourceId) {
        for (AdditionalCols add : additional) {
            if (add != null && add.hasCol(col)) {
                Double d = add.get(col, sourceId);
                if (d != null)
                    return d;
            }
        }
        return null;
    }

    /**
     * Files or folders with optionally gzipped CSVs containing additional columns to be matched by sourceid with the main catalog. Column names must comport
     * to {@link ColId}
     */
    protected String[] additionalFiles = null;
    private String additionalSplit = ",";

    /**
     * RUWE cap value. Will accept all stars with star_ruwe <= ruwe
     */
    protected Double ruwe = Double.NaN;

    /**
     * Distance cap in parsecs
     */
    protected double distCap = Double.MAX_VALUE;

    /**
     * <p>
     * The loader will only load stars for which the parallax error is
     * at most the percentage given here, in [0..1]. Faint stars (gmag >= 13.1)
     * More specifically, the following must be met:
     * </p>
     * <code>pllx_err &lt; pllx * pllxErrFactor</code>
     **/
    protected double parallaxErrorFactorFaint = 0.125;

    /**
     * <p>
     * The loader will only load stars for which the parallax error is
     * at most the percentage given here, in [0..1]. Bright stars (gmag < 13.1)
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

    public AbstractStarGroupDataProvider() {
        super();
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
        list = new Array<>(elems);
    }

    protected void initLists() {
        initLists(1000);
        sphericalPositions = new LongMap<>();
        colors = new LongMap<>();
    }

    @Override
    public Array<ParticleBean> loadData(String file) {
        return loadData(file, 1.0f);
    }

    @Override
    public Array<ParticleBean> loadData(String file, double factor) {
        return loadData(file, factor, true);
    }

    @Override
    public Array<ParticleBean> loadData(InputStream is, double factor) {
        return loadData(is, factor, true);
    }

    @Override
    public Array<ParticleBean> loadDataMapped(String file, double factor) {
        return loadDataMapped(file, factor, true);
    }


    /**
     * Returns whether the star must be loaded or not
     *
     * @param id The star ID
     * @return Whether the star with the given ID must be loaded
     */
    protected boolean mustLoad(long id) {
        return mustLoadIds == null || mustLoadIds.contains(id);
    }

    /**
     * Checks whether the parallax is accepted or not.
     * <p>
     * <b>If adaptive is not enabled:</b>
     * <pre>
     * accepted = pllx > 0 && pllx_err < pllx * pllx_err_factor && pllx_err <= 1
     * </pre>
     * </p>
     * <p>
     * <b>If adaptive is enabled:</b>
     * <pre>
     * accepted = pllx > 0 && pllx_err < pllx * max(0.5, pllx_err_factor) && pllx_err <= 1, if apparent_magnitude < 13.2
     * accepted = pllx > 0 && pllx_err < pllx * pllx_err_factor && pllx_err <= 1, otherwise
     * </pre>
     * </p>
     *
     * @param appmag  Apparent magnitude of star
     * @param pllx    Parallax of star
     * @param pllxerr Parallax error of star
     * @return True if parallax is accepted, false otherwise
     */
    protected boolean acceptParallax(double appmag, double pllx, double pllxerr) {
        // If geometric distances are present, always accept, we use distances directly
        if (hasAdditionalColumn(ColId.geodist))
            return true;

        if (adaptiveParallax && appmag < 13.1) {
            return pllx >= 0 && pllxerr < pllx * parallaxErrorFactorBright && pllxerr <= 1;
        } else {
            return pllx >= 0 && pllxerr < pllx * parallaxErrorFactorFaint && pllxerr <= 1;
        }
    }

    protected float getRuweValue(long sourceId, String[] tokens) {
        if (hasCol(ColId.ruwe)) {
            return Parser.parseFloat(tokens[idx(ColId.ruwe)]);
        } else if (hasAdditional(ColId.ruwe, sourceId)) {
            return getAdditionalValue(ColId.ruwe, sourceId).floatValue();
        }
        return Float.NaN;
    }

    /**
     * Gets the distance in parsecs to the star from the additional columns
     * map, if it exists. Otherwise, it returns a negative value.
     *
     * @param sourceId The source id of the source
     * @return The geometric distance in parsecs if it exists, -1 otherwise.
     */
    protected double getGeoDistance(long sourceId) {
        if (hasAdditional(ColId.geodist, sourceId)) {
            return getAdditionalValue(ColId.geodist, sourceId);
        }
        return -1;
    }

    /**
     * Checks whether to accept the distance
     *
     * @param distance Distance in parsecs
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
     * @return The number of lines
     * @throws IOException
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

    protected void dumpToDisk(Array<StarBean> data, String filename, String format) {
        if (format.equals("bin"))
            dumpToDiskBin(data, filename, false);
        else if (format.equals("csv"))
            dumpToDiskCsv(data, filename);
    }

    protected void dumpToDiskBin(Array<StarBean> data, String filename, boolean serialized) {
        if (serialized) {
            // Use java serialization method
            List<StarBean> l = new ArrayList<StarBean>(data.size);
            for (StarBean p : data)
                l.add(p);

            try {
                ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename));
                oos.writeObject(l);
                oos.close();
                logger.info("File " + filename + " written with " + l.size() + " stars");
            } catch (Exception e) {
                logger.error(e);
            }
        } else {
            // Use own binary format
            BinaryDataProvider io = new BinaryDataProvider();
            try {
                int n = data.get(0).data.length;
                io.writeData(data, new FileOutputStream(filename));
                logger.info("File " + filename + " written with " + n + " stars");
            } catch (Exception e) {
                logger.error(e);
            }
        }
    }

    protected void dumpToDiskCsv(Array<StarBean> data, String filename) {
        String sep = "' ";
        try {
            PrintWriter writer = new PrintWriter(filename, StandardCharsets.UTF_8);
            writer.println("name(s), x[km], y[km], z[km], absmag, appmag, r, g, b");
            Vector3d gal = new Vector3d();
            int n = 0;
            for (StarBean star : data) {
                float[] col = colors.get(star.id);
                double x = star.z();
                double y = -star.x();
                double z = star.y();
                gal.set(x, y, z).scl(Constants.U_TO_KM);
                gal.mul(Coordinates.equatorialToGalactic());
                writer.println(TextUtils.concatenate("|", star.names) + sep + x + sep + y + sep + z + sep + star.absmag() + sep + star.appmag() + sep + col[0] + sep + col[1] + sep + col[2]);
                n++;
            }
            writer.close();
            logger.info("File " + filename + " written with " + n + " stars");
        } catch (Exception e) {
            logger.error(e);
        }
    }

    public void setFileNumberCap(int cap) {
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
        this.additionalFiles = additionalFiles.split(",");
        if (additionalFiles != null && this.additionalFiles.length > 0)
            loadAdditional();
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
        for(String additionalFile : additionalFiles) {
            additionalSplit = ",";
            AdditionalCols addit = new AdditionalCols();
            addit.indices = new HashMap<>();
            addit.values = new LargeLongMap<>(10);

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
            int nfiles = files.length;
            int mod = nfiles / 20;
            int i = 1;
            for (File file : files) {
                if (nfiles > 60 && i % mod == 0) {
                    logger.info("Loading file " + i + "/" + nfiles);
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
     * @param f The path
     * @param addit The {@link AdditionalCols} instance
     * @throws IOException
     * @throws RuntimeException
     */
    private void loadAdditionalFile(Path f, AdditionalCols addit) throws IOException, RuntimeException {
        InputStream data = new FileInputStream(f.toFile());
        if (f.toString().endsWith(".gz"))
            data = new GZIPInputStream(data);
        BufferedReader br = new BufferedReader(new InputStreamReader(data));
        // Read header
        String[] header = br.readLine().strip().split(additionalSplit);
        if (header.length < 2) {
            additionalSplit = "\\s+";
            header = br.readLine().strip().split(additionalSplit);
        }
        int i = 0;
        for (String col : header) {
            col = col.strip();
            if (i == 0 && !col.equals(ColId.sourceid.name())) {
                logger.error("First column: " + col + ", should be: " + ColId.sourceid.name());
                throw new RuntimeException("Additional columns file must contain a sourceid in the first column");
            }
            if (i > 0 && !addit.indices.containsKey(col)) {
                addit.indices.put(col, i - 1);
            }
            i++;
        }
        int n = header.length - 1;
        String line;
        i = 0;
        try {
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(additionalSplit);
                Long sourceId = Parser.parseLong(tokens[0].trim());
                double[] vals = new double[n];
                for (int j = 1; j <= n; j++) {
                    if (tokens[j] != null && !tokens[j].strip().isBlank()) {
                        Double val = Parser.parseDouble(tokens[j].strip());
                        vals[j - 1] = val;
                    } else {
                        // No value
                        vals[j - 1] = Double.NaN;
                    }
                }
                addit.values.put(sourceId, vals);
                i++;
            }
            br.close();
        } catch (Exception e) {
            logger.error(e);
            br.close();
        }
    }

}
