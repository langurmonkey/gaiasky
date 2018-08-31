package gaia.cu9.ari.gaiaorbit.data.group;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.scenegraph.ParticleGroup.ParticleBean;
import gaia.cu9.ari.gaiaorbit.scenegraph.StarGroup.StarBean;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.Pair;
import gaia.cu9.ari.gaiaorbit.util.color.ColourUtils;
import gaia.cu9.ari.gaiaorbit.util.coord.AstroUtils;
import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.parse.Parser;

public class TGASDataProvider extends AbstractStarGroupDataProvider {
    private static Log logger = Logger.getLogger(TGASDataProvider.class);
    private static final boolean dumpToDisk = false;
    /** Colors BT, VT for all Tycho2 stars file **/
    private static final String btvtColorsFile = "data/tgas_final/bt-vt-tycho.csv";
    /** Gaia sourceid to radial velocities file **/
    private static final String raveTgasFile = "data/tgas_final/rave_rv.csv";

    public TGASDataProvider() {
        super();
        countsPerMag = new long[22];
    }

    public Array<StarBean> loadData(String file) {
        return loadData(file, 1d);
    }

    public Array<StarBean> loadData(String file, double factor) {
        logger.info(I18n.bundle.format("notif.datafile", file));

        FileHandle f = Gdx.files.internal(file);

        initLists(f);
        loadData(f.read(), factor);

        if (list != null)
            logger.info(I18n.bundle.format("notif.nodeloader", list.size, file));

        return list;
    }

    @Override
    public Array<? extends ParticleBean> loadData(InputStream is, double factor) {
        Pair<Map<String, Float>, Map<String, Integer>> extra = loadTYCBVHIP(btvtColorsFile);
        Map<Long, Double> radialVelocities = loadRadialVelocities(raveTgasFile);

        int raveStars = 0;
        try {

            initLists();

            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            // Skip first line
            br.readLine();
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty() && !line.startsWith("#")) {
                    // Read line
                    String[] tokens = line.split(",");
                    double[] point = new double[StarBean.SIZE];

                    double pllx = Parser.parseDouble(tokens[3]);
                    double pllxerr = Parser.parseDouble(tokens[4]);

                    double appmag = Parser.parseDouble(tokens[7]);

                    // Keep only stars with relevant parallaxes
                    if (acceptParallax(appmag, pllx, pllxerr)) {
                        /** ID **/
                        long sourceid = Parser.parseLong(tokens[0]);

                        /** DISTANCE **/
                        double distpc = (1000d / pllx);
                        double geodistpc = getGeoDistance(sourceid);
                        distpc = geodistpc > 0 ? geodistpc : distpc;

                        if (acceptDistance(distpc)) {

                            double dist = distpc * Constants.PC_TO_U;

                            /** INDEX **/
                            int tyc1 = -1, tyc2 = -1, tyc3 = -1;
                            String tyc = "";
                            if (tokens.length > 9) {
                                tyc = tokens[9].replace("\"", "");
                                String[] tycgroups = tyc.split("-");
                                tyc1 = !tyc.isEmpty() ? Integer.parseInt(tycgroups[0]) : -1;
                                tyc2 = !tyc.isEmpty() ? Integer.parseInt(tycgroups[1]) : -1;
                                tyc3 = !tyc.isEmpty() ? Integer.parseInt(tycgroups[2]) : -1;
                            }

                            int hip = Parser.parseInt(tokens[8]);
                            if (hip <= 0 && extra.getSecond().containsKey(tyc)) {
                                hip = extra.getSecond().get(tyc);
                            }

                            /** NAME **/
                            String name;
                            if (tyc1 > 0) {
                                name = "TYC " + tyc;
                            } else if (hip > 0) {
                                name = "HIP " + hip;
                            } else {
                                name = String.valueOf((long) sourceid);
                            }

                            /** RA and DEC **/
                            double ra = Parser.parseDouble(tokens[1]);
                            double dec = Parser.parseDouble(tokens[2]);
                            Vector3d pos = Coordinates.sphericalToCartesian(Math.toRadians(ra), Math.toRadians(dec), dist, new Vector3d());

                            /** PROPER MOTIONS in mas/yr **/
                            double mualphastar = Parser.parseDouble(tokens[5]);
                            double mudelta = Parser.parseDouble(tokens[6]);
                            //double mualpha = mualphastar / Math.cos(Math.toRadians(dec));

                            /** RADIAL VELOCITY in km/s **/
                            double radvel = 0;
                            if (radialVelocities != null && radialVelocities.containsKey(sourceid)) {
                                radvel = radialVelocities.get(sourceid);
                                raveStars++;
                            }

                            /**
                             * PROPER MOTION VECTOR = (pos+dx) - pos - [units/yr]
                             **/
                            Vector3d pm = AstroUtils.properMotionsToCartesian(mualphastar, mudelta, radvel, Math.toRadians(ra), Math.toRadians(dec), distpc);

                            double absmag = (appmag - 2.5 * Math.log10(Math.pow(distpc / 10d, 2d)));
                            double flux = Math.pow(10, -absmag / 2.5f);
                            double size = Math.min((Math.pow(flux, 0.5f) * Constants.PC_TO_U * 0.16f), 1e9f) / 1.5;

                            /** COLOR, we use the tycBV map if present **/
                            double colorbv = 0;
                            if (extra.getFirst() != null) {
                                if (extra.getFirst().containsKey(tyc)) {
                                    colorbv = extra.getFirst().get(tyc);
                                }
                            }

                            float[] rgb = ColourUtils.BVtoRGB(colorbv);
                            double col = Color.toFloatBits(rgb[0], rgb[1], rgb[2], 1.0f);
                            colors.put(sourceid, rgb);
                            sphericalPositions.put(sourceid, new double[] { ra, dec, dist });

                            point[StarBean.I_HIP] = hip;
                            point[StarBean.I_TYC1] = tyc1;
                            point[StarBean.I_TYC2] = tyc2;
                            point[StarBean.I_TYC3] = tyc3;
                            point[StarBean.I_X] = pos.x;
                            point[StarBean.I_Y] = pos.y;
                            point[StarBean.I_Z] = pos.z;
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

                            list.add(new StarBean(point, sourceid, name));

                            int appclmp = (int) MathUtilsd.clamp(appmag, 0, 21);
                            countsPerMag[(int) appclmp] += 1;
                        }
                    }
                }
            }

            br.close();

            if (dumpToDisk) {
                dumpToDisk(list, "/tmp/tgas.bin", "bin");
            }

        } catch (Exception e) {
            logger.error(e);
            list = null;
        }

        if (radialVelocities != null)
            logger.info("Found " + raveStars + " with RAVE radial velocities in TGAS");

        return list;
    }

    /**
     * Loads the radial velocities file (RAVE)
     * 
     * @param file
     *            The file location
     * @return The data map
     */
    private Map<Long, Double> loadRadialVelocities(String file) {
        Map<Long, Double> result = new HashMap<Long, Double>(220000);

        FileHandle f = Gdx.files.internal(file);

        if (!f.exists())
            return null;

        InputStream data = f.read();
        BufferedReader br = new BufferedReader(new InputStreamReader(data));
        try {
            // skip first line with headers
            br.readLine();

            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",");
                Long sourceid = Parser.parseLong(tokens[0]);
                Double radvel = Parser.parseDouble(tokens[1]);
                if (!result.containsKey(sourceid)) {
                    result.put(sourceid, radvel);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                logger.error(e);
            }

        }

        return result;
    }

    private Pair<Map<String, Float>, Map<String, Integer>> loadTYCBVHIP(String file) {
        final String comma = ",";
        final String comment = "#";

        Map<String, Float> colors = new HashMap<String, Float>();
        Map<String, Integer> hips = new HashMap<String, Integer>();
        FileHandle f = Gdx.files.internal(file);
        InputStream data = f.read();
        BufferedReader br = new BufferedReader(new InputStreamReader(data));
        try {
            // skip first line with headers
            br.readLine();
            int i = 1;
            String line;
            while ((line = br.readLine()) != null) {

                if (!line.startsWith(comment))
                    // Add B-V colour
                    addInfo(line, colors, hips, comma);
                logger.debug("Line " + i++);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                logger.error(e);
            }

        }

        return new Pair<Map<String, Float>, Map<String, Integer>>(colors, hips);
    }

    private void addInfo(String line, Map<String, Float> colors, Map<String, Integer> hips, String split) {
        String[] st = line.split(split);
        int tyc1 = Parser.parseInt(st[1].trim());
        int tyc2 = Parser.parseInt(st[2].trim());
        int tyc3 = Parser.parseInt(st[3].trim());

        int hip = -1;
        try {
            hip = Parser.parseInt(st[4]);
        } catch (Exception e) {
        }

        float BV = 0;
        if (st.length >= 7) {
            float bt = Parser.parseFloat(st[5].trim());
            float vt = Parser.parseFloat(st[6].trim());
            BV = 0.850f * (bt - vt);
        }
        String id = tyc1 + "-" + tyc2 + "-" + tyc3;
        colors.put(id, BV);
        if (hip > 0)
            hips.put(id, hip);

    }

    @Override
    public Array<? extends ParticleBean> loadDataMapped(String file, double factor) {
        // TODO Auto-generated method stub
        return null;
    }

}
