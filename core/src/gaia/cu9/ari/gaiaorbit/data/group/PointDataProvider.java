/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.data.group;

import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.scenegraph.ParticleGroup.ParticleBean;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

/**
 * This provider loads point data into particle beans
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
        InputStream is = GlobalConf.data.dataFileHandle(file).read();

        if(file.endsWith(".gz")){
            try {
                is = new GZIPInputStream(GlobalConf.data.dataFileHandle(file).read());
            }catch(IOException e){
                logger.error("File ends with '.gz' (" + file +") but is not a Gzipped file!", e);
            }
        }

        @SuppressWarnings("unchecked")
        Array<ParticleBean> pointData = (Array<ParticleBean>) loadData(is, factor);

        if (pointData != null)
            logger.info(I18n.bundle.format("notif.nodeloader", pointData.size, file));

        return pointData;
    }

    @Override
    public Array<? extends ParticleBean> loadData(InputStream is, double factor) {
        Array<ParticleBean> pointData = new Array<>();
        try {
            int tokenslen;
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty() && !line.startsWith("#")) {
                    try {
                        // Read line
                        String[] tokens = line.split("\\s+");
                        tokenslen = tokens.length;
                        double[] point = new double[tokenslen];
                        for (int j = 0; j < tokenslen; j++) {
                            // We use regular parser because of scientific notation
                            point[j] = Double.parseDouble(tokens[j]) * factor;
                        }
                        pointData.add(new ParticleBean(point));
                    }catch(NumberFormatException e){
                        // Skip line
                    }
                }
            }

            br.close();


        } catch (Exception e) {
            logger.error(e);
            return null;
        }finally{
            try {
                is.close();
            }catch(Exception e){
                // Nothing
            }
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
