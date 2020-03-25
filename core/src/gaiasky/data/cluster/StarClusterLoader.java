/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.cluster;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.data.ISceneGraphLoader;
import gaiasky.data.stars.AbstractCatalogLoader;
import gaiasky.scenegraph.StarCluster;
import gaiasky.util.*;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.Vector3d;
import gaiasky.util.parse.Parser;
import gaiasky.util.ucd.UCDParser;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads the star cluster catalogs in CSV format. The column order is not important. The
 * column names, however, are:
 *
 * <ul>
 * <li>name: {@link gaiasky.util.ucd.UCDParser#idcolnames}</li>
 * <li>ra[deg]: {@link gaiasky.util.ucd.UCDParser#racolnames}</li>
 * <li>de[deg]: {@link gaiasky.util.ucd.UCDParser#decolnames}</li>
 * <li>dist[pc]: {@link gaiasky.util.ucd.UCDParser#distcolnames}</li>
 * <li>pmra[mas/yr]: {@link gaiasky.util.ucd.UCDParser#pmracolnames}</li>
 * <li>pmde[mas/yr]: {@link gaiasky.util.ucd.UCDParser#pmdeccolnames}</li>
 * <li>rv[km/s]: {@link gaiasky.util.ucd.UCDParser#radvelcolnames}</li>
 * <li>radius[deg]: {@link gaiasky.util.ucd.UCDParser#radiuscolnames}</li>
 * <li>nstars: {@link gaiasky.util.ucd.UCDParser#nstarscolnames}</li>
 * </ul>
 *
 * @author Toni Sagrista
 */
public class StarClusterLoader extends AbstractCatalogLoader implements ISceneGraphLoader {
    boolean active = true;

    private enum ClusterProperties {
        NAME, RA, DEC, DIST, PMRA, PMDE, RV, RADIUS, NSTARS
    }

    @Override
    public Array<StarCluster> loadData() {
        Array<StarCluster> clusters = new Array<>();

        if (active)
            for (String file : files) {
                FileHandle f = GlobalConf.data.dataFileHandle(file);
                InputStream data = f.read();
                BufferedReader br = new BufferedReader(new InputStreamReader(data));

                try {
                    String header = br.readLine();
                    Map<ClusterProperties, Integer> indices = parseHeader(header);
                    String line;
                    while ((line = br.readLine()) != null) {
                        // Add galaxy
                        String[] tokens = line.split(",");
                        String name = tokens[0];
                        double ra = getDouble(tokens, ClusterProperties.RA, indices);
                        double rarad = Math.toRadians(ra);
                        double dec = getDouble(tokens, ClusterProperties.DEC, indices);
                        double decrad = Math.toRadians(dec);
                        double distpc = getDouble(tokens, ClusterProperties.DIST, indices);
                        double dist = distpc * Constants.PC_TO_U;
                        double mualphastar = getDouble(tokens, ClusterProperties.PMRA, indices);
                        double mudelta = getDouble(tokens, ClusterProperties.PMDE, indices);
                        double radvel = getDouble(tokens, ClusterProperties.RV, indices);
                        double radius = getDouble(tokens, ClusterProperties.RADIUS, indices);
                        int nstars = getInteger(tokens, ClusterProperties.NSTARS, indices);


                        Vector3d pos = Coordinates.sphericalToCartesian(rarad, decrad, dist, new Vector3d());

                        Vector3d pm = AstroUtils.properMotionsToCartesian(mualphastar, mudelta, radvel, Math.toRadians(ra), Math.toRadians(dec), distpc);

                        Vector3d posSph = new Vector3d((float) ra, (float) dec, (float) dist);
                        Vector3 pmSph = new Vector3((float) (mualphastar), (float) (mudelta), (float) radvel);

                        StarCluster c = new StarCluster(name, parentName != null ? parentName : "MWSC", pos, pm, posSph, pmSph, radius, nstars);

                        clusters.add(c);
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

    private String get(String[] tokens, ClusterProperties prop, Map<ClusterProperties, Integer> indices) {
        return (!indices.containsKey(prop) || tokens[indices.get(prop)].isEmpty()) ? null : tokens[indices.get(prop)];
    }

    private double getDouble(String[] tokens, ClusterProperties prop, Map<ClusterProperties, Integer> indices) {
        String s = get(tokens, prop, indices);
        if (s != null)
            return Parser.parseDouble(s);
        return 0;
    }

    private int getInteger(String[] tokens, ClusterProperties prop, Map<ClusterProperties, Integer> indices) {
        String s = get(tokens, prop, indices);
        if (s != null)
            return Parser.parseInt(s);
        return 0;
    }

    private Map<ClusterProperties, Integer> parseHeader(String header) {
        Map<ClusterProperties, Integer> indices = new HashMap<>();
        header = header.strip();
        if (header.startsWith("#"))
            header = header.substring(1);

        String[] tokens = header.split(",");

        int i = 0;
        for (String token : tokens) {
            if (UCDParser.isName(token))
                indices.put(ClusterProperties.NAME, i);
            else if (UCDParser.isRa(token))
                indices.put(ClusterProperties.RA, i);
            else if (UCDParser.isDec(token))
                indices.put(ClusterProperties.DEC, i);
            else if (UCDParser.isDist(token))
                indices.put(ClusterProperties.DIST, i);
            else if (UCDParser.isPmra(token))
                indices.put(ClusterProperties.PMRA, i);
            else if (UCDParser.isPmde(token))
                indices.put(ClusterProperties.PMDE, i);
            else if (UCDParser.isRadvel(token))
                indices.put(ClusterProperties.RV, i);
            else if (UCDParser.isRadius(token))
                indices.put(ClusterProperties.RADIUS, i);
            else if (UCDParser.isNstars(token))
                indices.put(ClusterProperties.NSTARS, i);

            i++;
        }

        return indices;
    }

}
