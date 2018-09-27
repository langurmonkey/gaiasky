package gaia.cu9.ari.gaiaorbit.data.stars;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.data.ISceneGraphLoader;
import gaia.cu9.ari.gaiaorbit.scenegraph.Galaxy;
import gaia.cu9.ari.gaiaorbit.scenegraph.Particle;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.parse.Parser;

/**
 * Loads the NBG catalog in csv format.
 *
 * <ul>
 * <li>Name</li>
 * <li>RAJ2000 [deg]</li>
 * <li>DEJ2000 [deg]</li>
 * <li>Dist [Mpc]</li>
 * <li>Kmag [-1.75/17.54] - 2MASS Ks band magnitude</li>
 * <li>Bmag [-4.6/21.4] - Apparent integral B band magnitude</li>
 * <li>a26 [arcmin] - Major angular diameter</li>
 * <li>b/a - Apparent axial ratio</li>
 * <li>HRV [km/s] - Heliocentric radial velocity</li>
 * <li>i [deg] - [0/90] Inclination of galaxy from the face-on (i=0)
 * position</li>
 * <li>TT [-3/11] - Morphology T-type code</li>
 * <li>Mcl [char] - Dwarf galaxy morphology (BCD, HIcld, Im, Ir, S0em, Sm, Sph,
 * Tr, dE, dEem, or dS0em)</li>
 * </ul>
 *
 * @author Toni Sagrista
 *
 */
public class NBGLoader extends AbstractCatalogLoader implements ISceneGraphLoader {
    private static final Log logger = Logger.getLogger(NBGLoader.class);

    boolean active = true;

    @Override
    public Array<Particle> loadData() throws FileNotFoundException {
        Array<Particle> galaxies = new Array<Particle>(900);
        long baseid = 5000;
        long offset = 0;
        if (active)
            for (String file : files) {
                FileHandle f = GlobalConf.data.dataFileHandle(file);
                InputStream data = f.read();
                BufferedReader br = new BufferedReader(new InputStreamReader(data));

                try {
                    String line;
                    int linenum = 0;
                    while ((line = br.readLine()) != null) {
                        if (linenum > 0) {
                            // Add galaxy
                            String[] tokens = line.split(",");
                            String name = tokens[0];
                            double ra = Parser.parseDouble(tokens[1]);
                            double dec = Parser.parseDouble(tokens[2]);
                            double dist = Parser.parseDouble(tokens[3]) * Constants.MPC_TO_U;
                            double kmag = Parser.parseDouble(tokens[4]);
                            double bmag = Parser.parseDouble(tokens[5]);
                            double a26 = Parser.parseDouble(tokens[6]);
                            double ba = Parser.parseDouble(tokens[7]);
                            int hrv = Parser.parseInt(tokens[8]);
                            int i = Parser.parseInt(tokens[9]);
                            int tt = Parser.parseInt(tokens[10]);
                            String Mcl = tokens[11];

                            Vector3d pos = Coordinates.sphericalToCartesian(Math.toRadians(ra), Math.toRadians(dec), dist, new Vector3d());
                            float colorbv = 0;
                            float absmag = (float) (kmag - 2.5 * Math.log10(Math.pow(dist * Constants.U_TO_PC / 10d, 2d)));

                            Galaxy gal = new Galaxy(pos, (float) kmag, absmag, colorbv, name, (float) ra, (float) dec, (float) bmag, (float) a26, (float) ba, hrv, i, tt, Mcl, baseid + offset);
                            gal.setParent("NBG");
                            galaxies.add(gal);
                            offset++;
                        }
                        linenum++;
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

        logger.info(I18n.bundle.format("notif.catalog.init", galaxies.size));
        return galaxies;
    }

}
