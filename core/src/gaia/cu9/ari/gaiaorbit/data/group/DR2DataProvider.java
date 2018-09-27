package gaia.cu9.ari.gaiaorbit.data.group;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.desktop.util.SysUtils;
import gaia.cu9.ari.gaiaorbit.scenegraph.ParticleGroup.ParticleBean;
import gaia.cu9.ari.gaiaorbit.scenegraph.StarGroup.StarBean;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.color.ColourUtils;
import gaia.cu9.ari.gaiaorbit.util.coord.AstroUtils;
import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;
import gaia.cu9.ari.gaiaorbit.util.io.ByteBufferInputStream;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.parse.Parser;

/**
 * Loads the DR2 catalog in CSV format
 * 
 * Source position and corresponding errors are in radians, parallax in mas and
 * proper motion in mas/yr.
 * 
 * @author Toni Sagrista
 *
 */
public class DR2DataProvider extends AbstractStarGroupDataProvider {
    private static final Log logger = Logger.getLogger(DR2DataProvider.class);
    private static final String comma = ",";

    private static final String separator = comma;

    /**
     * Maximum file count to load. 0 or negative for unlimited
     */
    private int fileNumberCap = -1;
    /** Whether to load the sourceId->HIP correspondences file **/
    public boolean useHIP = false;

    /**
     * INDICES:
     * 
     * source_id ra[deg] dec[deg] parallax[mas] ra_err[mas] dec_err[mas]
     * pllx_err[mas] mualpha[mas/yr] mudelta[mas/yr] radvel[km/s]
     * mualpha_err[mas/yr] mudelta_err[mas/yr] radvel_err[km/s] gmag[mag]
     * bp[mag] rp[mag] ref_epoch[julian years]
     */
    private static final int SOURCE_ID = 0;
    private static final int RA = 1;
    private static final int DEC = 2;
    private static final int PLLX = 3;
    private static final int RA_ERR = 4;
    private static final int DEC_ERR = 5;
    private static final int PLLX_ERR = 6;
    private static final int MUALPHA = 7;
    private static final int MUDELTA = 8;
    private static final int RADVEL = 9;
    private static final int MUALPHA_ERR = 10;
    private static final int MUDELTA_ERR = 11;
    private static final int RADVEL_ERR = 12;
    private static final int G_MAG = 13;
    private static final int BP_MAG = 14;
    private static final int RP_MAG = 15;
    private static final int REF_EPOCH = 16;
    private static final int TEFF = 17;
    private static final int RADIUS = 18;
    private static final int A_G = 19;
    private static final int E_BP_MIN_RP = 20;

    private static final int[] indices = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 };

    public DR2DataProvider() {
        super();
        countsPerMag = new long[22];
    }

    public void setFileNumberCap(int cap) {
        fileNumberCap = cap;
    }

    public Array<StarBean> loadData(String file) {
        return loadData(file, 1d);
    }

    public Array<StarBean> loadData(String file, double factor) {
        initLists(10000000);

        FileHandle f = GlobalConf.data.dataFileHandle(file);
        if (f.isDirectory()) {
            // Recursive
            FileHandle[] files = f.list();
            Arrays.sort(files, (FileHandle a, FileHandle b) -> {
                return a.name().compareTo(b.name());
            });
            int fn = 0;
            for (FileHandle fh : files) {
                loadDataMapped(fh.path(), factor, fn + 1);
                //loadFileFh(fh, factor, fn + 1);
                fn++;
                if (fileNumberCap > 0 && fn >= fileNumberCap)
                    break;
            }
        } else if (f.name().endsWith(".csv") || f.name().endsWith(".gz")) {
            loadDataMapped(file, factor, 1);
            //loadFileFh(f, factor, 1);
        } else {
            logger.warn("File skipped: " + f.path());
        }
        logger.info(I18n.bundle.format("notif.nodeloader", list.size, f.path()));
        return list;
    }

    public Array<StarBean> loadData(InputStream is, double factor) {
        initLists(100000);

        loadFileIs(is, factor, new LongWrap(0l), new LongWrap(0l));

        return list;
    }

    public void loadFileFh(FileHandle fh, double factor, int fileNumber) {
        //logger.info(this.getClass().getSimpleName(), I18n.bundle.format("notif.datafile", fh.path()));
        boolean gz = fh.name().endsWith(".gz");

        // Simple case
        InputStream data = fh.read();

        if (gz) {
            try {
                data = new GZIPInputStream(data);
            } catch (IOException e) {
                logger.error(e);
                return;
            }
        }
        LongWrap addedStars = new LongWrap(0l);
        LongWrap discardedStars = new LongWrap(0l);
        loadFileIs(data, factor, addedStars, discardedStars);
        logger.info(fileNumber + " - " + fh.name() + " --> " + addedStars.value + "/" + (addedStars.value + discardedStars.value) + " stars (" + (100 * addedStars.value / (addedStars.value + discardedStars.value)) + "%)");
    }

    public void loadFileIs(InputStream is, double factor, LongWrap addedStars, LongWrap discardedStars) {
        // Simple case
        InputStream data = is;
        BufferedReader br = new BufferedReader(new InputStreamReader(data));
        try {
            int i = 0;
            String line;
            while ((line = br.readLine()) != null) {
                // Skip first line
                if (i > 0) {
                    // Add star
                    if (addStar(line)) {
                        addedStars.value++;
                    } else {
                        discardedStars.value++;
                    }
                }
                i++;
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
     * @param line
     *            The string line
     * @return True if star was added, false otherwise
     */
    private boolean addStar(String line) {
        String[] tokens = line.split(separator);
        double[] point = new double[StarBean.SIZE];

        // Check that parallax exists (5-param solution), otherwise we have no distance
        if (!tokens[indices[PLLX]].isEmpty()) {
            // Add the zero point to the parallax
            double pllx = Parser.parseDouble(tokens[indices[PLLX]]) + parallaxZeroPoint;
            //pllx = 0.0200120072;
            double pllxerr = Parser.parseDouble(tokens[indices[PLLX_ERR]]);
            double appmag = Parser.parseDouble(tokens[indices[G_MAG]]);

            // Keep only stars with relevant parallaxes
            if (acceptParallax(appmag, pllx, pllxerr)) {
                /** ID **/
                long sourceid = Parser.parseLong(tokens[indices[SOURCE_ID]]);

                /** DISTANCE **/
                double distpc = (1000d / pllx);
                double geodistpc = getGeoDistance(sourceid);

                // If we have geometric distances, we only accept those, otherwise, accept all
                if (!hasGeoDistances() || (hasGeoDistances() && hasGeoDistance(sourceid))) {
                    distpc = geodistpc > 0 ? geodistpc : distpc;

                    if (acceptDistance(distpc)) {

                        double dist = distpc * Constants.PC_TO_U;

                        /** NAME **/
                        String name = String.valueOf((long) sourceid);

                        /** RA and DEC **/
                        double ra = Parser.parseDouble(tokens[indices[RA]]);
                        double dec = Parser.parseDouble(tokens[indices[DEC]]);
                        double rarad = Math.toRadians(ra);
                        double decrad = Math.toRadians(dec);
                        Vector3d pos = Coordinates.sphericalToCartesian(rarad, decrad, dist, new Vector3d());

                        /** PROPER MOTIONS in mas/yr **/
                        double mualphastar = Parser.parseDouble(tokens[indices[MUALPHA]]);
                        double mudelta = Parser.parseDouble(tokens[indices[MUDELTA]]);
                        //double mualpha = mualphastar / Math.cos(decrad);

                        /** RADIAL VELOCITY in km/s **/
                        double radvel = Parser.parseDouble(tokens[indices[RADVEL]]);
                        if (Double.isNaN(radvel)) {
                            radvel = 0;
                        }

                        /** PROPER MOTION VECTOR **/
                        Vector3d pm = AstroUtils.properMotionsToCartesian(mualphastar, mudelta, radvel, rarad, decrad, distpc);

                        // Line of sight extinction in the G band
                        double ag = 0;
                        // Galactic latitude in radians
                        double magcorraux = 0;
                        if (magCorrections) {
                            if (tokens.length >= 20 && !tokens[indices[A_G]].isEmpty()) {
                                // Take extinction from database
                                ag = Parser.parseDouble(tokens[indices[A_G]]);
                            } else {
                                // Compute extinction analitically
                                Vector3d posgal = new Vector3d(pos);
                                posgal.mul(Coordinates.eqToGal());
                                Vector3d posgalsph = Coordinates.cartesianToSpherical(posgal, new Vector3d());
                                double b = posgalsph.y;
                                magcorraux = Math.min(distpc, 150d / Math.abs(Math.sin(b)));
                                ag = magcorraux * 5.9e-4;
                            }
                        }
                        // Apply extinction
                        appmag -= ag;
                        double absmag = (appmag - 2.5 * Math.log10(Math.pow(distpc / 10d, 2d)));
                        double flux = Math.pow(10, -absmag / 2.5f);
                        double size = Math.min((Math.pow(flux, 0.5f) * Constants.PC_TO_U * 0.16f), 1e9f) / 1.5;
                        //double radius = tokens.length >= 19 && !tokens[indices[RADIUS]].isEmpty() ? Parser.parseDouble(tokens[indices[RADIUS]]) * Constants.Ro_TO_U : size * Constants.STAR_SIZE_FACTOR;

                        /** COLOR, we use the tycBV map if present **/

                        // Reddening
                        double ebr = 0;
                        if (magCorrections) {
                            if (tokens.length >= 21 && !tokens[indices[E_BP_MIN_RP]].isEmpty()) {
                                // Take reddening from table
                                ebr = Parser.parseDouble(tokens[indices[E_BP_MIN_RP]]);
                            } else {
                                // Compute reddening analtytically
                                ebr = magcorraux * 2.9e-4;
                            }
                        }

                        double xp = 0;
                        if (indices[BP_MAG] >= 0 && indices[RP_MAG] >= 0) {
                            // Real TGAS
                            float bp = new Double(Parser.parseDouble(tokens[indices[BP_MAG]].trim())).floatValue();
                            float rp = new Double(Parser.parseDouble(tokens[indices[RP_MAG]].trim())).floatValue();
                            xp = bp - rp - ebr;
                        } else {
                            // Use color value in BP
                            xp = new Double(Parser.parseDouble(tokens[indices[BP_MAG]].trim())).floatValue();
                        }

                        // See Gaia broad band photometry (https://doi.org/10.1051/0004-6361/201015441)
                        double teff;
                        if (tokens.length > 18 && !tokens[indices[TEFF]].isEmpty()) {
                            // Use database Teff
                            teff = Parser.parseDouble(tokens[indices[TEFF]]);
                        } else {
                            // Compute Teff from XP color
                            if (xp <= 1.5) {
                                teff = Math.pow(10.0, 3.999 - 0.654 * xp + 0.709 * Math.pow(xp, 2.0) - 0.316 * Math.pow(xp, 3.0));
                            } else {
                                // We do a linear regression between [1.5, 3521.6] and [15, 3000]
                                teff = MathUtilsd.lint(xp, 1.5, 15, 3521.6, 3000);
                            }
                        }
                        float[] rgb = ColourUtils.teffToRGB(teff);
                        double col = Color.toFloatBits(rgb[0], rgb[1], rgb[2], 1.0f);

                        point[StarBean.I_HIP] = -1;
                        point[StarBean.I_TYC1] = -1;
                        point[StarBean.I_TYC2] = -1;
                        point[StarBean.I_TYC3] = -1;
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

                        list.add(new StarBean(point, sourceid, name));

                        int appclmp = (int) MathUtilsd.clamp(appmag, 0, 21);
                        countsPerMag[(int) appclmp] += 1;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private class LongWrap {
        public Long value;

        public LongWrap(Long val) {
            this.value = val;
        }

        @Override
        public String toString() {
            return Long.toString(value);
        }

    }

    @Override
    public Array<? extends ParticleBean> loadDataMapped(String file, double factor) {
        return loadDataMapped(file, factor, 0);
    }

    /**
     * Uses memory mapped files to load catalog files.
     * This is not working right now
     * @param file
     * @param factor
     * @param fileNumber
     * @return
     */
    public Array<? extends ParticleBean> loadDataMapped(String file, double factor, int fileNumber) {
        //logger.info(this.getClass().getSimpleName(), I18n.bundle.format("notif.datafile", fh.path()));
        boolean gz = file.endsWith(".gz");
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
            LongWrap addedStars = new LongWrap(0l);
            LongWrap discardedStars = new LongWrap(0l);
            loadFileIs(data, factor, addedStars, discardedStars);
            logger.info(fileNumber + " - " + file + " --> " + addedStars.value + "/" + (addedStars.value + discardedStars.value) + " stars (" + (100 * addedStars.value / (addedStars.value + discardedStars.value)) + "%)");

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

}
