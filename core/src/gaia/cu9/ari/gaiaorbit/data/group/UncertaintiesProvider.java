package gaia.cu9.ari.gaiaorbit.data.group;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.scenegraph.ParticleGroup.ParticleBean;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.coord.Coordinates;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.parse.Parser;

public class UncertaintiesProvider implements IParticleGroupDataProvider {
    private static final Log logger = Logger.getLogger(UncertaintiesProvider.class);
    
    @Override
    public Array<ParticleBean> loadData(String file) {
        return loadData(file, 1);
    }

    @Override
    public Array<ParticleBean> loadData(String file, double factor) {

        FileHandle f = Gdx.files.internal(file);
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
            Vector3d pos = new Vector3d();
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty() && !line.startsWith("#")) {
                    // Read line
                    String[] tokens = line.split("\\s+");
                    tokenslen = tokens.length;
                    double[] point = new double[tokenslen];
                    for (int j = 0; j < tokenslen; j++) {
                        point[j] = Parser.parseDouble(tokens[j]) * factor;
                    }

                    pos.set(point[1], point[2], (point[0] + 8));
                    pos.mul(Coordinates.galToEq());
                    pos.scl(Constants.KPC_TO_U);

                    point[0] = pos.x;
                    point[1] = pos.y;
                    point[2] = pos.z;

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
