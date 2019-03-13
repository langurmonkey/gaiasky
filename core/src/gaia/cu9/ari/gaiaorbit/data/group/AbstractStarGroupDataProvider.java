package gaia.cu9.ari.gaiaorbit.data.group;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.LongMap;
import gaia.cu9.ari.gaiaorbit.scenegraph.StarGroup.StarBean;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.LargeLongMap;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.parse.Parser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

public abstract class AbstractStarGroupDataProvider implements IStarGroupDataProvider {
    protected static Log logger = Logger.getLogger(AbstractStarGroupDataProvider.class);

    protected Array<StarBean> list;
    protected LongMap<double[]> sphericalPositions;
    protected LongMap<float[]> colors;
    protected long[] countsPerMag;
    protected LargeLongMap<Double> geoDistances = null;
    protected LargeLongMap<Float> ruweValues = null;
    protected Set<Long> mustLoadIds = null;

    /**
     * Points to the location of a file or directory which contains a set of <sourceId, distance[pc]>
     */
    protected String geoDistFile = null;

    /**
     * Location of gzipped file with <sourceId, RUWE>
     */
    protected String ruweFile = null;

    /**
     * RUWE cap value. Will accept all stars with star_ruwe <= ruwe
     */
    protected Double ruwe = Double.NaN;

    /**
     * Errors (negative or nan values) reading geometric distance files
     */
    protected long geoDistErrors = 0;

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
     *
     * @param elems
     */
    protected void initLists(int elems) {
        list = new Array<StarBean>(elems);
    }

    protected void initLists() {
        initLists(1000);
        sphericalPositions = new LongMap<double[]>();
        colors = new LongMap<float[]>();
    }

    /**
     * Returns whether the star must be loaded or not
     * @param id The star ID
     * @return Whether the star with the given ID must be loaded
     */
    protected boolean mustLoad(long id){
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
        if (geoDistances != null)
            return true;

        if (adaptiveParallax && appmag < 13.1) {
            return pllx >= 0 && pllxerr < pllx * parallaxErrorFactorBright && pllxerr <= 1;
        } else {
            return pllx >= 0 && pllxerr < pllx * parallaxErrorFactorFaint && pllxerr <= 1;
        }
    }

    protected float getRuweValue(long sourceId){
        if(ruweValues != null && ruweValues.containsKey(sourceId))
            return ruweValues.get(sourceId);
        return Float.NaN;
    }

    /**
     * Gets the distance in parsecs to the star from the geometric distances
     * map, if it exists. Otherwise, it returns a negative value.
     *
     * @param sourceId The source id of the source
     * @return The geometric distance in parsecs if it exists, -1 otherwise.
     */
    protected double getGeoDistance(long sourceId) {
        if (geoDistances != null && geoDistances.containsKey(sourceId))
            return geoDistances.get(sourceId);
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

    protected boolean hasGeoDistance(long sourceId) {
        if (geoDistances != null && geoDistances.containsKey(sourceId))
            return true;
        return false;
    }

    protected boolean hasGeoDistances() {
        return geoDistances != null && !geoDistances.isEmpty();
    }

    protected int countLines(FileHandle f) throws IOException {
        InputStream is = new BufferedInputStream(f.read());
        return countLines(is);
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
            PrintWriter writer = new PrintWriter(filename, "UTF-8");
            writer.println("name, x[km], y[km], z[km], absmag, appmag, r, g, b");
            Vector3d gal = new Vector3d();
            int n = 0;
            for (StarBean star : data) {
                float[] col = colors.get(star.id);
                double x = star.z();
                double y = -star.x();
                double z = star.y();
                gal.set(x, y, z).scl(Constants.U_TO_KM);
                gal.mul(Coordinates.equatorialToGalactic());
                writer.println(star.name + sep + x + sep + y + sep + z + sep + star.absmag() + sep + star.appmag() + sep + col[0] + sep + col[1] + sep + col[2]);
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
    public void setGeoDistancesFile(String geoDistFile) {
        this.geoDistFile = geoDistFile;
        if (geoDistFile != null)
            loadGeometricDistances();
    }

    @Override
    public void setDistanceCap(double distCap) {
        this.distCap = distCap;
    }

    @Override
    public void setRUWEFile(String RUWEFile) {
        this.ruweFile = RUWEFile;
        if(this.ruweFile != null)
            loadRuweFile();
    }

    @Override
    public void setRUWECap(double RUWE) {
        this.ruwe = RUWE;
    }

    private void loadRuweFile() {
        if(ruweFile != null) {
            ruweValues = new LargeLongMap<Float>(20);
            logger.info("Loading RUWE values from " + ruweFile);

            Path f = Paths.get(ruweFile);
            try {
                loadRuweFile(f);
                logger.info(ruweValues.size() + " RUWE values loaded");
            } catch (Exception e) {
                logger.error(e, "Loading RUWE file failed: " + f.toString());
            }

        }
    }

    private void loadRuweFile(Path p) throws IOException {
            InputStream fileStream = new FileInputStream(p.toFile());
            InputStream gzipStream = new GZIPInputStream(fileStream);
            BufferedReader br = new BufferedReader(new InputStreamReader(gzipStream));
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split("\\s+");
                Long sourceId = Parser.parseLong(tokens[0].trim());
                Double ruweVal = Parser.parseDouble(tokens[1].trim());
                ruweValues.put(sourceId, ruweVal.floatValue());

            }
            br.close();
    }

    private void loadGeometricDistances() {
        geoDistances = new LargeLongMap<Double>(10);
        geoDistErrors = 0;

        logger.info("Loading geometric distances from " + geoDistFile);

        Path f = Paths.get(geoDistFile);
        loadGeometricDistances(f);

        logger.info(geoDistances.size() + " geometric distances loaded (" + geoDistErrors + " negative or nan values)");
    }

    private void loadGeometricDistances(Path f) {
        if (Files.isDirectory(f, LinkOption.NOFOLLOW_LINKS)) {
            File[] files = f.toFile().listFiles();
            int nfiles = files.length;
            int mod = nfiles / 20;
            int i = 1;
            for (File file : files) {
                if (i % mod == 0) {
                    logger.info("Loading file " + i + "/" + nfiles);
                }
                loadGeometricDistances(file.toPath());
                i++;
            }
        } else {
            try {
                loadGeometricDistanceFile(f);
            } catch (Exception e) {
                logger.error(e, "Loading failed: " + f.toString());
            }
        }
    }

    private void loadGeometricDistanceFile(Path f) throws IOException, RuntimeException {
        InputStream data = new FileInputStream(f.toFile());
        BufferedReader br = new BufferedReader(new InputStreamReader(data));
        // Skip header
        br.readLine();
        String line;
        int i = 0;
        try {
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split("\\s+");
                Long sourceId = Parser.parseLong(tokens[0].trim());
                Double dist = Parser.parseDouble(tokens[1].trim());
                if (!dist.isNaN() && dist >= 0) {
                    geoDistances.put(sourceId, dist);
                } else {
                    logger.debug("Distance " + i + " is NaN or negative: " + dist + " (file " + f.toString() + ")");
                    geoDistErrors++;
                }
                i++;
            }
            br.close();
        } catch (Exception e) {
            logger.error(e);
            br.close();
        }
    }
}
