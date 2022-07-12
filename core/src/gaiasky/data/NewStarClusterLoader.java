/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector3;
import gaiasky.scene.Archetype;
import gaiasky.scene.Mapper;
import gaiasky.scenegraph.StarCluster;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Vector2d;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.util.parse.Parser;
import gaiasky.util.ucd.UCDParser;
import gaiasky.util.units.Quantity.Angle;
import gaiasky.util.units.Quantity.Angle.AngleUnit;
import gaiasky.util.units.Quantity.Length;
import gaiasky.util.units.Quantity.Length.LengthUnit;
import org.apfloat.Apfloat;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.formats.AsciiTableBuilder;
import uk.ac.starlink.table.formats.CsvTableBuilder;
import uk.ac.starlink.util.DataSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the star cluster catalogs from CSV files or STIL data sources. The column order is not important. The
 * column names, however, must be:
 *
 * <ul>
 * <li>name: {@link UCDParser#idcolnames}, separate multiple names with '|'</li>
 * <li>ra[deg]: {@link UCDParser#racolnames}</li>
 * <li>dist[pc]: {@link UCDParser#distcolnames}</li>
 * <li>pmra[mas/yr]: {@link UCDParser#pmracolnames}</li>
 * <li>pmde[mas/yr]: {@link UCDParser#pmdeccolnames}</li>
 * <li>rv[km/s]: {@link UCDParser#radvelcolnames}</li>
 * <li>radius[deg]: {@link UCDParser#radiuscolnames}</li>
 * <li>nstars: {@link UCDParser#nstarscolnames}</li>
 * </ul>
 */
public class NewStarClusterLoader extends AbstractSceneLoader {
    private static final Log logger = Logger.getLogger(NewStarClusterLoader.class);
    boolean active = true;
    private Archetype archetype;
    private int numLoaded = 0;

    private enum ClusterProperties {
        NAME,
        RA,
        DEC,
        DIST,
        PLLX,
        PMRA,
        PMDE,
        RV,
        RADIUS,
        NSTARS
    }

    @Override
    public void loadData() {
        // The cluster archetype
        archetype = scene.archetypes().get(StarCluster.class);
        if (active) {
            numLoaded = 0;
            if (filePaths != null) {
                for (String file : filePaths) {
                    FileHandle f = Settings.settings.data.dataFileHandle(file);
                    InputStream is = f.read();
                    try {
                        loadClustersCsv(is);
                    } catch (IOException e) {
                        logger.error(e);
                    } finally {
                        try {
                            is.close();
                        } catch (IOException e) {
                            logger.error(e);
                        }

                    }
                }
            } else if (dataSource != null) {
                try {
                    loadClustersStil(dataSource);
                } catch (IOException e) {
                    logger.error(e);
                }
            }
        }

        logger.info(I18n.msg("notif.catalog.init", numLoaded));
    }

    @Override
    public void setName(String name) {

    }

    @Override
    public void setDescription(String description) {

    }

    @Override
    public void setParams(Map<String, Object> params) {

    }

    /**
     * Loads clusters from a STIL data source.
     *
     * @param ds The data source.
     *
     * @throws IOException
     */
    private void loadClustersStil(DataSource ds) throws IOException {
        // Add extra builders
        StarTableFactory factory = new StarTableFactory();
        List builders = factory.getDefaultBuilders();
        builders.add(new CsvTableBuilder());
        builders.add(new AsciiTableBuilder());

        // Try to load
        StarTable table = factory.makeStarTable(ds);

        Map<ClusterProperties, Integer> indices = parseHeader(table);

        if (!checkIndices(indices)) {
            logger.error("At least 'ra', 'dec', 'pllx'|'dist', 'radius' and 'name' are needed, please check your columns!");
            return;
        }
        RowSequence rs = table.getRowSequence();
        while (rs.next()) {
            Object[] row = rs.getRow();
            String[] names = parseName(row[indices.get(ClusterProperties.NAME)].toString());
            double ra = getDouble(row, ClusterProperties.RA, indices, table, "deg");
            double rarad = Math.toRadians(ra);
            double dec = getDouble(row, ClusterProperties.DEC, indices, table, "deg");
            double decrad = Math.toRadians(dec);
            double distpc = 0;
            if (indices.containsKey(ClusterProperties.DIST)) {
                distpc = getDouble(row, ClusterProperties.DIST, indices, table, "pc");
            } else if (indices.containsKey(ClusterProperties.PLLX)) {
                distpc = 1000d / getDouble(row, ClusterProperties.PLLX, indices, table, "mas");
            }
            double dist = distpc * Constants.PC_TO_U;
            double mualphastar = getDouble(row, ClusterProperties.PMRA, indices, table, "mas/yr");
            double mudelta = getDouble(row, ClusterProperties.PMDE, indices, table, "mas/yr");
            double radvel = getDouble(row, ClusterProperties.RV, indices, table, "km/s");
            double radius = getDouble(row, ClusterProperties.RADIUS, indices, table, "deg");
            int nstars = getInteger(row, ClusterProperties.NSTARS, indices);

            addCluster(names, ra, rarad, dec, decrad, dist, distpc, mualphastar, mudelta, radvel, radius, nstars);
        }
    }

    /**
     * Loads clusters from a CSV file directly.
     *
     * @param data The CSV file input stream.
     *
     * @throws IOException
     */
    private void loadClustersCsv(InputStream data) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(data));

        String header = br.readLine();
        Map<ClusterProperties, Integer> indices = parseHeader(header);

        if (!checkIndices(indices)) {
            logger.error("At least 'ra', 'dec', 'pllx'|'dist', 'radius' and 'name' are needed, please check your columns!");
            return;
        }
        String line;
        while ((line = br.readLine()) != null) {
            // Add galaxy
            String[] tokens = line.split(",");
            String[] names = parseName(tokens[indices.get(ClusterProperties.NAME)]);
            double ra = getDouble(tokens, ClusterProperties.RA, indices);
            double rarad = Math.toRadians(ra);
            double dec = getDouble(tokens, ClusterProperties.DEC, indices);
            double decrad = Math.toRadians(dec);
            double distpc = 0;
            if (indices.containsKey(ClusterProperties.DIST)) {
                distpc = getDouble(tokens, ClusterProperties.DIST, indices);
            } else if (indices.containsKey(ClusterProperties.PLLX)) {
                distpc = 1000d / getDouble(tokens, ClusterProperties.PLLX, indices);
            }
            double dist = distpc * Constants.PC_TO_U;
            double mualphastar = getDouble(tokens, ClusterProperties.PMRA, indices);
            double mudelta = getDouble(tokens, ClusterProperties.PMDE, indices);
            double radvel = getDouble(tokens, ClusterProperties.RV, indices);
            double radius = getDouble(tokens, ClusterProperties.RADIUS, indices);
            int nstars = getInteger(tokens, ClusterProperties.NSTARS, indices);

            addCluster(names, ra, rarad, dec, decrad, dist, distpc, mualphastar, mudelta, radvel, radius, nstars);
        }

    }

    private void addCluster(String[] names, double ra, double rarad, double dec, double decrad, double dist, double distpc, double mualphastar, double mudelta, double radvel, double radius, int nstars) {
        Vector3b pos = Coordinates.sphericalToCartesian(rarad, decrad, new Apfloat(dist, Constants.PREC), new Vector3b());

        Vector3d pmv = AstroUtils.properMotionsToCartesian(mualphastar, mudelta, radvel, Math.toRadians(ra), Math.toRadians(dec), distpc, new Vector3d());

        Vector3d posSph = new Vector3d((float) ra, (float) dec, (float) dist);
        Vector3 pmSph = new Vector3((float) (mualphastar), (float) (mudelta), (float) radvel);

        // Create cluster archetype
        Entity entity = archetype.createEntity();

        var base = Mapper.base.get(entity);
        base.setNames(names);

        var graph = Mapper.graph.get(entity);
        graph.setParent(parentName != null ? parentName : "MWSC");

        var body = Mapper.body.get(entity);
        body.pos = pos;
        body.posSph = new Vector2d(posSph.x, posSph.y);
        body.setColor(new float[] { 0.93f, 0.93f, 0.3f, 1f });
        body.setLabelColor(new float[] { 0.93f, 0.93f, 0.3f, 1f });

        var pm = Mapper.pm.get(entity);
        pm.pm = pmv.put(new Vector3());
        pm.pmSph = pmSph;
        pm.hasPm = true;

        var cluster = Mapper.cluster.get(entity);
        cluster.raddeg = radius;
        cluster.numStars = nstars;
        cluster.dist = posSph.z;

        scene.engine.addEntity(entity);

        numLoaded += 1;
    }

    private String[] parseName(String name) {
        String[] names = name.split(Constants.nameSeparatorRegex);
        for (int i = 0; i < names.length; i++)
            names[i] = names[i].strip().replace("_", " ");
        return names;
    }

    private String get(String[] tokens, ClusterProperties prop, Map<ClusterProperties, Integer> indices) {
        return (!indices.containsKey(prop) || tokens[indices.get(prop)].isEmpty()) ? null : tokens[indices.get(prop)];
    }

    private Object get(Object[] row, ClusterProperties prop, Map<ClusterProperties, Integer> indices) {
        return (!indices.containsKey(prop) || row[indices.get(prop)] == null) ? null : row[indices.get(prop)];
    }

    private double getDouble(String[] tokens, ClusterProperties prop, Map<ClusterProperties, Integer> indices) {
        String s = get(tokens, prop, indices);
        if (s != null)
            return Parser.parseDouble(s);
        return 0;
    }

    private double getDouble(Object[] row, ClusterProperties prop, Map<ClusterProperties, Integer> indices, StarTable table, String defaultUnit) {
        Integer idx = indices.get(prop);
        if (idx != null) {
            ColumnInfo col = table.getColumnInfo(idx);
            String unit = (col.getUnitString() != null && !col.getUnitString().isBlank() ? col.getUnitString() : defaultUnit);
            Object obj = get(row, prop, indices);
            if (obj != null) {
                double val = ((Number) obj).doubleValue();
                if (Angle.isAngle(unit)) {
                    Angle angle = new Angle(val, unit);
                    return angle.get(AngleUnit.valueOf(defaultUnit.toUpperCase()));
                } else if (Length.isLength(unit)) {
                    Length length = new Length(val, unit);
                    return length.get(LengthUnit.valueOf(defaultUnit.toUpperCase()));
                } else {
                    // We just assume default unit
                    return val;
                }

            }
        }
        return 0;
    }

    private int getInteger(String[] tokens, ClusterProperties prop, Map<ClusterProperties, Integer> indices) {
        String s = get(tokens, prop, indices);
        if (s != null)
            return Parser.parseInt(s);
        return 0;
    }

    private int getInteger(Object[] row, ClusterProperties prop, Map<ClusterProperties, Integer> indices) {
        Object s = get(row, prop, indices);
        if (s != null)
            return ((Number) s).intValue();
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
            else if (UCDParser.isPllx(token))
                indices.put(ClusterProperties.PLLX, i);
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

    private boolean checkIndices(Map<ClusterProperties, Integer> indices) {
        return indices.containsKey(ClusterProperties.RA)
                && indices.containsKey(ClusterProperties.DEC)
                && (indices.containsKey(ClusterProperties.DIST) || indices.containsKey(ClusterProperties.PLLX))
                && indices.containsKey(ClusterProperties.RADIUS)
                && indices.containsKey(ClusterProperties.NAME);
    }

    private Map<ClusterProperties, Integer> parseHeader(StarTable table) {
        Map<ClusterProperties, Integer> indices = new HashMap<>();

        int ncols = table.getColumnCount();

        for (int i = 0; i < ncols; i++) {
            ColumnInfo ci = table.getColumnInfo(i);
            String cName = ci.getName();

            if (UCDParser.isName(cName))
                indices.put(ClusterProperties.NAME, i);
            else if (UCDParser.isRa(cName))
                indices.put(ClusterProperties.RA, i);
            else if (UCDParser.isDec(cName))
                indices.put(ClusterProperties.DEC, i);
            else if (UCDParser.isDist(cName))
                indices.put(ClusterProperties.DIST, i);
            else if (UCDParser.isPllx(cName))
                indices.put(ClusterProperties.PLLX, i);
            else if (UCDParser.isPmra(cName))
                indices.put(ClusterProperties.PMRA, i);
            else if (UCDParser.isPmde(cName))
                indices.put(ClusterProperties.PMDE, i);
            else if (UCDParser.isRadvel(cName))
                indices.put(ClusterProperties.RV, i);
            else if (UCDParser.isRadius(cName))
                indices.put(ClusterProperties.RADIUS, i);
            else if (UCDParser.isNstars(cName))
                indices.put(ClusterProperties.NSTARS, i);
        }

        return indices;
    }

}
