/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import gaiasky.scenegraph.ParticleGroup.ParticleBean;
import gaiasky.scenegraph.StarGroup.StarBean;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.format.INumberFormat;
import gaiasky.util.format.NumberFormatFactory;
import gaiasky.util.io.ByteBufferInputStream;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Vector3d;
import gaiasky.util.parse.Parser;
import gaiasky.util.ucd.UCD;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

/**
 * Loads star catalogs in CSV format
 * <p>
 * Source position and corresponding errors are in radians, parallax in mas and
 * proper motion in mas/yr.
 *
 * @author Toni Sagrista
 */
public class CsvCatalogDataProvider extends AbstractStarGroupDataProvider {
    private static final Log logger = Logger.getLogger(CsvCatalogDataProvider.class);
    private static final String comma = ",";

    private static final String separator = comma;

    /**
     * Maximum file count to load. 0 or negative for unlimited
     */
    private int fileNumberCap = -1;

    /**
     * Number formatter
     */
    private final INumberFormat nf;

    /**
     * Load lines in parallel
     */
    private boolean parallel = true;

    // Buffer in number of lines
    private int parallelBufferSize = 750000;

    public CsvCatalogDataProvider() {
        super();
        indexMap = new HashMap<>();
        countsPerMag = new long[22];
        nf = NumberFormatFactory.getFormatter("###.##");
    }

    /**
     * Default columns for DR2
     */
    public void setDR2Columns() {
        setColumns(ColId.sourceid.toString(), ColId.ra.toString(), ColId.dec.toString(), ColId.pllx.toString(), ColId.ra_err.toString(), ColId.dec_err.toString(), ColId.pllx_err.toString(), ColId.pmra.toString(), ColId.pmdec.toString(), ColId.radvel.toString(), ColId.pmra_err.toString(), ColId.pmdec_err.toString(), ColId.pllx_err.toString(), ColId.gmag.toString(), ColId.bpmag.toString(), ColId.rpmag.toString(), ColId.ref_epoch.toString());
    }

    /**
     * Set the CSV columns to work out the indices, as a comma-separated string
     *
     * @param columns Columns
     */
    public void setColumns(String columns) {
        if (columns == null || columns.length() == 0) {
            throw new RuntimeException("Please provide a list of columns");
        }
        String[] cols = columns.strip().split(comma);
        setColumns(cols);
    }

    /**
     * Set the CSV columns to work out the indices, as a comma-separated string
     *
     * @param cols The columns
     */
    public void setColumns(String... cols) {
        int c = 0;
        ColId[] colIds = ColId.values();
        for (String col : cols) {
            for (ColId colId : colIds) {
                if (!col.strip().isBlank() && col.strip().equals(colId.toString())) {
                    indexMap.put(colId, c);
                    break;
                }
            }
            c++;
        }
    }

    public void setFileNumberCap(int cap) {
        fileNumberCap = cap;
    }

    public List<ParticleBean> loadData(String file) {
        return loadData(file, 1d);
    }

    public List<ParticleBean> loadData(String file, double factor, boolean compat) {
        initLists(1000000);

        FileHandle f = GlobalConf.data.dataFileHandle(file);
        if (f.isDirectory()) {
            long numFiles = 0;
            try {
                if (fileNumberCap > 0)
                    numFiles = Math.min(GlobalResources.fileCount(Paths.get(file)), fileNumberCap);
                else
                    numFiles = GlobalResources.fileCount(Paths.get(file));
            } catch (IOException e) {
                logger.error("Error counting files in dir: " + file);
            }
            // Recursive
            FileHandle[] files = f.list();
            Arrays.sort(files, Comparator.comparing(FileHandle::name));
            int fn = 0;
            for (FileHandle fh : files) {
                loadDataMapped(fh.path(), factor, fn + 1, numFiles);
                //loadFileFh(fh, factor, fn + 1);
                fn++;
                if (fileNumberCap > 0 && fn >= fileNumberCap)
                    break;
            }
        } else if (f.name().endsWith(".csv") || f.name().endsWith(".gz")) {
            loadDataMapped(file, factor, 1, 1);
        } else {
            logger.warn("File skipped: " + f.path());
        }
        logger.info(I18n.bundle.format("notif.nodeloader", list.size(), f.path()));
        return list;
    }

    public List<ParticleBean> loadData(InputStream is, double factor, boolean compat) {
        initLists(100000);

        loadFileIs(is, factor, new AtomicLong(0l), new AtomicLong(0l));

        return list;
    }

    public void loadFileIs(InputStream is, double factor, AtomicLong addedStars, AtomicLong discardedStars) {
        // Simple case
        InputStream data = is;
        BufferedReader br = new BufferedReader(new InputStreamReader(data));
        Consumer<String> c = (l) -> {
            if (addStar(l))
                addedStars.incrementAndGet();
            else
                discardedStars.incrementAndGet();
        };
        try {
            List<String> lineBuffer = new ArrayList<>(parallelBufferSize);
            int i = 0;
            String line;
            while ((line = br.readLine()) != null) {
                // Add to buffer
                if (i > 0)
                    lineBuffer.add(line);
                if (lineBuffer.size() >= parallelBufferSize) {
                    if (parallel)
                        lineBuffer.parallelStream().forEach(c);
                    else
                        lineBuffer.stream().forEach(c);
                    lineBuffer.clear();
                }
                i++;
            }
            // Flush resting
            if (lineBuffer.size() > 0) {
                if (parallel)
                    lineBuffer.parallelStream().forEach(c);
                else
                    lineBuffer.stream().forEach(c);
                lineBuffer.clear();
            }
        } catch (IOException e) {
            logger.error(e);
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                logger.error(e);
            }
        }
    }

    /**
     * Adds the star if it meets the criteria.
     *
     * @param line The string line
     * @return True if star was added, false otherwise
     */
    private boolean addStar(String line) {
        String[] tokens = line.split(separator);
        double[] point = new double[StarBean.SIZE + 3];

        // Check that parallax exists (5-param solution), otherwise we have no distance
        if (!tokens[idx(ColId.pllx)].isEmpty()) {
            /** Extra attributes **/
            Map<UCD, Double> extra = new HashMap<>();

            /** ID **/
            long sourceid = Parser.parseLong(tokens[idx(ColId.sourceid)]);
            boolean mustLoad = mustLoad(sourceid);

            /** PARALLAX **/
            // Add the zero point to the parallax
            double pllx = Parser.parseDouble(tokens[idx(ColId.pllx)]) + parallaxZeroPoint;
            double pllxerr = Parser.parseDouble(tokens[idx(ColId.pllx_err)]);
            double appmag = Parser.parseDouble(tokens[idx(ColId.gmag)]);
            extra.put(new UCD("pllx_err", ColId.pllx_err.toString(), "", 0), pllxerr);

            // Keep only stars with relevant parallaxes
            if (mustLoad || acceptParallax(appmag, pllx, pllxerr)) {

                /** DISTANCE **/
                double distpc = (1000d / pllx);
                double geodistpc = getGeoDistance(sourceid);

                float ruweVal = getRuweValue(sourceid, tokens);
                if (!mustLoad && !ruwe.isNaN()) {
                    // RUWE test!
                    if (ruweVal > ruwe) {
                        // Do not accept
                        return false;
                    }
                }
                // Add ruwe to extra
                if (!Float.isNaN(ruweVal)) {
                    extra.put(new UCD("ruwe", ColId.ruwe.toString(), "", 0), (double) ruweVal);
                }

                // If we have geometric distances, we only accept those, otherwise, accept all
                boolean geodist = hasAdditionalColumn(ColId.geodist);
                if (mustLoad || !geodist || (geodist && hasAdditional(ColId.geodist, sourceid))) {
                    distpc = geodistpc > 0 ? geodistpc : distpc;

                    if (mustLoad || acceptDistance(distpc)) {

                        double dist = distpc * Constants.PC_TO_U;

                        /** NAME **/
                        String name = String.valueOf(sourceid);

                        /** RA and DEC **/
                        double ra = Parser.parseDouble(tokens[idx(ColId.ra)]);
                        double dec = Parser.parseDouble(tokens[idx(ColId.dec)]);
                        double rarad = Math.toRadians(ra);
                        double decrad = Math.toRadians(dec);
                        // If distance is negative due to mustLoad, we need to be able to retrieve sph pos later on, so we use 1 m to mark it
                        Vector3d pos = Coordinates.sphericalToCartesian(rarad, decrad, Math.max(dist, NEGATIVE_DIST), new Vector3d());

                        /** PROPER MOTIONS in mas/yr **/
                        double mualphastar = Parser.parseDouble(tokens[idx(ColId.pmra)]);
                        double mudelta = Parser.parseDouble(tokens[idx(ColId.pmdec)]);

                        /** RADIAL VELOCITY in km/s **/
                        double radvel = Parser.parseDouble(tokens[idx(ColId.radvel)]);
                        if (Double.isNaN(radvel)) {
                            radvel = 0;
                        }

                        /** PROPER MOTION VECTOR **/
                        Vector3d pm = AstroUtils.properMotionsToCartesian(mualphastar, mudelta, radvel, rarad, decrad, distpc);

                        // Line of sight extinction in the G band
                        double ag = 0;
                        // Galactic latitude in radians
                        Vector3d posgal = new Vector3d(pos);
                        posgal.mul(Coordinates.eqToGal());
                        Vector3d posgalsph = Coordinates.cartesianToSpherical(posgal, new Vector3d());
                        double b = posgalsph.y;
                        double magcorraux = Math.min(distpc, 150d / Math.abs(Math.sin(b)));

                        if (magCorrections) {
                            if (hasCol(ColId.ag) && !tokens[idx(ColId.ag)].isEmpty()) {
                                // Take extinction from database
                                ag = Parser.parseDouble(tokens[idx(ColId.ag)]);
                            } else if (hasAdditional(ColId.ag, sourceid)) {
                                // Take extinction from additional file
                                ag = getAdditionalValue(ColId.ag, sourceid);
                            } else {
                                // Compute extinction analytically
                                ag = magcorraux * 5.9e-4;
                                // Limit to 3
                                ag = Math.min(ag, 3.2);
                            }
                        }
                        // Apply extinction
                        if (Double.isFinite(ag))
                            appmag -= ag;

                        double absmag = appmag - 5.0 * Math.log10((distpc <= 0 ? 10 : distpc)) + 5.0;
                        // Pseudo-luminosity. Usually L = L0 * 10^(-0.4*Mbol). We omit M0 and approximate Mbol = M
                        double pseudoL = Math.pow(10.0, -0.4 * absmag);
                        double sizeFactor = Nature.PC_TO_M * Constants.ORIGINAL_M_TO_U * 0.15;
                        double size = Math.min((Math.pow(pseudoL, 0.45) * sizeFactor), 1.0e10);
                        //double radius = tokens.length >= 19 && !tokens[IDX_RADIUS]].isEmpty() ? Parser.parseDouble(tokens[IDX_RADIUS]]) * Constants.Ro_TO_U : size * Constants.STAR_SIZE_FACTOR;

                        /** COLOR, we use the tycBV map if present **/
                        // Reddening
                        double ebr = 0;
                        if (magCorrections) {
                            if (hasCol(ColId.ebp_min_rp) && !tokens[idx(ColId.ebp_min_rp)].isEmpty()) {
                                // Take reddening from table
                                ebr = Parser.parseDouble(tokens[idx(ColId.ebp_min_rp)]);
                            } else if (hasAdditional(ColId.ebp_min_rp, sourceid)) {
                                // From additional
                                ebr = getAdditionalValue(ColId.ebp_min_rp, sourceid);
                            } else {
                                // Compute reddening analytically
                                ebr = magcorraux * 2.9e-4;
                                // Limit to 1.6
                                ebr = Math.min(ebr, 1.6);
                            }
                        }

                        // XP = BP - RP - Reddening
                        float bp = (float) Parser.parseDouble(tokens[idx(ColId.bpmag)].trim());
                        float rp = (float) Parser.parseDouble(tokens[idx(ColId.rpmag)].trim());
                        double xp = bp - rp - ebr;

                        // See Gaia broad band photometry (https://doi.org/10.1051/0004-6361/201015441)
                        double teff;
                        if (tokens.length > 18 && !tokens[idx(ColId.teff)].isEmpty()) {
                            // Use database Teff
                            teff = Parser.parseDouble(tokens[idx(ColId.teff)]);
                        } else {
                            // Compute Teff from XP color
                            if (xp <= 1.5) {
                                teff = Math.pow(10.0, 3.999 - 0.654 * xp + 0.709 * Math.pow(xp, 2.0) - 0.316 * Math.pow(xp, 3.0));
                            } else {
                                // We do a linear regression between [1.5, 3521.6] and [15, 3000]
                                teff = MathUtilsd.lint(xp, 1.5, 15, 3521.6, 3000);
                            }
                        }
                        float[] rgb = ColorUtils.teffToRGB(teff);
                        double col = Color.toFloatBits(rgb[0], rgb[1], rgb[2], 1.0f);

                        point[StarBean.I_HIP] = -1;
                        point[StarBean.I_X] = pos.x;
                        point[StarBean.I_Y] = pos.y;
                        point[StarBean.I_Z] = pos.z;
                        point[StarBean.I_PMX] = pm.x;
                        point[StarBean.I_PMY] = pm.y;
                        point[StarBean.I_PMZ] = pm.z;
                        point[StarBean.I_MUALPHA] = mualphastar;
                        point[StarBean.I_MUDELTA] = mudelta;
                        point[StarBean.I_RADVEL] = radvel;
                        point[StarBean.I_COL] = col;
                        point[StarBean.I_SIZE] = size;
                        //point[StarBean.I_RADIUS] = radius;
                        //point[StarBean.I_TEFF] = teff;
                        point[StarBean.I_APPMAG] = appmag;
                        point[StarBean.I_ABSMAG] = absmag;

                        list.add(new StarBean(point, sourceid, name, extra));

                        int appClamp = (int) MathUtilsd.clamp(appmag, 0, 21);
                        countsPerMag[appClamp] += 1;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public List<ParticleBean> loadDataMapped(String file, double factor, boolean compat) {
        return loadDataMapped(file, factor, -1, -1);
    }

    /**
     * Uses memory mapped files to load catalog files.
     *
     * @param file       The file to load
     * @param factor     Position factor
     * @param fileNumber File number
     * @param totalFiles Total number of files
     * @return
     */
    public List<ParticleBean> loadDataMapped(String file, double factor, int fileNumber, long totalFiles) {
        boolean gz = file.endsWith(".gz");
        String fileName = file.substring(file.lastIndexOf('/') + 1);
        FileChannel fc = null;
        try {
            fc = new RandomAccessFile(file, "r").getChannel();
            MappedByteBuffer mem = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            InputStream data = new ByteBufferInputStream(mem);

            if (gz) {
                try {
                    data = new GZIPInputStream(data);
                } catch (IOException e) {
                    logger.error(e);
                }
            }
            AtomicLong addedStars = new AtomicLong(0l);
            AtomicLong discardedStars = new AtomicLong(0l);
            loadFileIs(data, factor, addedStars, discardedStars);

            if (fileNumber >= 0 && totalFiles >= 0)
                logger.info(fileNumber + "/" + totalFiles + " (" + nf.format((double) fileNumber * 100d / (double) totalFiles) + "%): " + fileName + " --> " + addedStars.get() + "/" + (addedStars.get() + discardedStars.get()) + " stars (" + nf.format(100d * (double) addedStars.get() / (double) (addedStars.get() + discardedStars.get())) + "%)");
            else
                logger.info(fileName + " --> " + addedStars.get() + "/" + (addedStars.get() + discardedStars.get()) + " stars (" + nf.format(100d * (double) addedStars.get() / (double) (addedStars.get() + discardedStars.get())) + "%)");

            return list;
        } catch (Exception e) {
            logger.error(e);
        } finally {
            if (fc != null)
                try {
                    fc.close();
                } catch (Exception e) {
                    logger.error(e);
                }
        }
        return null;
    }

    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    public void setParallelBufferSize(int size) {
        this.parallelBufferSize = size;
    }
}
