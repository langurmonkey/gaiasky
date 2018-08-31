package gaia.cu9.ari.gaiaorbit.data.stars;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.data.ISceneGraphLoader;
import gaia.cu9.ari.gaiaorbit.scenegraph.AbstractPositionEntity;
import gaia.cu9.ari.gaiaorbit.scenegraph.Star;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.Nature;
import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.parse.Parser;

/**
 * Loads the HYG catalog in CSV format
 * 
 * @author Toni Sagrista
 *
 */
public class HYGCSVLoader extends AbstractCatalogLoader implements ISceneGraphLoader {
    private static final Log logger = Logger.getLogger(HYGCSVLoader.class);
    
    private static final String separator = "\t";
    private static final String pmseparator = ",";

    public String pmFile;

    @Override
    public Array<AbstractPositionEntity> loadData() throws FileNotFoundException {
        // Proper motions

        Map<Integer, float[]> pmMap = null;

        if (pmFile != null) {
            pmMap = new HashMap<Integer, float[]>();
            FileHandle pmf = Gdx.files.internal(pmFile);
            InputStream pmdata = pmf.read();
            BufferedReader pmbr = new BufferedReader(new InputStreamReader(pmdata));
            try {
                String line;
                while ((line = pmbr.readLine()) != null) {
                    if (!line.startsWith("#")) {
                        String[] tokens = line.split(pmseparator);
                        Integer hip = Parser.parseInt(tokens[0]);
                        float mualpha = Parser.parseFloat(tokens[1]);
                        float mudelta = Parser.parseFloat(tokens[2]);
                        float radvel = 0f;
                        pmMap.put(hip, new float[] { mualpha, mudelta, radvel });
                    }
                }
            } catch (Exception e) {
                logger.error(e);
            } finally {
                try {
                    pmbr.close();
                } catch (IOException e) {
                    logger.error(e);
                }

            }
        }

        Array<AbstractPositionEntity> stars = new Array<AbstractPositionEntity>();
        for (String file : files) {
            FileHandle f = Gdx.files.internal(file);
            InputStream data = f.read();
            BufferedReader br = new BufferedReader(new InputStreamReader(data));

            try {
                //Skip first line
                String[] header = br.readLine().split(separator);

                for (String head : header) {
                    head = head.trim();
                }
                String line;
                while ((line = br.readLine()) != null) {
                    //Add star
                    addStar(line, stars, pmMap);
                }
            } catch (IOException e) {
                logger.error(e);
            } finally {
                try {
                    br.close();
                } catch (IOException e) {
                    logger.error(e);
                }

            }
        }

        logger.info(I18n.bundle.format("notif.catalog.init", stars.size));
        return stars;
    }

    private void addStar(String line, Array<AbstractPositionEntity> stars, Map<Integer, float[]> pmMap) {
        String[] st = line.split(separator);

        long starid = -1l;
        int hip = Parser.parseInt(st[1].trim());
        if (hip == 0) {
            hip = -1;
        }

        double ra = MathUtilsd.lint(Parser.parseDouble(st[7].trim()), 0, 24, 0, 360);
        double dec = Parser.parseDouble(st[8].trim());
        double dist = Parser.parseDouble(st[9]) * Constants.PC_TO_U;
        Vector3d pos = Coordinates.sphericalToCartesian(Math.toRadians(ra), Math.toRadians(dec), dist, new Vector3d());

        // Proper motion
        Vector3 pmfloat = new Vector3(0f, 0f, 0f);
        Vector3 pmSph = new Vector3(0f, 0f, 0f);
        if (pmMap != null && hip > 0) {
            if (pmMap.containsKey(hip)) {
                float[] pmf = pmMap.get(hip);
                double mualpha = pmf[0] * Nature.MILLARCSEC_TO_DEG;
                double mudelta = pmf[1] * Nature.MILLARCSEC_TO_DEG;
                double radvel = pmf[2] * Constants.KM_TO_U * Nature.S_TO_Y;

                // Proper motion vector = (pos+dx) - pos
                Vector3d pm = Coordinates.sphericalToCartesian(Math.toRadians(ra + mualpha), Math.toRadians(dec + mudelta), dist + radvel, new Vector3d());
                pm.sub(pos);
                pmSph.set(pmf);

                pmfloat = pm.toVector3();
            }
        }

        float appmag = Parser.parseFloat(st[10].trim());
        float colorbv = 0f;

        if (st.length >= 14 && !st[13].trim().isEmpty()) {
            colorbv = Parser.parseFloat(st[13].trim());
        } else {
            colorbv = 1;
        }

        if (appmag < GlobalConf.data.LIMIT_MAG_LOAD) {
            float absmag = Parser.parseFloat(st[11].trim());
            String name = null;
            if (!st[6].trim().isEmpty()) {
                name = st[6].trim().replaceAll("\\s+", " ");
            } else if (!st[5].trim().isEmpty()) {
                name = st[5].trim().replaceAll("\\s+", " ");
            } else if (!st[1].trim().isEmpty() && Parser.parseInt(st[1]) > 0) {
                name = "HIP " + st[1].trim();
            } else if (!st[2].trim().isEmpty() && Parser.parseInt(st[2]) > 0) {
                name = "HD " + st[2].trim();
            } else if (!st[4].trim().isEmpty()) {
                name = st[4].trim().replaceAll("\\s+", " ");
            }

            Star star = new Star(pos, pmfloat, pmSph, appmag, absmag, colorbv, name, (float) ra, (float) dec, starid, hip, null, (byte) 2);
            if (runFiltersAnd(star))
                stars.add(star);
        }
    }

    public void setPmFile(String pm) {
        pmFile = pm;
    }

}
