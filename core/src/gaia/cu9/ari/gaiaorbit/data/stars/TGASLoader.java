package gaia.cu9.ari.gaiaorbit.data.stars;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.data.ISceneGraphLoader;
import gaia.cu9.ari.gaiaorbit.scenegraph.Particle;
import gaia.cu9.ari.gaiaorbit.scenegraph.Star;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.Nature;
import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.parse.Parser;

/**
 * Loads TGAS stars in the original ASCII format:
 * 
 * # 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 # cat sourceId alpha
 * alphaStarError delta deltaError varpi varpiError muAlphaStar muAlphaStarError
 * muDelta muDeltaError nObsAl nOut excessNoise gMag nuEff C M
 * 
 * Source position and corresponding errors are in radians, parallax in mas and
 * propermotion in mas/yr. The colors we get from the BT and VT magnitudes in
 * the original Tycho2 catalog: B-V = 0.85 * (BT - VT)
 * 
 * @author Toni Sagrista
 *
 */
public class TGASLoader extends AbstractCatalogLoader implements ISceneGraphLoader {
    private static final Log logger = Logger.getLogger(TGASLoader.class);

    // Version to load; 0 - old, 1 - new
    public static int VERSION = 1;

    private static final String separator_new = ",";

    private static final String comma = ",";
    private static final String comment = "#";

    /** Whether to load the sourceId->HIP correspondences file **/
    public boolean useHIP = false;
    /** Map of Gaia sourceId to HIP id **/
    public Map<Long, Integer> sidHIPMap;

    /** Colors BT, VT for all Tycho2 stars file **/
    private static final String btvtColorsFile = "data/tgas_final/bt-vt-tycho.csv";
    /** TYC identifier to B-V colours **/
    private Map<String, Float> tycBV;

    /** Gaia sourceid to radial velocities file **/
    private static final String raveTgasFile = "data/tgas_final/rave_rv.csv";
    /** SourceId to heliocentric radial velocity in km/s **/
    private Map<Long, Double> radialVelocities;

    /**
     * INDICES: source_id ra[deg] dec[deg] parallax[mas] parallax_error[mas]
     * pmra[mas/yr] pmdec[mas/yr] radial_velocity[km/s]
     * radial_velocity_error[km/s] phot_g_mean_mag[mag] phot_bp_mean_mag[mag]
     * phot_rp_mean_mag[mag] hip tycho2_id ref_epoch[julianyears]
     */
    private static final int SOURCE_ID = 0;
    private static final int RA = 1;
    private static final int DEC = 2;
    private static final int PLLX = 3;
    private static final int PLLX_ERR = 4;
    private static final int MUALPHA = 5;
    private static final int MUDELTA = 6;
    private static final int APPMAG = 7;
    private static final int HIP = 8;
    private static final int TYCHO2 = 9;
    private static final int REF_EPOCH = 10;

    private static final int[] indices_new = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 };

    private int sidhipfound = 0;

    @Override
    public Array<Particle> loadData() throws FileNotFoundException {
        String separator = null;
        int[] indices = null;
        if (VERSION == 1) {
            // NEW
            separator = separator_new;
            indices = indices_new;
        } else {
            logger.error("VERSION number not recognized");
            return null;
        }

        tycBV = loadTYCBVColours(btvtColorsFile);
        radialVelocities = loadRadialVelocities(raveTgasFile);

        Array<Particle> stars = new Array<Particle>();
        for (String file : files) {
            FileHandle f = Gdx.files.internal(file);
            InputStream data = f.read();
            BufferedReader br = new BufferedReader(new InputStreamReader(data));

            try {
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.startsWith(comment))
                        // Add star
                        addStar(line, stars, indices, separator);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    br.close();
                } catch (IOException e) {
                    logger.error(e);
                }

            }
        }

        logger.info("SourceId matched to HIP in " + sidhipfound + " stars");
        logger.info(I18n.bundle.format("notif.catalog.init", stars.size));
        return stars;
    }

    private void addStar(String line, Array<Particle> stars, int[] indices, String separator) {
        String[] st = line.split(separator);

        int hip = -1;
        long sourceid = Parser.parseLong(st[indices[SOURCE_ID]]);

        if (indices[HIP] >= 0) {
            hip = Parser.parseInt(st[indices[HIP]].trim());
        }

        String tycho2 = null;
        if (indices[TYCHO2] >= 0) {
            tycho2 = st[indices[TYCHO2]].trim();
        }

        float appmag = new Double(Parser.parseDouble(st[indices[APPMAG]].trim())).floatValue();

        if (appmag < GlobalConf.data.LIMIT_MAG_LOAD) {

            double ra = Parser.parseDouble(st[indices[RA]].trim());
            double dec = Parser.parseDouble(st[indices[DEC]].trim());
            double pllx = Parser.parseDouble(st[indices[PLLX]].trim());
            double pllxerr = Parser.parseDouble(st[indices[PLLX_ERR]].trim());

            double dist = (1000d / pllx) * Constants.PC_TO_U;
            // Keep only stars with relevant parallaxes
            if (dist >= 0 && pllx / pllxerr > 8 && pllxerr <= 1) {

                Vector3d pos = Coordinates.sphericalToCartesian(Math.toRadians(ra), Math.toRadians(dec), dist, new Vector3d());

                /** PROPER MOTIONS in mas/yr **/
                double mualphastar = Parser.parseDouble(st[indices[MUALPHA]].trim()) * Nature.MILLARCSEC_TO_DEG;
                double mudelta = Parser.parseDouble(st[indices[MUDELTA]].trim()) * Nature.MILLARCSEC_TO_DEG;
                double mualpha = mualphastar / Math.cos(Math.toRadians(dec));

                /** RADIAL VELOCITY in km/s, convert to u/ur **/
                double radvel = radialVelocities != null && radialVelocities.containsKey(sourceid) ? radialVelocities.get(sourceid) : 0;

                /** PROPER MOTION VECTOR = (pos+dx) - pos **/
                Vector3d pm = Coordinates.sphericalToCartesian(Math.toRadians(ra + mualpha), Math.toRadians(dec + mudelta), dist + radvel * Constants.KM_TO_U / Nature.S_TO_Y, new Vector3d());
                pm.sub(pos);

                Vector3 pmfloat = pm.toVector3();
                Vector3 pmSph = new Vector3(Parser.parseFloat(st[indices[MUALPHA]].trim()), Parser.parseFloat(st[indices[MUDELTA]].trim()), (float) radvel);

                /** COLOR, we use the tycBV map if present **/
                float colorbv = 0;
                if (tycBV != null) {
                    if (tycBV.containsKey(tycho2)) {
                        colorbv = tycBV.get(tycho2);
                    }
                }

                double distpc = 1000d / pllx;
                float absmag = (float) (appmag - 2.5 * Math.log10(Math.pow(distpc / 10d, 2d)));
                String name = Long.toString(sourceid);

                Star star = new Star(pos, pmfloat, pmSph, appmag, absmag, colorbv, name, (float) ra, (float) dec, sourceid, hip, tycho2, (byte) 1);
                if (runFiltersAnd(star))
                    stars.add(star);
            }

        }
    }

    /**
     * Loads the radial velocities file (RAVE)
     * 
     * @param file
     *            The file location
     * @return The data map
     */
    private Map<Long, Double> loadRadialVelocities(String file) {
        Map<Long, Double> result = new HashMap<Long, Double>();

        FileHandle f = Gdx.files.internal(file);

        if (!f.exists())
            return null;

        InputStream data = f.read();
        BufferedReader br = new BufferedReader(new InputStreamReader(data));
        try {
            // skip first line with headers
            br.readLine();

            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",");
                Long sourceid = Parser.parseLong(tokens[0]);
                Double radvel = Parser.parseDouble(tokens[1]);
                result.put(sourceid, radvel);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                logger.error(e);
            }

        }

        return result;
    }

    /**
     * Loads the TYC to BT-VT colour file
     * 
     * @param file
     *            The file location
     * @return Map with the data
     */
    private Map<String, Float> loadTYCBVColours(String file) {
        Map<String, Float> result = new HashMap<String, Float>();
        FileHandle f = Gdx.files.internal(file);
        InputStream data = f.read();
        BufferedReader br = new BufferedReader(new InputStreamReader(data));
        try {
            // skip first line with headers
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {

                if (!line.startsWith(comment))
                    // Add B-V colour
                    addColour(line, result);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                logger.error(e);
            }

        }

        return result;
    }

    private void addColour(String line, Map<String, Float> map) {
        String[] st = line.split(comma);
        int tyc1 = Parser.parseInt(st[1].trim());
        int tyc2 = Parser.parseInt(st[2].trim());
        int tyc3 = Parser.parseInt(st[3].trim());

        float BV = 0;
        if (st.length >= 7) {
            float bt = Parser.parseFloat(st[5].trim());
            float vt = Parser.parseFloat(st[6].trim());
            BV = 0.850f * (bt - vt);
        }

        map.put(String.format("%04d", tyc1) + "-" + String.format("%05d", tyc2) + "-" + tyc3, BV);
    }

}