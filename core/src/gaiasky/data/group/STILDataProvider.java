/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.LongMap;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.scenegraph.particle.ParticleRecord;
import gaiasky.util.*;
import gaiasky.util.Logger.Log;
import gaiasky.util.color.BVToTeff_ballesteros;
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
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.formats.AsciiTableBuilder;
import uk.ac.starlink.table.formats.CsvTableBuilder;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;

import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;

/**
 * Loads VOTables, FITS, etc. This data provider makes educated guesses using UCDs and column names to
 * match columns to attributes.
 * <p>
 * More information on this can be found <a href="https://gaia.ari.uni-heidelberg.de/gaiasky/docs/SAMP.html#stil-data-provider">here</a>.
 */
public class STILDataProvider extends AbstractStarGroupDataProvider {
    private static final Log logger = Logger.getLogger(STILDataProvider.class);
    private StarTableFactory factory;
    private long starId = 10000000;
    // Dataset options, may be null
    private DatasetOptions datasetOptions;

    // These names are not allowed
    private static final String[] forbiddenNameValues = { "-", "...", "nop", "nan", "?", "_", "x", "n/a" };

    public STILDataProvider() {
        super();
        // Logging level to WARN
        try {
            java.util.logging.Logger.getLogger("uk.ac.starlink").setLevel(Level.WARNING);
            java.util.logging.Logger.getLogger("org.astrogrid").setLevel(Level.WARNING);
            factory = new StarTableFactory();
            countsPerMag = new long[22];
            initLists();
        } catch (Exception e) {
            factory = null;
            logger.error(e);
        }
    }

    public void setDatasetOptions(DatasetOptions datasetOptions) {
        this.datasetOptions = datasetOptions;
    }

    @Override
    public List<IParticleRecord> loadData(String file, double factor) {
        logger.info(I18n.txt("notif.datafile", file));
        try {
            loadData(new FileDataSource(Settings.settings.data.dataFile(file)), factor);
        } catch (Exception e1) {
            try {
                logger.info("File " + file + " not found in data folder, trying relative path");
                loadData(new FileDataSource(file), factor);
            } catch (Exception e2) {
                logger.error(e1);
                logger.error(e2);
            }
        }
        logger.info(I18n.txt("notif.nodeloader", list.size(), file));
        return list;
    }

    /**
     * Gets the first ucd that can be translated to a double from the set.
     *
     * @param UCDs The array of UCDs. The UCDs which coincide with the names should be first.
     * @param row  The row objects
     * @return Pair of <UCD,Double>
     */
    private Pair<UCD, Double> getDoubleUcd(Array<UCD> UCDs, Object[] row) {
        for (UCD ucd : UCDs) {
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
     * @param UCDs The set of UCD objects
     * @param row  The row
     * @return A pair with the UCD and the string
     */
    private Pair<UCD, String> getStringUcd(Array<UCD> UCDs, Object[] row) {
        for (UCD ucd : UCDs) {
            try {
                String str = row[ucd.index].toString().strip();
                return new Pair<>(ucd, str);
            } catch (Exception e) {
                // not working, try next
            }
        }
        return null;
    }

    private Pair<UCD, String>[] getAllStringsUcd(Array<UCD> UCDs, Object[] row) {
        Array<Pair<UCD, String>> strings = new Array<>(false, 2);
        for (UCD ucd : UCDs) {
            try {
                String str = row[ucd.index].toString().strip();
                strings.add(new Pair<>(ucd, str));
            } catch (Exception e) {
                // not working, try next
            }
        }
        Pair<UCD, String>[] result = new Pair[strings.size];
        int i = 0;
        for (Pair<UCD, String> value : strings) {
            result[i++] = value;
        }
        return result;

    }

    public List<IParticleRecord> loadData(DataSource ds, double factor) {
        return loadData(ds, factor, null, null, null);
    }

    /**
     * @param ds
     * @param factor
     * @param preCallback    A function that runs before.
     * @param updateCallback A function that runs after each object has loaded. Gets two longs, the first holds the current number of loaded objects and the
     *                       second holds the total number of objects to load.
     * @param postCallback   A function that runs after the data has been loaded.
     * @return The list of particle records.
     */
    public List<IParticleRecord> loadData(DataSource ds, double factor, Runnable preCallback, RunnableLongLong updateCallback, Runnable postCallback) {
        try {
            if (factory != null) {

                // Add extra builders
                List<TableBuilder> builders = factory.getDefaultBuilders();
                builders.add(new CsvTableBuilder());
                builders.add(new AsciiTableBuilder());

                if (preCallback != null)
                    preCallback.run();

                // Try to load
                StarTable table = factory.makeStarTable(ds);

                long count = table.getRowCount();
                initLists((int) count);

                UCDParser ucdp = new UCDParser();
                ucdp.parse(table);

                if (ucdp.haspos) {
                    BVToTeff_ballesteros bvToTeff = new BVToTeff_ballesteros();

                    int nInvalidPllx = 0;
                    long i = 0l;
                    long step = Math.max(1l, Math.round(count / 100d));

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

                            Pair<UCD, Double> pos3 = getDoubleUcd(ucdp.POS3, row);
                            // Check missing pos3 -> Use default parallax
                            if (ucdp.POS3.isEmpty() || pos3 == null || pos3.getSecond() == null || !Double.isFinite(pos3.getSecond())) {
                                c = new Pair<>(null, 0.04);
                                unitc = "mas";
                                nInvalidPllx++;
                            } else {
                                c = getDoubleUcd(ucdp.POS3, row);
                                unitc = c.getFirst().unit;
                            }

                            PositionType pt = ucdp.getPositionType(a.getFirst(), b.getFirst(), c.getFirst());
                            // Check negative parallaxes -> Use default for consistency
                            if (pt.isParallax() && (c.getSecond() == null || c.getSecond().isNaN() || c.getSecond() <= 0)) {
                                c.setSecond(0.04);
                                unitc = "mas";
                                nInvalidPllx++;
                            }

                            Position p = new Position(a.getSecond(), a.getFirst().unit, b.getSecond(), b.getFirst().unit, c.getSecond(), unitc, pt);

                            double distpc = p.gsposition.len();
                            if ((pt.isParallax() && c.getSecond() <= 0) || !Double.isFinite(distpc) || distpc < 0) {
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
                                pm = AstroUtils.properMotionsToCartesian(mualphastar, mudelta, radvel, rarad, decrad, distpc, new Vector3d());
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
                            double magscl = (datasetOptions != null && datasetOptions.type == DatasetOptions.DatasetLoadType.STARS) ? datasetOptions.magnitudeScale : 0f;
                            appmag -= magscl;

                            //
                            double absmag = appmag - 5 * Math.log10((distpc <= 0 ? 10 : distpc)) + 5;
                            // Pseudo-luminosity. Usually L = L0 * 10^(-0.4*Mbol). We omit M0 and approximate Mbol = M
                            double pseudoL = Math.pow(10, -0.4 * absmag);
                            double sizeFactor = Nature.PC_TO_M * Constants.ORIGINAL_M_TO_U * 0.15;
                            float size = (float) Math.min((Math.pow(pseudoL, 0.45) * sizeFactor), 1e10);
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

                            /* TEFF */
                            float teff;
                            if (!ucdp.TEFF.isEmpty()) {
                                Pair<UCD, Double> teffPair = getDoubleUcd(ucdp.TEFF, row);
                                teff = teffPair.getSecond().floatValue();
                            } else {
                                // Convert B-V to T_eff using Ballesteros 2012
                                teff = (float) bvToTeff.bvToTeff(color);
                            }

                            // RGB
                            float[] rgb = ColorUtils.BVtoRGB(color);
                            //float[] rgb = ColorUtils.teffToRGB_harre(teff);

                            float col = Color.toFloatBits(rgb[0], rgb[1], rgb[2], 1.0f);

                            /* IDENTIFIER AND NAME */
                            String[] names;
                            Long id = -1l;
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
                                        id = ++starId;
                                    }
                                } else {
                                    // Emtpy ID
                                    id = ++starId;
                                    names = new String[] { id.toString() };
                                }
                            } else {
                                // We have name
                                Pair<UCD, String>[] namePairs = getAllStringsUcd(ucdp.NAME, row);
                                Array<String> namesArray = new Array<>(false, namePairs.length);
                                for (Pair<UCD, String> pair : namePairs) {
                                    String[] currNames = pair.getSecond().split(Constants.nameSeparatorRegex);
                                    for (String actualName : currNames) {
                                        if (actualName != null && !actualName.isEmpty() && !TextUtils.contains(forbiddenNameValues, actualName, true)) {
                                            namesArray.add(actualName);
                                        }
                                    }
                                }
                                names = new String[namesArray.size];
                                int k = 0;
                                for (String n : namesArray) {
                                    names[k++] = n;
                                }
                                if (names.length == 0) {
                                    names = new String[] { id.toString() };
                                }

                                // Take care of HIP stars
                                if (!ucdp.ID.isEmpty()) {
                                    Pair<UCD, String> idpair = getStringUcd(ucdp.ID, row);
                                    if (idpair.getFirst().colname.equalsIgnoreCase("hip")) {
                                        hip = Integer.valueOf(idpair.getSecond());
                                        id = (long) hip;
                                    } else {
                                        id = ++starId;
                                    }
                                } else {
                                    id = ++starId;
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

                            if (datasetOptions == null || datasetOptions.type == DatasetOptions.DatasetLoadType.STARS) {
                                double[] dataD = new double[ParticleRecord.STAR_SIZE_D];
                                float[] dataF = new float[ParticleRecord.STAR_SIZE_F];
                                dataD[ParticleRecord.I_X] = p.gsposition.x;
                                dataD[ParticleRecord.I_Y] = p.gsposition.y;
                                dataD[ParticleRecord.I_Z] = p.gsposition.z;

                                dataF[ParticleRecord.I_FPMX] = (float) pm.x;
                                dataF[ParticleRecord.I_FPMY] = (float) pm.y;
                                dataF[ParticleRecord.I_FPMZ] = (float) pm.z;
                                dataF[ParticleRecord.I_FMUALPHA] = (float) mualphastar;
                                dataF[ParticleRecord.I_FMUDELTA] = (float) mudelta;
                                dataF[ParticleRecord.I_FRADVEL] = (float) radvel;
                                dataF[ParticleRecord.I_FAPPMAG] = (float) appmag;
                                dataF[ParticleRecord.I_FABSMAG] = (float) absmag;
                                dataF[ParticleRecord.I_FCOL] = col;
                                dataF[ParticleRecord.I_FSIZE] = size;
                                dataF[ParticleRecord.I_FHIP] = hip;

                                // Extra
                                ObjectDoubleMap<UCD> extraAttributes = addExtraAttributes(ucdp, row);
                                if (ucdp.TEFF.isEmpty()) {
                                    UCD teffUCD = new UCD("phys.temperature.effective", "teff", "K", -1);
                                    extraAttributes = initExtraAttributes(extraAttributes);
                                    extraAttributes.put(teffUCD, teff);
                                } else {
                                    extraAttributes = initExtraAttributes(extraAttributes);
                                    extraAttributes.put(ucdp.TEFF.first(), teff);
                                }

                                IParticleRecord sb = new ParticleRecord(dataD, dataF, id, names, extraAttributes);
                                list.add(sb);

                                int appclmp = (int) MathUtilsd.clamp(appmag, 0, 21);
                                countsPerMag[appclmp] += 1;
                            } else if (datasetOptions.type == DatasetOptions.DatasetLoadType.PARTICLES) {
                                double[] point = new double[3];
                                point[ParticleRecord.I_X] = p.gsposition.x;
                                point[ParticleRecord.I_Y] = p.gsposition.y;
                                point[ParticleRecord.I_Z] = p.gsposition.z;

                                // Extra
                                ObjectDoubleMap<UCD> extraAttributes = addExtraAttributes(ucdp, row);

                                IParticleRecord pb = new ParticleRecord(point, null, null, names, extraAttributes);
                                list.add(pb);
                            }

                        } catch (Exception e) {
                            logger.debug(e);
                            logger.debug("Exception parsing row " + i + ": skipping");
                        } i++;
                        if (updateCallback != null && i % step == 0) {
                            updateCallback.run(i, count);
                        }
                    } if (nInvalidPllx > 0) {
                        logger.warn("Found " + nInvalidPllx + " rows with nonexistent or negative parallax. Using the default 0.04 mas for them.");
                    }
                } else {
                    logger.error("Table not loaded: Position not found");
                }
            }
        } catch (Exception e) {
            logger.error(e);
        } finally {
            if (postCallback != null)
                postCallback.run();
        }

        return list;
    }

    private ObjectDoubleMap<UCD> initExtraAttributes(ObjectDoubleMap<UCD> extra){
        if(extra == null)
            extra = new ObjectDoubleMap<>(5);
        return extra;
    }

    private ObjectDoubleMap<UCD> addExtraAttributes(UCDParser ucdp, Object[] row) {
        // Extra
        ObjectDoubleMap<UCD> extraAttributes = null;
        for (UCD extra : ucdp.extra) {
            Double val = Double.NaN;
            try {
                val = ((Number) row[extra.index]).doubleValue();
            } catch (Exception e) {
                Object o = row[extra.index];
                if (o instanceof Character) {
                    Character c = (Character) o;
                    val = (double) c.charValue();
                }
            }
            if (extraAttributes == null)
                extraAttributes = new ObjectDoubleMap<>((int) (ucdp.extra.size * 1.25f), 0.8f);
            extraAttributes.put(extra, val);
        }
        return extraAttributes;
    }

    @Override
    public List<IParticleRecord> loadData(InputStream is, double factor) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<IParticleRecord> loadDataMapped(String file, double factor) {
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
