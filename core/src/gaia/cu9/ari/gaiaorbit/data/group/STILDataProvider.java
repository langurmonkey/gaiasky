package gaia.cu9.ari.gaiaorbit.data.group;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.LongMap;
import gaia.cu9.ari.gaiaorbit.scenegraph.ParticleGroup.ParticleBean;
import gaia.cu9.ari.gaiaorbit.scenegraph.StarGroup.StarBean;
import gaia.cu9.ari.gaiaorbit.util.*;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.color.ColourUtils;
import gaia.cu9.ari.gaiaorbit.util.coord.AstroUtils;
import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.ucd.UCD;
import gaia.cu9.ari.gaiaorbit.util.ucd.UCDParser;
import gaia.cu9.ari.gaiaorbit.util.units.Position;
import gaia.cu9.ari.gaiaorbit.util.units.Position.PositionType;
import gaia.cu9.ari.gaiaorbit.util.units.Quantity.Angle;
import gaia.cu9.ari.gaiaorbit.util.units.Quantity.Angle.AngleUnit;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.formats.AsciiTableBuilder;
import uk.ac.starlink.table.formats.CsvTableBuilder;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Loads VOTables, FITS, etc. This data provider makes educated guesses using UCDs and column names to
 * match columns to attributes.
 *
 * More information on this can be found <a href="http://gaia.ari.uni-heidelberg.de/gaiasky/docs/html/latest/SAMP.html#stil-data-provider">here</a>.
 *
 * @author tsagrista
 */
public class STILDataProvider extends AbstractStarGroupDataProvider {
    private static Log logger = Logger.getLogger(STILDataProvider.class);
    private StarTableFactory factory;
    private long starid = 10000000;

    public STILDataProvider() {
        super();
        // Disable logging
        java.util.logging.Logger.getLogger("org.astrogrid").setLevel(Level.OFF);
        factory = new StarTableFactory();
        countsPerMag = new long[22];
        initLists();
    }

    @Override
    public Array<? extends ParticleBean> loadData(String file) {
        return loadData(file, 1.0f);
    }

    @Override
    public Array<? extends ParticleBean> loadData(String file, double factor) {
        logger.info(I18n.bundle.format("notif.datafile", file));
        try {
            loadData(new FileDataSource(GlobalConf.data.dataFile(file)), factor);
        } catch (Exception e) {
            logger.error(e);
        }
        logger.info(I18n.bundle.format("notif.nodeloader", list.size, file));
        return list;
    }

    /**
     * Gets the first ucd that can be translated to a double from the set.
     * @param ucds The set of UCDs
     * @param row The row objects
     * @return Pair of <UCD,Double>
     */
    private Pair<UCD, Double> getDoubleUcd(Set<UCD> ucds, Object[] row) {
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
     * @param row The row
     * @return
     */
    private Pair<UCD, String> getStringUcd(Set<UCD> ucds, Object[] row) {
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
                while(rs.next()){
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
                        double absmag = (appmag - 2.5 * Math.log10(Math.pow(distpc / 10.0, 2.0)));
                        double flux = Math.pow(10, -absmag / 2.5);
                        double size = Math.min((Math.pow(flux, 0.5) * Constants.PC_TO_U * 0.16), 1e9) / 1.5;

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
                        float[] rgb = ColourUtils.BVtoRGB(color);
                        double col = Color.toFloatBits(rgb[0], rgb[1], rgb[2], 1.0f);

                        /* IDENTIFIER AND NAME */
                        String name;
                        Long id;
                        int hip = -1;
                        if (ucdp.NAME.isEmpty()) {
                            // Empty name
                            if (!ucdp.ID.isEmpty()) {
                                // We have ID
                                Pair<UCD, String> namePair = getStringUcd(ucdp.ID, row);
                                name = namePair.getSecond();
                                if (namePair.getFirst().colname.equalsIgnoreCase("hip")) {
                                    hip = Integer.valueOf(namePair.getSecond());
                                    id = (long) hip;
                                } else {
                                    id = ++starid;
                                }
                            } else {
                                // Emtpy ID
                                id = ++starid;
                                name = id.toString();
                            }
                        } else {
                            // We have name
                            Pair<UCD, String> namePair = getStringUcd(ucdp.NAME, row);
                            name = namePair.getSecond();
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
                        sphericalPositions.put(id, new double[]{sph.x, sph.y, sph.z});

                        double[] point = new double[StarBean.SIZE];
                        point[StarBean.I_HIP] = hip;
                        point[StarBean.I_TYC1] = -1;
                        point[StarBean.I_TYC2] = -1;
                        point[StarBean.I_TYC3] = -1;
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
                        //point[StarBean.I_RADIUS] = radius;
                        //point[StarBean.I_TEFF] = teff;
                        point[StarBean.I_APPMAG] = appmag;
                        point[StarBean.I_ABSMAG] = absmag;

                        list.add(new StarBean(point, id, name));

                        int appclmp = (int) MathUtilsd.clamp(appmag, 0, 21);
                        countsPerMag[appclmp] += 1;
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

    @Override
    public Array<? extends ParticleBean> loadData(InputStream is, double factor) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Array<? extends ParticleBean> loadDataMapped(String file, double factor) {
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
