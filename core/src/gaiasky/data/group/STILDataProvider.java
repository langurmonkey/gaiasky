/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.LongMap;
import gaiasky.scenegraph.ParticleGroup.ParticleBean;
import gaiasky.scenegraph.StarGroup.StarBean;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Vector3d;
import gaiasky.util.ucd.UCD;
import gaiasky.util.ucd.UCDParser;
import gaiasky.util.units.Position;
import gaiasky.util.units.Position.PositionType;
import gaiasky.util.units.Quantity.Angle;
import gaiasky.util.units.Quantity.Angle.AngleUnit;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.formats.AsciiTableBuilder;
import uk.ac.starlink.table.formats.CsvTableBuilder;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Loads VOTables, FITS, etc. This data provider makes educated guesses using UCDs and column names to
 * match columns to attributes.
 * <p>
 * More information on this can be found <a href="http://gaia.ari.uni-heidelberg.de/gaiasky/docs/html/latest/SAMP.html#stil-data-provider">here</a>.
 *
 * @author tsagrista
 */
public class STILDataProvider extends AbstractStarGroupDataProvider {
    private static Log logger = Logger.getLogger(STILDataProvider.class);
    private StarTableFactory factory;
    private long starid = 10000000;
    // Dataset options, may be null
    private DatasetOptions dops;


    public STILDataProvider() {
        super();
        // Disable logging
        java.util.logging.Logger.getLogger("org.astrogrid").setLevel(Level.OFF);
        factory = new StarTableFactory();
        countsPerMag = new long[22];
        initLists();
    }

    public void setDatasetOptions(DatasetOptions dops){
        this.dops = dops;
    }

    @Override
    public Array<ParticleBean> loadData(String file, double factor, boolean compat) {
        logger.info(I18n.bundle.format("notif.datafile", file));
        try {
            loadData(new FileDataSource(GlobalConf.data.dataFile(file)), factor, compat);
        } catch (Exception e1) {
            try {
                logger.info("File " + file + " not found in data folder, trying relative path");
                loadData(new FileDataSource(file), factor, compat);
            } catch (Exception e2) {
                logger.error(e1);
                logger.error(e2);
            }
        }
        logger.info(I18n.bundle.format("notif.nodeloader", list.size, file));
        return list;
    }

    /**
     * Gets the first ucd that can be translated to a double from the set.
     *
     * @param ucds The array of UCDs. The UCDs which coincide with the names should be first.
     * @param row  The row objects
     * @return Pair of <UCD,Double>
     */
    private Pair<UCD, Double> getDoubleUcd(Array<UCD> ucds, Object[] row) {
        for (UCD ucd : ucds) {
            try {
                double num = ((Number) row[ucd.index]).doubleValue();
                if (Double.isNaN(num)) {
                    throw new Exception();
                }
                return new Pair<>(ucd, num);
            } catch (Exception e) {
                // not working, try next
            }
        }
        return null;
    }

    /**
     * Gets the first ucd as a string from the set.
     *
     * @param ucds The set of UCD objects
     * @param row  The row
     * @return
     */
    private Pair<UCD, String> getStringUcd(Array<UCD> ucds, Object[] row) {
        for (UCD ucd : ucds) {
            try {
                String str = row[ucd.index].toString();
                return new Pair<>(ucd, str);
            } catch (Exception e) {
                // not working, try next
            }
        }
        return null;
    }

    public Array<? extends ParticleBean> loadData(DataSource ds, double factor) {
        return loadData(ds, factor, true);
    }

    public Array<? extends ParticleBean> loadData(DataSource ds, double factor, boolean compat) {

        try {
            // Add extra builders
            List builders = factory.getDefaultBuilders();
            builders.add(new CsvTableBuilder());
            builders.add(new AsciiTableBuilder());

            // Try to load
            StarTable table = factory.makeStarTable(ds);

            initLists((int) table.getRowCount());

            UCDParser ucdp = new UCDParser();
            ucdp.parse(table);

            if (ucdp.haspos) {
                int i = 0;
                RowSequence rs = table.getRowSequence();
                while (rs.next()) {
                    Object[] row = rs.getRow();
                    boolean skip = false;
                    try {
                        /* POSITION */
                        Pair<UCD, Double> a = getDoubleUcd(ucdp.POS1, row);
                        Pair<UCD, Double> b = getDoubleUcd(ucdp.POS2, row);
                        Pair<UCD, Double> c;
                        String unitc;

                        if (ucdp.POS3.isEmpty() || getDoubleUcd(ucdp.POS3, row) == null) {
                            c = new Pair<>(null, 0.04);
                            unitc = "mas";
                        } else {
                            c = getDoubleUcd(ucdp.POS3, row);
                            unitc = c.getFirst().unit;
                        }

                        PositionType pt = ucdp.getPositionType(a.getFirst(), b.getFirst(), c.getFirst());
                        // Check negative parallaxes. What to do?
                        // Simply ignore object
                        if (pt.isParallax() && (c.getSecond() == null || c.getSecond().isNaN() || c.getSecond() <= 0)) {
                            skip = true;
                        }

                        Position p = new Position(a.getSecond(), a.getFirst().unit, b.getSecond(), b.getFirst().unit, c.getSecond(), unitc, pt);

                        double distpc = p.gsposition.len();
                        if((pt.isParallax() && c.getSecond() <= 0) || !Double.isFinite(distpc) || distpc < 0){
                            // Next
                            break;
                        }

                        p.gsposition.scl(Constants.PC_TO_U);
                        // Find out RA/DEC/Dist
                        Vector3d sph = new Vector3d();
                        Coordinates.cartesianToSpherical(p.gsposition, sph);

                        /* PROPER MOTION */
                        Vector3d pm;
                        double mualphastar = 0, mudelta = 0, radvel = 0;
                        // Only supported if position is equatorial spherical coordinates (ra/dec)
                        if (pt == PositionType.EQ_SPH_DIST || pt == PositionType.EQ_SPH_PLX) {
                            Pair<UCD, Double> pma = getDoubleUcd(ucdp.PMRA, row);
                            Pair<UCD, Double> pmb = getDoubleUcd(ucdp.PMDEC, row);
                            Pair<UCD, Double> pmc = getDoubleUcd(ucdp.RADVEL, row);

                            mualphastar = pma != null ? pma.getSecond() : 0;
                            mudelta = pmb != null ? pmb.getSecond() : 0;
                            radvel = pmc != null ? pmc.getSecond() : 0;

                            double rarad = new Angle(a.getSecond(), a.getFirst().unit).get(AngleUnit.RAD);
                            double decrad = new Angle(b.getSecond(), b.getFirst().unit).get(AngleUnit.RAD);
                            pm = AstroUtils.properMotionsToCartesian(mualphastar, mudelta, radvel, rarad, decrad, distpc);
                        } else {
                            pm = new Vector3d(Vector3d.Zero);
                        }

                        /* MAGNITUDE */
                        double appmag;
                        if (!ucdp.MAG.isEmpty()) {
                            Pair<UCD, Double> appmagPair = getDoubleUcd(ucdp.MAG, row);
                            appmag = appmagPair.getSecond();
                        } else {
                            // Default magnitude
                            appmag = 15;
                        }
                        // Scale magnitude if needed
                        double magscl = (dops != null && dops.type == DatasetOptions.DatasetLoadType.STARS) ? dops.magnitudeScale : 0f;
                        appmag -= magscl;

                        double absmag = appmag - 5 * Math.log10((distpc <= 0 ? 10 : distpc)) + 5;
                        // Pseudo-luminosity. Usually L = L0 * 10^(-0.4*Mbol). We omit M0 and approximate Mbol = M
                        double pseudoL = Math.pow(10, -0.4 * absmag);
                        double sizeFactor = Nature.PC_TO_M * Constants.ORIGINAL_M_TO_U * 0.15;
                        double size = Math.min((Math.pow(pseudoL, 0.45) * sizeFactor), 1e10);
                        size *= Constants.DISTANCE_SCALE_FACTOR;

                        /* COLOR */
                        float color;
                        if (!ucdp.COL.isEmpty()) {
                            Pair<UCD, Double> colPair = getDoubleUcd(ucdp.COL, row);
                            if (colPair == null) {
                                color = 0.656f;
                            } else {
                                color = colPair.getSecond().floatValue();
                            }
                        } else {
                            // Default color
                            color = 0.656f;
                        }
                        float[] rgb = ColorUtils.BVtoRGB(color);
                        double col = Color.toFloatBits(rgb[0], rgb[1], rgb[2], 1.0f);

                        /* IDENTIFIER AND NAME */
                        String[] names;
                        Long id;
                        int hip = -1;
                        if (ucdp.NAME.isEmpty()) {
                            // Empty name
                            if (!ucdp.ID.isEmpty()) {
                                // We have ID
                                Pair<UCD, String> namePair = getStringUcd(ucdp.ID, row);
                                names = new String[] { namePair.getSecond() };
                                if (namePair.getFirst().colname.equalsIgnoreCase("hip")) {
                                    hip = Integer.valueOf(namePair.getSecond());
                                    id = (long) hip;
                                } else {
                                    id = ++starid;
                                }
                            } else {
                                // Emtpy ID
                                id = ++starid;
                                names = new String[] { id.toString() };
                            }
                        } else {
                            // We have name
                            Pair<UCD, String> namePair = getStringUcd(ucdp.NAME, row);
                            names = namePair.getSecond().split("\\|");
                            // Take care of HIP stars
                            if (!ucdp.ID.isEmpty()) {
                                Pair<UCD, String> idpair = getStringUcd(ucdp.ID, row);
                                if (idpair.getFirst().colname.equalsIgnoreCase("hip")) {
                                    hip = Integer.valueOf(idpair.getSecond());
                                    id = (long) hip;
                                } else {
                                    id = ++starid;
                                }
                            } else {
                                id = ++starid;
                            }
                        }

                        if (mustLoad(id)) {
                            // Check must load
                            skip = false;
                        }

                        if (skip) {
                            // Next
                            continue;
                        }

                        // Populate provider lists
                        colors.put(id, rgb);
                        sphericalPositions.put(id, new double[] { sph.x, sph.y, sph.z });

                        if(dops == null || dops.type == DatasetOptions.DatasetLoadType.STARS) {
                            double[] point = new double[StarBean.SIZE + 3];
                            point[StarBean.I_HIP] = hip;
                            point[StarBean.I_X] = p.gsposition.x;
                            point[StarBean.I_Y] = p.gsposition.y;
                            point[StarBean.I_Z] = p.gsposition.z;
                            point[StarBean.I_PMX] = pm.x;
                            point[StarBean.I_PMY] = pm.y;
                            point[StarBean.I_PMZ] = pm.z;
                            point[StarBean.I_MUALPHA] = mualphastar;
                            point[StarBean.I_MUDELTA] = mudelta;
                            point[StarBean.I_RADVEL] = radvel;
                            point[StarBean.I_COL] = col;
                            point[StarBean.I_SIZE] = size;
                            point[StarBean.I_APPMAG] = appmag;
                            point[StarBean.I_ABSMAG] = absmag;

                            // Extra
                            Map<UCD, Double> extraAttributes = addExtraAttributes(ucdp, row);

                            StarBean sb = new StarBean(point, id, names, extraAttributes);
                            list.add(sb);

                            int appclmp = (int) MathUtilsd.clamp(appmag, 0, 21);
                            countsPerMag[appclmp] += 1;
                        } else if(dops.type == DatasetOptions.DatasetLoadType.PARTICLES){
                            double[] point = new double[3];
                            point[ParticleBean.I_X] = p.gsposition.x;
                            point[ParticleBean.I_Y] = p.gsposition.y;
                            point[ParticleBean.I_Z] = p.gsposition.z;

                            // TODO reorganise existing star properties into extra attributes

                            // Extra
                            Map<UCD, Double> extraAttributes = addExtraAttributes(ucdp, row);

                            ParticleBean pb = new ParticleBean(point, names, extraAttributes);
                            list.add(pb);
                        }

                    } catch (Exception e) {
                        logger.debug(e);
                        logger.debug("Exception parsing row " + i + ": skipping");
                    }
                    i++;
                }
            } else {
                logger.error("Table not loaded: Position not found");
            }

        } catch (Exception e) {
            logger.error(e);
        }

        return list;
    }

    private Map<UCD, Double> addExtraAttributes(UCDParser ucdp, Object[] row){
        // Extra
        Map<UCD, Double> extraAttributes = null;
        for (UCD extra : ucdp.extra) {
            Double val = Double.NaN;
            try {
                val = ((Number) row[extra.index]).doubleValue();
            } catch (Exception e) {
                Object o = row[extra.index];
                if(o instanceof Character){
                   Character c = (Character) o;
                   val = (double) c.charValue();
                }
            }
            if (extraAttributes == null)
                extraAttributes = new HashMap<>();
            extraAttributes.put(extra, val);
        }
        return extraAttributes;
    }

    @Override
    public Array<ParticleBean> loadData(InputStream is, double factor, boolean compat) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Array<ParticleBean> loadDataMapped(String file, double factor, boolean compat) {
        return null;
    }

    @Override
    public void setFileNumberCap(int cap) {
    }

    @Override
    public LongMap<float[]> getColors() {
        return null;
    }

    @Override
    public void setParallaxErrorFactorFaint(double parallaxErrorFactor) {

    }

    @Override
    public void setParallaxErrorFactorBright(double parallaxErrorFactor) {

    }

    @Override
    public void setParallaxZeroPoint(double parallaxZeroPoint) {
    }

    @Override
    public void setMagCorrections(boolean magCorrections) {
    }

}
