/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.scene.Archetype;
import gaiasky.scene.Mapper;
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
import net.jafama.FastMath;
import org.apfloat.Apfloat;
import uk.ac.starlink.table.*;
import uk.ac.starlink.table.formats.AsciiTableBuilder;
import uk.ac.starlink.table.formats.CsvTableBuilder;
import uk.ac.starlink.util.DataSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads star clusters in CSV or in STIL-supported formats.
 * @deprecated Modern cluster catalogs use the {@link gaiasky.data.group.STILDataProvider}.
 */
@Deprecated
public class StarClusterLoader extends AbstractSceneLoader {
    private static final Log logger = Logger.getLogger(StarClusterLoader.class);
    boolean active = true;
    private Archetype archetype;
    private int numLoaded = 0;

    private final float[] clusterColor = new float[] { 0.93f, 0.93f, 0.3f, 1f };

    private Array<Entity> clusters;

    public void setColor(double[] clusterColor) {
        this.clusterColor[0] = (float) clusterColor[0];
        this.clusterColor[1] = (float) clusterColor[1];
        this.clusterColor[2] = (float) clusterColor[2];
        if (clusterColor.length > 3) {
            this.clusterColor[3] = (float) clusterColor[3];
        } else {
            this.clusterColor[3] = 1;
        }
    }

    @Override
    public Array<Entity> loadData() {
        clusters = new Array<>();
        // The cluster archetype
        archetype = scene.archetypes().get("gaiasky.scenegraph.StarCluster");
        if (active) {
            numLoaded = 0;
            if (filePaths != null) {
                for (String file : filePaths) {
                    FileHandle f = Settings.settings.data.dataFileHandle(file);
                    try (InputStream is = f.read()) {
                        try {
                            loadClustersCsv(is, clusters);
                        } catch (IOException e) {
                            logger.error(e);
                        }
                    } catch (IOException e) {
                        logger.error(e);
                    }
                }
            } else if (dataSource != null) {
                try {
                    loadClustersStil(dataSource, clusters);
                } catch (IOException e) {
                    logger.error(e);
                }
            }
        }

        logger.info(I18n.msg("notif.catalog.init", numLoaded));
        return clusters;
    }

    public Array<Entity> getClusters() {
        return clusters;
    }

    @Override
    public void setName(String name) {

    }

    @Override
    public void setDescription(String description) {

    }

    @Override
    public void setParams(Map<String, Object> params) {
        if (params.containsKey("color")) {
            var col = params.get("color");
            if (col.getClass().isArray()) {
                double[] color = (double[]) col;
                setColor(color);
            }
        }
    }

    /**
     * Loads clusters from a STIL data source.
     *
     * @param ds   The data source.
     * @param list The list to put the loaded objects.
     */
    private void loadClustersStil(DataSource ds, Array<Entity> list) throws IOException {
        // Add extra builders
        StarTableFactory factory = new StarTableFactory();
        List<TableBuilder> builders = factory.getDefaultBuilders();
        builders.add(new CsvTableBuilder());
        builders.add(new AsciiTableBuilder());

        // Try to load
        StarTable table = factory.makeStarTable(ds);

        Map<ClusterProperties, Integer> indices = parseHeader(table);

        if (!checkIndices(indices)) {
            logger.error("At least 'ra', 'dec', 'pllx'|'parallax', 'dist'|'distance', 'radius' and 'name' are needed, please check your columns!");
            return;
        }
        RowSequence rs = table.getRowSequence();
        while (rs.next()) {
            Object[] row = rs.getRow();
            String[] names = parseName(row[indices.get(ClusterProperties.NAME)].toString());
            double ra = getDouble(row, ClusterProperties.RA, indices, table, "deg");
            double rarad = FastMath.toRadians(ra);
            double dec = getDouble(row, ClusterProperties.DEC, indices, table, "deg");
            double decrad = FastMath.toRadians(dec);
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

            addCluster(names, ra, rarad, dec, decrad, dist, distpc, mualphastar, mudelta, radvel, radius, nstars, list);
        }
    }

    /**
     * Loads clusters from a CSV file directly.
     *
     * @param data The CSV file input stream.
     * @param list The list to put the loaded objects.
     */
    private void loadClustersCsv(InputStream data, Array<Entity> list) throws IOException {
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
            double raRad = FastMath.toRadians(ra);
            double dec = getDouble(tokens, ClusterProperties.DEC, indices);
            double decRad = FastMath.toRadians(dec);
            double distPc = 0;
            if (indices.containsKey(ClusterProperties.DIST)) {
                distPc = getDouble(tokens, ClusterProperties.DIST, indices);
            } else if (indices.containsKey(ClusterProperties.PLLX)) {
                distPc = 1000d / getDouble(tokens, ClusterProperties.PLLX, indices);
            }
            double distInternal = distPc * Constants.PC_TO_U;
            double muAlphaStar = getDouble(tokens, ClusterProperties.PMRA, indices);
            double muDelta = getDouble(tokens, ClusterProperties.PMDE, indices);
            double radVel = getDouble(tokens, ClusterProperties.RV, indices);
            double radiusDeg = getDouble(tokens, ClusterProperties.RADIUS, indices);
            int nStars = getInteger(tokens, ClusterProperties.NSTARS, indices);

            addCluster(names, ra, raRad, dec, decRad, distInternal, distPc, muAlphaStar, muDelta, radVel, radiusDeg, nStars, list);
        }

    }

    private void addCluster(String[] names, double ra, double raRad, double dec, double decRad, double dist, double distPc, double muAlphaStar, double muDelta, double radVel, double radiusDeg, int nStars, Array<Entity> list) {
        Vector3b pos = Coordinates.sphericalToCartesian(raRad, decRad, new Apfloat(dist, Constants.PREC), new Vector3b());

        Vector3d pmv = Coordinates.properMotionsToCartesian(muAlphaStar, muDelta, radVel, FastMath.toRadians(ra), FastMath.toRadians(dec), distPc, new Vector3d());

        Vector3d posSph = new Vector3d((float) ra, (float) dec, (float) dist);
        Vector3 pmSph = new Vector3((float) (muAlphaStar), (float) (muDelta), (float) radVel);

        // Create cluster archetype
        Entity entity = archetype.createEntity();

        var base = Mapper.base.get(entity);
        base.setNames(names);

        var graph = Mapper.graph.get(entity);
        graph.setParent(parentName != null ? parentName : "MWSC");

        var body = Mapper.body.get(entity);
        body.setPos(pos);
        body.posSph = new Vector2d(posSph.x, posSph.y);
        body.setColor(Arrays.copyOf(clusterColor, clusterColor.length));
        body.setLabelColor(Arrays.copyOf(clusterColor, clusterColor.length));

        var pm = Mapper.pm.get(entity);
        pm.pm = pmv.put(new Vector3());
        pm.pmSph = pmSph;
        pm.hasPm = true;

        var cluster = Mapper.cluster.get(entity);
        cluster.radiusDeg = radiusDeg;
        cluster.numStars = nStars;
        cluster.dist = posSph.z;

        list.add(entity);

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

    private int getInteger(String[] tokens, ClusterProperties properties, Map<ClusterProperties, Integer> indices) {
        String s = get(tokens, properties, indices);
        if (s != null)
            return Parser.parseInt(s);
        return 0;
    }

    private int getInteger(Object[] row, ClusterProperties properties, Map<ClusterProperties, Integer> indices) {
        Object s = get(row, properties, indices);
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

        int nColumns = table.getColumnCount();

        for (int i = 0; i < nColumns; i++) {
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

}
