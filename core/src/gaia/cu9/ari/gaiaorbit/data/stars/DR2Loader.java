package gaia.cu9.ari.gaiaorbit.data.stars;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.GZIPInputStream;

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
 * Loads the DR2 catalog in CSV format
 * 
 * Source position and corresponding errors are in radians, parallax in mas and
 * propermotion in mas/yr. The colors we get from the BT and VT magnitudes in
 * the original Tycho2 catalog: B-V = 0.85 * (BT - VT)
 * 
 * @author Toni Sagrista
 *
 */
public class DR2Loader extends AbstractCatalogLoader implements ISceneGraphLoader {
    private static final Log logger = Logger.getLogger(DR2Loader.class);
    private static final String comma = ",";
    private static final String comment = "#";

    private static final String separator = comma;

    /** Whether to load the sourceId->HIP correspondences file **/
    public boolean useHIP = false;
    /** Map of Gaia sourceId to HIP id **/
    public Map<Long, Integer> sidHIPMap;

    /**
     * INDICES:
     * 
     * source_id ra[deg] dec[deg] parallax[mas] ra_err[mas] dec_err[mas]
     * pllx_err[mas] pmra[mas/yr] pmdec[mas/yr] radvel[km/s] mualpha_err[mas/yr]
     * mudelta_err[mas/yr] radvel_err[km/s] gmag[mag] bp[mag] rp[mag]
     * ref_epoch[julian years]
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

    private static final int[] indices = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };

    private int sidhipfound = 0;

    @Override
    public Array<Particle> loadData() throws FileNotFoundException {

        Array<Particle> stars = new Array<Particle>();
        for (String file : files) {

            FileHandle fh = Gdx.files.absolute(file);
            loadFile(fh, stars);

        }

        logger.info("SourceId matched to HIP in " + sidhipfound + " stars");
        logger.info(I18n.bundle.format("notif.catalog.init", stars.size));
        return stars;
    }

    public void loadFile(FileHandle f, Array<Particle> stars) {
        if (f.isDirectory()) {
            // Recursive
            FileHandle[] files = f.list();
            Arrays.sort(files, (FileHandle a, FileHandle b) -> {
                return a.name().compareTo(b.name());
            });
            for (FileHandle fh : files)
                loadFile(fh, stars);
        } else if (f.name().endsWith(".csv") || f.name().endsWith(".gz")) {
            boolean gz = f.name().endsWith(".gz");

            // Simple case
            InputStream data = f.read();

            if (gz) {
                try {
                    data = new GZIPInputStream(data);
                } catch (IOException e) {
                    logger.error(e);
                    return;
                }
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(data));

            long i = 0;
            try {
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.startsWith(comment)) {
                        // Add star
                        addStar(line, stars);
                        i++;
                    }
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
            logger.warn("File loaded: " + f.path() + " - Objects: " + i);
        } else {
            logger.warn("File skipped: " + f.path());
        }
    }

    private void addStar(String line, Array<Particle> stars) {
        String[] tokens = line.split(separator);

        long sourceid = Parser.parseLong(tokens[indices[SOURCE_ID]]);

        float appmag = new Double(Parser.parseDouble(tokens[indices[G_MAG]].trim())).floatValue();

        if (appmag < GlobalConf.data.LIMIT_MAG_LOAD) {

            double ra = Parser.parseDouble(tokens[indices[RA]].trim());
            double dec = Parser.parseDouble(tokens[indices[DEC]].trim());
            // We add 29 mas because the zero point was recalibrated and shifted
            double pllx = Parser.parseDouble(tokens[indices[PLLX]].trim()) + 29;
            double pllxerr = Parser.parseDouble(tokens[indices[PLLX_ERR]].trim());

            double dist = (1000d / pllx) * Constants.PC_TO_U;
            // Keep only stars with relevant parallaxes
            if (dist >= 0 && pllx / pllxerr > 8 && pllxerr <= 1) {

                Vector3d pos = Coordinates.sphericalToCartesian(Math.toRadians(ra), Math.toRadians(dec), dist, new Vector3d());

                /** PROPER MOTIONS in mas/yr **/
                double mualphastar = Parser.parseDouble(tokens[indices[MUALPHA]].trim()) * Nature.MILLARCSEC_TO_DEG;
                double mudelta = Parser.parseDouble(tokens[indices[MUDELTA]].trim()) * Nature.MILLARCSEC_TO_DEG;
                double mualpha = mualphastar / Math.cos(Math.toRadians(dec));

                /** RADIAL VELOCITY in km/s, convert to u/yr **/
                double radvel = 0;
                if (indices[RADVEL] >= 0) {
                    radvel = tokens[indices[RADVEL]].isEmpty() ? 0 : Parser.parseDouble(tokens[indices[RADVEL]].trim());
                }

                /** PROPER MOTION VECTOR = (pos+dx) - pos **/
                Vector3d pm = Coordinates.sphericalToCartesian(Math.toRadians(ra + mualpha), Math.toRadians(dec + mudelta), dist + radvel * Constants.KM_TO_U / Nature.S_TO_Y, new Vector3d());
                pm.sub(pos);

                Vector3 pmfloat = pm.toVector3();
                Vector3 pmSph = new Vector3(Parser.parseFloat(tokens[indices[MUALPHA]].trim()), Parser.parseFloat(tokens[indices[MUDELTA]].trim()), (float) radvel);

                /** COLOR, we use the tycBV map if present **/
                float colorbv = 0;
                if (indices[BP_MAG] >= 0 && indices[RP_MAG] >= 0) {
                    // Real TGAS
                    float bp = new Double(Parser.parseDouble(tokens[indices[BP_MAG]].trim())).floatValue();
                    float rp = new Double(Parser.parseDouble(tokens[indices[RP_MAG]].trim())).floatValue();
                    colorbv = bp - rp;
                } else {
                    // Use color value in BP
                    colorbv = new Double(Parser.parseDouble(tokens[indices[BP_MAG]].trim())).floatValue();
                }

                double distpc = 1000d / pllx;
                float absmag = (float) (appmag - 2.5 * Math.log10(Math.pow(distpc / 10d, 2d)));
                String name = Long.toString(sourceid);

                Star star = new Star(pos, pmfloat, pmSph, appmag, absmag, colorbv, name, (float) ra, (float) dec, sourceid, -1, null, (byte) 1);
                if (runFiltersAnd(star))
                    stars.add(star);
            }

        }
    }
}
