/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import com.badlogic.gdx.files.FileHandle;
import gaiasky.scenegraph.ParticleGroup.ParticleBean;
import gaiasky.util.Constants;
import gaiasky.util.GlobalConf;
import gaiasky.util.I18n;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.Vector3d;
import gaiasky.util.parse.Parser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class UncertaintiesProvider implements IParticleGroupDataProvider {
    private static final Log logger = Logger.getLogger(UncertaintiesProvider.class);
    
    @Override
    public List<ParticleBean> loadData(String file) {
        return loadData(file, 1);
    }

    @Override
    public List<ParticleBean> loadData(String file, double factor) {

        FileHandle f = GlobalConf.data.dataFileHandle(file);
        @SuppressWarnings("unchecked")
        List<ParticleBean> pointData = loadData(f.read(), factor);

        if (pointData != null)
            logger.info(I18n.bundle.format("notif.nodeloader", pointData.size(), file));

        return pointData;
    }

    @Override
    public List<ParticleBean> loadData(InputStream is, double factor) {
        List<ParticleBean> pointData = new ArrayList<>();
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
    public List<ParticleBean> loadDataMapped(String file, double factor) {
        // TODO Auto-generated method stub
        return null;
    }
}
