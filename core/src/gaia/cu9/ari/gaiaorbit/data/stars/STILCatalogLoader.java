package gaia.cu9.ari.gaiaorbit.data.stars;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.scenegraph.AbstractPositionEntity;
import gaia.cu9.ari.gaiaorbit.scenegraph.CelestialBody;
import gaia.cu9.ari.gaiaorbit.scenegraph.Particle;
import gaia.cu9.ari.gaiaorbit.scenegraph.Star;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.units.Position;
import gaia.cu9.ari.gaiaorbit.util.units.Position.PositionType;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;

public class STILCatalogLoader extends AbstractCatalogLoader {
    private static final Log logger = Logger.getLogger(STILCatalogLoader.class);

    @Override
    public Array<AbstractPositionEntity> loadData() throws FileNotFoundException {
        long starid = 1000000;

        Array<AbstractPositionEntity> result = new Array<AbstractPositionEntity>();
        StarTableFactory factory = new StarTableFactory();
        logger.info(I18n.bundle.format("notif.limitmag", GlobalConf.data.LIMIT_MAG_LOAD));

        for (String file : files) {
            logger.info(I18n.bundle.format("notif.datafile", file));
            try {
                Map<String, ColumnInfo> ucds = new HashMap<String, ColumnInfo>();
                Map<String, Integer> ucdsi = new HashMap<String, Integer>();
                Map<String, ColumnInfo> colnames = new HashMap<String, ColumnInfo>();
                Map<String, Integer> colnamesi = new HashMap<String, Integer>();

                DataSource ds = new FileDataSource(file);
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
                logger.info("Selected table " + table.getName() + ": " + table.getRowCount() + " elements");

                int count = table.getColumnCount();
                ColumnInfo[] colInfo = new ColumnInfo[count];
                for (int i = 0; i < count; i++) {
                    colInfo[i] = table.getColumnInfo(i);
                    ucds.put(colInfo[i].getUCD(), colInfo[i]);
                    ucdsi.put(colInfo[i].getUCD(), i);
                    colnames.put(colInfo[i].getName(), colInfo[i]);
                    colnamesi.put(colInfo[i].getName(), i);
                }

                /** POSITION **/

                ColumnInfo ac = null, bc = null, cc = null;
                int ai, bi, ci;
                PositionType type = null;
                int tychoi = -1;
                int hipi = -1;

                // Check positions
                if (ucds.containsKey("pos.eq.ra")) {
                    // RA_DEC_DIST or RA_DEC_PLX
                    ac = ucds.get("pos.eq.ra");
                    bc = ucds.get("pos.eq.dec");
                    cc = ucds.containsKey("pos.parallax.trig") ? ucds.get("pos.parallax.trig") : ucds.get("pos.distance");
                    type = ucds.containsKey("pos.parallax.trig") ? PositionType.EQ_SPH_PLX : PositionType.EQ_SPH_DIST;
                }

                if (type == null && ucds.containsKey("pos.galactic.lon")) {
                    // GLON_GLAT_DIST or GLON_GLAT_PLX
                    ac = ucds.get("pos.galactic.lon");
                    bc = ucds.get("pos.galactic.lat");
                    cc = ucds.containsKey("pos.parallax.trig") ? ucds.get("pos.parallax.trig") : ucds.get("pos.distance");
                    type = ucds.containsKey("pos.parallax.trig") ? PositionType.GAL_SPH_PLX : PositionType.GAL_SPH_DIST;
                }

                if (type == null && ucds.containsKey("pos.eq.x")) {
                    // Equatorial XYZ
                    ac = ucds.get("pos.eq.x");
                    bc = ucds.get("pos.eq.y");
                    cc = ucds.get("pos.eq.z");
                    type = PositionType.EQ_XYZ;
                }

                if (type == null && ucds.containsKey("pos.galactic.x")) {
                    // Galactic XYZ
                    ac = ucds.get("pos.galactic.x");
                    bc = ucds.get("pos.galactic.y");
                    cc = ucds.get("pos.galactic.z");
                    type = PositionType.GAL_XYZ;
                }

                if (type == null) {
                    throw new RuntimeException("Could not find suitable position candidate columns");
                } else {
                    ai = ucdsi.get(ac.getUCD());
                    bi = ucdsi.get(bc.getUCD());
                    ci = ucdsi.get(cc.getUCD());
                }

                /** CHECK FOR TYCHO NUMBER **/
                if (colnames.containsKey("TYC")) {
                    tychoi = colnamesi.get("TYC");
                }

                /** CHECK FOR HIP NUMBER **/
                if (colnames.containsKey("HIP")) {
                    hipi = colnamesi.get("HIP");
                }

                /** APP MAGNITUDE **/
                ColumnInfo magc = null;
                int magi;
                if (ucds.containsKey("phot.mag;em.opt.V")) {
                    magc = ucds.get("phot.mag;em.opt.V");
                } else if (ucds.containsKey("phot.mag;em.opt.B")) {
                    magc = ucds.get("phot.mag;em.opt.B");
                } else if (ucds.containsKey("phot.mag;em.opt.I")) {
                    magc = ucds.get("phot.mag;em.opt.I");
                } else if (ucds.containsKey("phot.mag;em.opt.R")) {
                    magc = ucds.get("phot.mag;em.opt.R");
                } else {
                    throw new RuntimeException("Could not find suitable magnitude candidate column");
                }
                magi = ucdsi.get(magc.getUCD());

                /** ABS MAGNITUDE **/
                ColumnInfo abmagc = null;
                int abmagi;
                if (ucds.containsKey("phys.magAbs;em.opt.V")) {
                    abmagc = ucds.get("phys.magAbs;em.opt.V");
                } else if (ucds.containsKey("phys.magAbs;em.opt.B")) {
                    abmagc = ucds.get("phys.magAbs;em.opt.B");
                } else if (ucds.containsKey("phys.magAbs;em.opt.I")) {
                    abmagc = ucds.get("phys.magAbs;em.opt.I");
                } else if (ucds.containsKey("phys.magAbs;em.opt.R")) {
                    abmagc = ucds.get("phys.magAbs;em.opt.R");
                } else {
                    abmagc = magc;
                }
                abmagi = ucdsi.get(abmagc.getUCD());

                /** COLOR **/
                ColumnInfo colc = null;
                int coli = -1;
                if (ucds.containsKey("phot.color;em.opt.B;em.opt.V")) {
                    // B-V
                    colc = ucds.get("phot.color;em.opt.B;em.opt.V");
                }
                if (colc != null) {
                    coli = ucdsi.get(colc.getUCD());
                }

                /** NAME **/
                ColumnInfo idstrc = null;
                ColumnInfo idc = null;
                int idstri = 0, idi = 0;
                if (ucds.containsKey("meta.id")) {
                    idstrc = ucds.get("meta.id");
                    idstri = ucdsi.get("meta.id");
                }
                if (ucds.containsKey("meta.id;meta.main")) {
                    idc = ucds.get("meta.id;meta.main");
                    idi = ucdsi.get("meta.id;meta.main");
                }

                long rowcount = table.getRowCount();
                for (long i = 0; i < rowcount; i++) {
                    Object[] row = table.getRow(i);

                    String tycho = "";
                    if (tychoi >= 0 && row[tychoi] != null)
                        tycho = (String) row[tychoi];

                    int hip = -1;
                    if (hipi >= 0 && row[hipi] != null)
                        hip = ((Number) row[hipi]).intValue();

                    double a = ((Number) row[ai]).doubleValue();
                    double b = ((Number) row[bi]).doubleValue();
                    double c = ((Number) row[ci]).doubleValue();
                    Position p = new Position(a, ac.getUnitString(), b, bc.getUnitString(), c, cc.getUnitString(), type);
                    double dist = p.gsposition.len();
                    p.gsposition.scl(Constants.PC_TO_U);
                    // Find out RA/DEC/Dist
                    Vector3d sph = new Vector3d();
                    Coordinates.cartesianToSpherical(p.gsposition, sph);

                    float mag = ((Number) row[magi]).floatValue();
                    float absmag = ((Number) row[abmagi]).floatValue();

                    float color = coli > 0 ? ((Number) row[coli]).floatValue() : 0.656f;

                    starid++;
                    String idstr = null;
                    if (idstrc == null || !idstrc.getContentClass().isAssignableFrom(String.class)) {
                        // ID string from catalog if possible
                        if (hipi >= 0 && row[hipi] != null) {
                            idstr = "HIP" + row[hipi];
                        } else if (tychoi >= 0 && row[tychoi] != null) {
                            idstr = "TYC" + row[tychoi];
                        } else {
                            idstr = "star_" + starid;
                        }

                    } else {
                        idstr = (String) row[idstri];
                    }

                    Long id = (idc == null || !idc.getContentClass().isAssignableFrom(Number.class)) ? starid : ((Number) row[idi]).longValue();

                    CelestialBody s = null;
                    if (dist > 2.2e7) {
                        // Galaxy
                        s = new Particle(p.gsposition, mag, absmag, color, idstr, (float) Math.toDegrees(sph.x), (float) Math.toDegrees(sph.y), id);
                    } else {
                        s = new Star(p.gsposition, mag, absmag, color, idstr, (float) Math.toDegrees(sph.x), (float) Math.toDegrees(sph.y), id, hip, tycho, (byte) -1);
                    }
                    if (runFiltersAnd(s)) {
                        s.initialize();
                        result.add(s);
                    }
                }

            } catch (Exception e) {
                logger.error(e);
            }
        }

        logger.info(I18n.bundle.format("notif.catalog.init", result.size));
        return result;
    }


}
