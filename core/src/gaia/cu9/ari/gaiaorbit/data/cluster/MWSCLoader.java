package gaia.cu9.ari.gaiaorbit.data.cluster;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.data.ISceneGraphLoader;
import gaia.cu9.ari.gaiaorbit.data.stars.AbstractCatalogLoader;
import gaia.cu9.ari.gaiaorbit.scenegraph.StarCluster;
import gaia.cu9.ari.gaiaorbit.util.*;
import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.parse.Parser;

import java.io.*;

/**
 * Loads the MWSC catalog in CSV format.
 *
 * <ul>
 * <li>name</li>
 * <li>raj2000 [deg]</li>
 * <li>dej2000 [deg]</li>
 * <li>dist [pc]</li>
 * <li>pmra [deg/yr]</li>
 * <li>pmde [deg/yr]</li>
 * <li>rv [km/s]</li>
 * <li>rcluster [deg] - Radius</li>
 * <li>nstars - Number of stars</li>
 * </ul>
 *
 * @author Toni Sagrista
 *
 */
public class MWSCLoader extends AbstractCatalogLoader implements ISceneGraphLoader {
    boolean active = true;

    @Override
    public Array<StarCluster> loadData() throws FileNotFoundException {
        Array<StarCluster> clusters = new Array<StarCluster>(3006);

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
                            double dist = Parser.parseDouble(tokens[3]) * Constants.PC_TO_U;
                            double mualpha = Parser.parseDouble(tokens[4]);
                            double mudelta = Parser.parseDouble(tokens[5]);
                            double radvel = tokens[6].isEmpty() ? 0 : Parser.parseDouble(tokens[6]);
                            double radius = Parser.parseDouble(tokens[7]);
                            int nstars = Parser.parseInt(tokens[8]);

                            Vector3d pos = Coordinates.sphericalToCartesian(Math.toRadians(ra), Math.toRadians(dec), dist, new Vector3d());
                            Vector3d pm = Coordinates.sphericalToCartesian(Math.toRadians(ra + mualpha), Math.toRadians(dec + mudelta), dist + radvel * Constants.KM_TO_U / Nature.S_TO_Y, new Vector3d());
                            pm.sub(pos);

                            Vector3d posSph = new Vector3d((float) ra, (float) dec, (float) dist);
                            Vector3 pmSph = new Vector3((float) (mualpha * Nature.DEG_TO_MILLARCSEC), (float) (mudelta * Nature.DEG_TO_MILLARCSEC), (float) radvel);

                            StarCluster c = new StarCluster(name, "MWSC", pos, pm, posSph, pmSph, radius, nstars);

                            clusters.add(c);
                        }
                        linenum++;
                    }

                    for (StarCluster c : clusters) {
                        c.initialize();
                    }

                } catch (IOException e) {
                    Logger.getLogger(this.getClass()).error(e);
                } finally {
                    try {
                        br.close();
                    } catch (IOException e) {
                        Logger.getLogger(this.getClass()).error(e);
                    }

                }
            }

        Logger.getLogger(this.getClass()).info(I18n.bundle.format("notif.catalog.init", clusters.size));
        return clusters;
    }

}
