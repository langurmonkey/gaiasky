package gaia.cu9.ari.gaiaorbit.data.group;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.scenegraph.ParticleGroup.ParticleBean;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * This provider loads point data in the internal reference system format from text
 * files with a series of [x, y, z]
 * 
 * @author tsagrista
 *
 */
public class PointDataProvider implements IParticleGroupDataProvider {
    private static final Log logger = Logger.getLogger(PointDataProvider.class);
    
    public Array<? extends ParticleBean> loadData(String file) {
        return loadData(file, 1d);
    }

    public Array<? extends ParticleBean> loadData(String file, double factor) {
        FileHandle f = GlobalConf.data.dataFileHandle(file);
        @SuppressWarnings("unchecked")
        Array<ParticleBean> pointData = (Array<ParticleBean>) loadData(f.read(), factor);

        if (pointData != null)
            logger.info(I18n.bundle.format("notif.nodeloader", pointData.size, file));

        return pointData;
    }

    @Override
    public Array<? extends ParticleBean> loadData(InputStream is, double factor) {
        Array<ParticleBean> pointData = new Array<ParticleBean>();
        try {
            int tokenslen;
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty() && !line.startsWith("#")) {
                    // Read line
                    String[] tokens = line.split("\\s+");
                    tokenslen = tokens.length;
                    double[] point = new double[tokenslen];
                    for (int j = 0; j < tokenslen; j++) {
                        // We use regular parser because of scientific notation
                        point[j] = Double.parseDouble(tokens[j]) * factor;
                    }
                    pointData.add(new ParticleBean(point));
                }
            }

            br.close();

        } catch (Exception e) {
            logger.error(e);
            return null;
        }

        return pointData;
    }

    public void setFileNumberCap(int cap) {
    }

    @Override
    public Array<? extends ParticleBean> loadDataMapped(String file, double factor) {
        // TODO Auto-generated method stub
        return null;
    }
}
