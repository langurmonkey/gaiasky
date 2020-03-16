/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import gaiasky.scenegraph.ParticleGroup.ParticleBean;
import gaiasky.scenegraph.StarGroup.StarBean;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.color.ColourUtils;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.format.INumberFormat;
import gaiasky.util.format.NumberFormatFactory;
import gaiasky.util.io.ByteBufferInputStream;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Vector3d;
import gaiasky.util.parse.Parser;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.zip.GZIPInputStream;

/**
 * Loads the DR2 catalog in CSV format
 * <p>
 * Source position and corresponding errors are in radians, parallax in mas and
 * proper motion in mas/yr.
 *
 * @author Toni Sagrista
 */
public class DR2DataProvider extends AbstractStarGroupDataProvider {
    private static final Log logger = Logger.getLogger(DR2DataProvider.class);
    private static final String comma = ",";

    private static final String separator = comma;

    /**
     * INDICES:
     * <p>
     * source_id ra[deg] dec[deg] parallax[mas] ra_err[mas] dec_err[mas]
     * pllx_err[mas] mualpha[mas/yr] mudelta[mas/yr] radvel[km/s]
     * mualpha_err[mas/yr] mudelta_err[mas/yr] radvel_err[km/s] gmag[mag]
     * bp[mag] rp[mag] ref_epoch[julian years]
     */
    private static final int IDX_SOURCE_ID = 0;
    private static final int IDX_RA = 1;
    private static final int IDX_DEC = 2;
    private static final int IDX_PLLX = 3;
    private static final int IDX_RA_ERR = 4;
    private static final int IDX_DEC_ERR = 5;
    private static final int IDX_PLLX_ERR = 6;
    private static final int IDX_MUALPHA = 7;
    private static final int IDX_MUDELTA = 8;
    private static final int IDX_RADVEL = 9;
    private static final int IDX_MUALPHA_ERR = 10;
    private static final int IDX_MUDELTA_ERR = 11;
    private static final int IDX_RADVEL_ERR = 12;
    private static final int IDX_G_MAG = 13;
    private static final int IDX_BP_MAG = 14;
    private static final int IDX_RP_MAG = 15;
    private static final int IDX_REF_EPOCH = 16;
    private static final int IDX_TEFF = 17;
    private static final int IDX_RADIUS = 18;
    private static final int IDX_A_G = 19;
    private static final int IDX_E_BP_MIN_RP = 20;


    /**
     * Maximum file count to load. 0 or negative for unlimited
     */
    private int fileNumberCap = -1;

    /**
     * Number formatter
     */
    private INumberFormat nf;

    public DR2DataProvider() {
        super();
        countsPerMag = new long[22];
        nf = NumberFormatFactory.getFormatter("###.##");
    }

    public void setFileNumberCap(int cap) {
        fileNumberCap = cap;
    }

    public Array<ParticleBean> loadData(String file) {
        return loadData(file, 1d);
    }


    public Array<ParticleBean> loadData(String file, double factor) {
        initLists(10000000);

        FileHandle f = GlobalConf.data.dataFileHandle(file);
        if (f.isDirectory()) {
            long numFiles = 0;
            try {
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
        logger.info(I18n.bundle.format("notif.nodeloader", list.size, f.path()));
        return list;
    }

    public Array<ParticleBean> loadData(InputStream is, double factor) {
        initLists(100000);

        loadFileIs(is, factor, new LongWrap(0l), new LongWrap(0l));

        return list;
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
     * @param line The string line
     * @return True if star was added, false otherwise
     */
    private boolean addStar(String line) {
        String[] tokens = line.split(separator);
        double[] point = new double[StarBean.SIZE + 3];

        // Check that parallax exists (5-param solution), otherwise we have no distance
        if (!tokens[IDX_PLLX].isEmpty()) {
            /** ID **/
            long sourceid = Parser.parseLong(tokens[IDX_SOURCE_ID]);
            boolean mustLoad = mustLoad(sourceid);

            /** PARALLAX **/
            // Add the zero point to the parallax
            double pllx = Parser.parseDouble(tokens[IDX_PLLX]) + parallaxZeroPoint;
            //pllx = 0.0200120072;
            double pllxerr = Parser.parseDouble(tokens[IDX_PLLX_ERR]);
            double appmag = Parser.parseDouble(tokens[IDX_G_MAG]);

            // Keep only stars with relevant parallaxes
            if (mustLoad || acceptParallax(appmag, pllx, pllxerr)) {

                /** DISTANCE **/
                double distpc = (1000d / pllx);
                double geodistpc = getGeoDistance(sourceid);

                if (!mustLoad && !ruwe.isNaN()) {
                    // RUWE test!
                    float ruweVal = getRuweValue(sourceid);
                    if (ruweVal > ruwe) {
                        // Do not accept
                        return false;
                    }
                }

                // If we have geometric distances, we only accept those, otherwise, accept all
                if (mustLoad || !hasGeoDistances() || (hasGeoDistances() && hasGeoDistance(sourceid))) {
                    distpc = geodistpc > 0 ? geodistpc : distpc;

                    if (mustLoad || acceptDistance(distpc)) {

                        double dist = distpc * Constants.PC_TO_U;

                        /** NAME **/
                        String name = String.valueOf(sourceid);

                        /** RA and DEC **/
                        double ra = Parser.parseDouble(tokens[IDX_RA]);
                        double dec = Parser.parseDouble(tokens[IDX_DEC]);
                        double rarad = Math.toRadians(ra);
                        double decrad = Math.toRadians(dec);
                        // If distance is negative due to mustLoad, we need to be able to retrieve sph pos later on, so we use 1 m to mark it
                        Vector3d pos = Coordinates.sphericalToCartesian(rarad, decrad, Math.max(dist, NEGATIVE_DIST), new Vector3d());

                        /** PROPER MOTIONS in mas/yr **/
                        double mualphastar = Parser.parseDouble(tokens[IDX_MUALPHA]);
                        double mudelta = Parser.parseDouble(tokens[IDX_MUDELTA]);

                        /** RADIAL VELOCITY in km/s **/
                        double radvel = Parser.parseDouble(tokens[IDX_RADVEL]);
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
                            if (tokens.length >= 20 && !tokens[IDX_A_G].isEmpty()) {
                                // Take extinction from database
                                ag = Parser.parseDouble(tokens[IDX_A_G]);
                            } else {
                                // Compute extinction analytically
                                Vector3d posgal = new Vector3d(pos);
                                posgal.mul(Coordinates.eqToGal());
                                Vector3d posgalsph = Coordinates.cartesianToSpherical(posgal, new Vector3d());
                                double b = posgalsph.y;
                                magcorraux = Math.min(distpc, 150d / Math.abs(Math.sin(b)));
                                ag = magcorraux * 5.9e-4;
                                // Limit to 3
                                ag = Math.min(ag, 3.2);
                            }
                        }
                        // Apply extinction
                        appmag -= ag;
                        double absmag = appmag - 5 * Math.log10((distpc <= 0 ? 10 : distpc)) + 5;
                        // Pseudo-luminosity. Usually L = L0 * 10^(-0.4*Mbol). We omit M0 and approximate Mbol = M
                        double pseudoL = Math.pow(10, -0.4 * absmag);
                        double sizeFactor = Nature.PC_TO_M * Constants.ORIGINAL_M_TO_U * 0.15;
                        double size = Math.min((Math.pow(pseudoL, 0.45) * sizeFactor), 1e10);
                        //double radius = tokens.length >= 19 && !tokens[IDX_RADIUS]].isEmpty() ? Parser.parseDouble(tokens[IDX_RADIUS]]) * Constants.Ro_TO_U : size * Constants.STAR_SIZE_FACTOR;

                        /** COLOR, we use the tycBV map if present **/
                        // Reddening
                        double ebr = 0;
                        if (magCorrections) {
                            if (tokens.length >= 21 && !tokens[IDX_E_BP_MIN_RP].isEmpty()) {
                                // Take reddening from table
                                ebr = Parser.parseDouble(tokens[IDX_E_BP_MIN_RP]);
                            } else {
                                // Compute reddening analtytically
                                ebr = magcorraux * 2.9e-4;
                                // Limit to 1.6
                                ebr = Math.min(ebr, 1.6);
                            }
                        }

                        // XP = BP - RP - Reddening
                        float bp = (float) Parser.parseDouble(tokens[IDX_BP_MAG].trim());
                        float rp = (float) Parser.parseDouble(tokens[IDX_RP_MAG].trim());
                        double xp = bp - rp - ebr;

                        // See Gaia broad band photometry (https://doi.org/10.1051/0004-6361/201015441)
                        double teff;
                        if (tokens.length > 18 && !tokens[IDX_TEFF].isEmpty()) {
                            // Use database Teff
                            teff = Parser.parseDouble(tokens[IDX_TEFF]);
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
                        //point[StarBean.I_TYC1] = -1;
                        //point[StarBean.I_TYC2] = -1;
                        //point[StarBean.I_TYC3] = -1;
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

                        int appClmp = (int) MathUtilsd.clamp(appmag, 0, 21);
                        countsPerMag[appClmp] += 1;
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
    public Array<ParticleBean> loadDataMapped(String file, double factor) {
        return loadDataMapped(file, factor, -1, -1);
    }

    /**
     * Uses memory mapped files to load catalog files.
     * This is not working right now
     *
     * @param file
     * @param factor
     * @param fileNumber
     * @return
     */
    public Array<ParticleBean> loadDataMapped(String file, double factor, int fileNumber, long totalFiles) {
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
            LongWrap addedStars = new LongWrap(0l);
            LongWrap discardedStars = new LongWrap(0l);
            loadFileIs(data, factor, addedStars, discardedStars);

            if (fileNumber >= 0 && totalFiles >= 0)
                logger.info(fileNumber + "/" + totalFiles + " (" + nf.format((double) fileNumber * 100d / (double) totalFiles) + "%): " + fileName + " --> " + addedStars.value + "/" + (addedStars.value + discardedStars.value) + " stars (" + nf.format(100d * (double) addedStars.value / (double) (addedStars.value + discardedStars.value)) + "%)");
            else
                logger.info(fileName + " --> " + addedStars.value + "/" + (addedStars.value + discardedStars.value) + " stars (" + nf.format(100d * (double) addedStars.value / (double) (addedStars.value + discardedStars.value)) + "%)");

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
