/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.stars;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import gaiasky.data.ISceneGraphLoader;
import gaiasky.data.JsonLoader;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.scenegraph.BillboardGalaxy;
import gaiasky.scenegraph.CelestialBody;
import gaiasky.scenegraph.Particle;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.coord.StaticCoordinates;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.util.parse.Parser;
import org.apfloat.Apfloat;

import java.io.*;
import java.util.Arrays;

/**
 * Loads the NBG catalog in csv format.
 *
 * <ul>
 * <li>Name</li>
 * <li>Altname</li>
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
 * <li>Img - path to billboard image</li>
 * <li>Size - the size [pc]</li>
 * </ul>
 * @deprecated Use JSON format instead, with {@link JsonLoader}.
 */
@Deprecated
public class NBGLoader extends AbstractCatalogLoader implements ISceneGraphLoader {
    private static final Log logger = Logger.getLogger(NBGLoader.class);

    boolean active = true;

    @Override
    public Array<CelestialBody> loadData() {
        Array<CelestialBody> galaxies = new Array<>(false, 900);
        long baseId = 8500000;
        long offset = 0;
        if (active) {
            for (String file : files) {
                FileHandle f = Settings.settings.data.dataFileHandle(file);
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
                            String altname = tokens[1];
                            double ra = Parser.parseDouble(tokens[2]);
                            double dec = Parser.parseDouble(tokens[3]);
                            double distMpc = Parser.parseDouble(tokens[4]);
                            double distPc = distMpc * 1e6d;
                            double kmag = Parser.parseDouble(tokens[5]);
                            String img = tokens[13];
                            double sizepc = Parser.parseDouble(tokens[14]);

                            CelestialBody g;
                            if (img == null || img.isEmpty()) {
                                // Regular shaded light point
                                Apfloat dist = new Apfloat(distMpc * Constants.MPC_TO_U, Constants.PREC);
                                Vector3b pos = Coordinates.sphericalToCartesian(Math.toRadians(ra), Math.toRadians(dec), dist, new Vector3b());
                                float absMag = (float) (kmag - 2.5 * Math.log10(Math.pow(distPc / 10d, 2d)));
                                float colorbv = 0;

                                Particle gal = new Particle(pos, (float) kmag, absMag, colorbv, altname.isBlank() ? new String[] { name } : new String[] { name, altname }, (float) ra, (float) dec, baseId + offset, 0, 2e-10, 1.7e-12, 0.15f, 1.2e1f, 0.00004f, 3.3f, RenderGroup.BILLBOARD_GAL);
                                gal.setCt("Galaxies");
                                gal.setParent("NBG");
                                g = gal;
                            } else {
                                // Billboard
                                BillboardGalaxy gal = new BillboardGalaxy(new String[] { name, altname }, ra, dec, distPc, sizepc, "data/tex/extragal/" + img);
                                // Fade in parsecs from sun
                                gal.setFade(new double[] { distPc * 0.3, distPc * 0.6 });
                                g = gal;
                            }
                            galaxies.add(g);
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
        }

        logger.info(I18n.msg("notif.catalog.init", galaxies.size));
        return galaxies;
    }

}
