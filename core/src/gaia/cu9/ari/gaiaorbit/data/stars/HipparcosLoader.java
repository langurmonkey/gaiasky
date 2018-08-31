
package gaia.cu9.ari.gaiaorbit.data.stars;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
 * Loads Hipparcos stars in the original CSV format:
 * 
 * The columns are:
 * hip,ra,dec,plx,e_plx,pmra,pmdec,b_v,vmag,btmag,vtmag
 * 
 * @author Toni Sagrista
 *
 */
public class HipparcosLoader extends AbstractCatalogLoader implements ISceneGraphLoader {
    private static final Log logger = Logger.getLogger(HipparcosLoader.class);
    
    // Version to load; 0 - old, 1 - new
    public static int VERSION = 1;

    private static final String comma = ",";
    private static final String comment = "#";

    /**
     * INDICES: hip,ra,dec,plx,e_plx,pmra,pmdec,b_v,vmag,btmag,vtmag
     */
    private static final int HIP = 0;
    private static final int RA = 1;
    private static final int DEC = 2;
    private static final int PLLX = 3;
    private static final int PLLX_ERR = 4;
    private static final int MUALPHA = 5;
    private static final int MUDELTA = 6;
    private static final int BV_COLOR = 7;
    private static final int V_MAG = 8;

    private int sidhipfound = 0;

    @Override
    public Array<Particle> loadData() throws FileNotFoundException {
        String separator = comma;

        Array<Particle> stars = new Array<Particle>();
        for (String file : files) {
            FileHandle f = Gdx.files.internal(file);
            InputStream data = f.read();
            BufferedReader br = new BufferedReader(new InputStreamReader(data));

            try {
                String line;
                // Skip first line with headers
                br.readLine();
                while ((line = br.readLine()) != null) {
                    if (!line.startsWith(comment))
                        //Add star
                        addStar(line, stars, separator);
                    // addStar(line, stars, indices_old, separator_old);
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

    private void addStar(String line, Array<Particle> stars, String separator) {
        String[] st = line.split(separator);

        try {

            int hip = Parser.parseInt(st[HIP]);

            float appmag = new Double(Parser.parseDouble(st[V_MAG].trim())).floatValue();

            if (appmag < GlobalConf.data.LIMIT_MAG_LOAD) {
                double ra = Parser.parseDouble(st[RA].trim());
                double dec = Parser.parseDouble(st[DEC].trim());
                double pllx = Parser.parseDouble(st[PLLX].trim());
                double pllxerr = Parser.parseDouble(st[PLLX_ERR].trim());

                double dist = (1000d / pllx) * Constants.PC_TO_U;
                // Keep only stars with relevant parallaxes
                if (dist >= 0 && pllx / pllxerr > 8 && pllxerr <= 1) {

                    Vector3d pos = Coordinates.sphericalToCartesian(Math.toRadians(ra), Math.toRadians(dec), dist, new Vector3d());

                    /** PROPER MOTIONS in mas/yr **/
                    double mualpha = Parser.parseDouble(st[MUALPHA].trim()) * Nature.MILLARCSEC_TO_DEG;
                    double mudelta = Parser.parseDouble(st[MUDELTA].trim()) * Nature.MILLARCSEC_TO_DEG;

                    /** PROPER MOTION VECTOR = (pos+dx) - pos **/
                    Vector3d pm = Coordinates.sphericalToCartesian(Math.toRadians(ra + mualpha), Math.toRadians(dec + mudelta), dist, new Vector3d());
                    pm.sub(pos);

                    // TODO Use radial velocity if necessary to get a 3D proper motion

                    Vector3 pmfloat = pm.toVector3();
                    Vector3 pmSph = new Vector3(Parser.parseFloat(st[MUALPHA].trim()), Parser.parseFloat(st[MUDELTA].trim()), 0f);

                    /** COLOR, we use the tycBV map if present **/
                    float colorbv = Parser.parseFloat(st[BV_COLOR].trim());

                    float absmag = appmag;
                    String name = "HIP " + Integer.toString(hip);

                    Star star = new Star(pos, pmfloat, pmSph, appmag, absmag, colorbv, name, (float) ra, (float) dec, hip, hip, null, (byte) 2);
                    if (runFiltersAnd(star))
                        stars.add(star);
                }

            }
        } catch (Exception e) {
            logger.error("Error adding star");
        }
    }

}
