package gaia.cu9.ari.gaiaorbit.data.group;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.LongMap;

import gaia.cu9.ari.gaiaorbit.scenegraph.ParticleGroup.ParticleBean;
import gaia.cu9.ari.gaiaorbit.scenegraph.StarGroup.StarBean;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.Pair;
import gaia.cu9.ari.gaiaorbit.util.color.ColourUtils;
import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.ucd.UCD;
import gaia.cu9.ari.gaiaorbit.util.ucd.UCDParser;
import gaia.cu9.ari.gaiaorbit.util.units.Position;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;

/**
 * Loads VOTables, FITS, etc.
 * @author tsagrista
 *
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
    }

    @Override
    public Array<? extends ParticleBean> loadData(String file) {
        return loadData(file, 1.0f);
    }

    @Override
    public Array<? extends ParticleBean> loadData(String file, double factor) {
        logger.info(this.getClass().getSimpleName(), I18n.bundle.format("notif.datafile", file));
        try {
            loadData(new FileDataSource(file), factor);
        } catch (Exception e) {
            logger.error(e);
        }
        return list;
    }

    /**
     * Gets the first ucd that can be translated to a double from the set.
     * @param ucds
     * @param row
     * @return
     */
    private Pair<UCD, Double> getDoubleUcd(Set<UCD> ucds, Object[] row) {
        for (UCD ucd : ucds) {
            try {
                double num = ((Number) row[ucd.index]).doubleValue();
                if (Double.isNaN(num)) {
                    throw new Exception();
                }
                return new Pair<UCD, Double>(ucd, num);
            } catch (Exception e) {
                // not working, try next
            }
        }
        return null;
    }

    /**
     * Gets the first ucd as a string from the set.
     * @param ucds
     * @param row
     * @return
     */
    private Pair<UCD, String> getStringUcd(Set<UCD> ucds, Object[] row) {
        for (UCD ucd : ucds) {
            try {
                String str = row[ucd.index].toString();
                return new Pair<UCD, String>(ucd, str);
            } catch (Exception e) {
                // not working, try next
            }
        }
        return null;
    }

    public Array<? extends ParticleBean> loadData(DataSource ds, double factor) {

        try {
            TableSequence ts = factory.makeStarTables(ds);
            // Find table
            List<StarTable> tables = new LinkedList<StarTable>();
            StarTable table = null;
            long maxElems = 0;
            for (StarTable t; (t = ts.nextTable()) != null;) {
                tables.add(t);
                if (t.getRowCount() > maxElems) {
                    maxElems = t.getRowCount();
                    table = t;
                }
            }

            initLists((int) table.getRowCount());

            UCDParser ucdp = new UCDParser();
            ucdp.parse(table);

            if (ucdp.haspos) {
                long rowcount = table.getRowCount();
                for (long i = 0; i < rowcount; i++) {
                    Object[] row = table.getRow(i);

                    try {
                        /** POSITION **/
                        Pair<UCD, Double> a = getDoubleUcd(ucdp.POS1, row);
                        Pair<UCD, Double> b = getDoubleUcd(ucdp.POS2, row);
                        Pair<UCD, Double> c;
                        String unitc;

                        if (ucdp.POS3.isEmpty() || getDoubleUcd(ucdp.POS3, row) == null) {
                            c = new Pair<UCD, Double>(null, 0.04);
                            unitc = "mas";
                        } else {
                            c = getDoubleUcd(ucdp.POS3, row);
                            unitc = c.getFirst().unit;
                        }

                        Position p = new Position(a.getSecond(), a.getFirst().unit, b.getSecond(), b.getFirst().unit, c.getSecond(), unitc, ucdp.getPositionType(a.getFirst(), b.getFirst(), c.getFirst()));
                        double distpc = p.gsposition.len();
                        p.gsposition.scl(Constants.PC_TO_U);
                        // Find out RA/DEC/Dist
                        Vector3d sph = new Vector3d();
                        Coordinates.cartesianToSpherical(p.gsposition, sph);

                        /** MAGNITUDE **/
                        double appmag;
                        if (!ucdp.MAG.isEmpty()) {
                            Pair<UCD, Double> appmagpair = getDoubleUcd(ucdp.MAG, row);
                            appmag = appmagpair.getSecond();
                        } else {
                            // Default magnitude
                            appmag = 15;
                        }
                        double absmag = (appmag - 2.5 * Math.log10(Math.pow(distpc / 10d, 2d)));
                        double flux = Math.pow(10, -absmag / 2.5f);
                        double size = Math.min((Math.pow(flux, 0.5f) * Constants.PC_TO_U * 0.16f), 1e9f) / 1.5;

                        /** COLOR **/
                        float color;
                        if (!ucdp.COL.isEmpty()) {
                            Pair<UCD, Double> colpair = getDoubleUcd(ucdp.COL, row);
                            if (colpair == null) {
                                color = 0.656f;
                            } else {
                                color = colpair.getSecond().floatValue();
                            }
                        } else {
                            // Default color
                            color = 0.656f;
                        }
                        float[] rgb = ColourUtils.BVtoRGB(color);
                        double col = Color.toFloatBits(rgb[0], rgb[1], rgb[2], 1.0f);

                        /** IDENTIFIER AND NAME **/
                        String name;
                        Long id;
                        if (!ucdp.ID.isEmpty()) {
                            Pair<UCD, String> namepair = getStringUcd(ucdp.ID, row);
                            name = namepair.getSecond();
                            id = ++starid;
                        } else {
                            id = ++starid;
                            name = id.toString();
                        }

                        double[] point = new double[StarBean.SIZE];
                        point[StarBean.I_HIP] = -1;
                        point[StarBean.I_TYC1] = -1;
                        point[StarBean.I_TYC2] = -1;
                        point[StarBean.I_TYC3] = -1;
                        point[StarBean.I_X] = p.gsposition.x;
                        point[StarBean.I_Y] = p.gsposition.y;
                        point[StarBean.I_Z] = p.gsposition.z;
                        point[StarBean.I_PMX] = 0;
                        point[StarBean.I_PMY] = 0;
                        point[StarBean.I_PMZ] = 0;
                        point[StarBean.I_MUALPHA] = 0;
                        point[StarBean.I_MUDELTA] = 0;
                        point[StarBean.I_RADVEL] = 0;
                        point[StarBean.I_COL] = col;
                        point[StarBean.I_SIZE] = size;
                        //point[StarBean.I_RADIUS] = radius;
                        //point[StarBean.I_TEFF] = teff;
                        point[StarBean.I_APPMAG] = appmag;
                        point[StarBean.I_ABSMAG] = absmag;

                        list.add(new StarBean(point, id, name));
                    } catch (Exception e) {
                        logger.debug(e);
                        logger.debug("Exception parsing row " + i + ": skipping");
                    }

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
