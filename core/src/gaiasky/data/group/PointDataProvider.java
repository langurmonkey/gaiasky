/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.scenegraph.particle.PointParticleRecord;
import gaiasky.util.i18n.I18n;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * This provider loads point data into particle beans.
 */
public class PointDataProvider implements IParticleGroupDataProvider {
    private static final Log logger = Logger.getLogger(PointDataProvider.class);

    public List<IParticleRecord> loadData(String file) {
        return loadData(file, 1d);
    }

    public List<IParticleRecord> loadData(String file, double factor) {
        InputStream is = Settings.settings.data.dataFileHandle(file).read();

        if(file.endsWith(".gz")){
            try {
                is = new GZIPInputStream(Settings.settings.data.dataFileHandle(file).read());
            }catch(IOException e){
                logger.error("File ends with '.gz' (" + file +") but is not a Gzipped file!", e);
            }
        }

        List<IParticleRecord> pointData = loadData(is, factor);

        if (pointData != null)
            logger.info(I18n.txt("notif.nodeloader", pointData.size(), file));

        return pointData;
    }

    @Override
    public List<IParticleRecord> loadData(InputStream is, double factor) {
        List<IParticleRecord> pointData = new ArrayList<>();
        try (is) {
            int tokensLength;
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty() && !line.startsWith("#")) {
                    try {
                        // Read line
                        String[] tokens = line.split("\\s+");
                        tokensLength = tokens.length;
                        double[] point = new double[tokensLength];
                        for (int j = 0; j < tokensLength; j++) {
                            // We use regular parser because of scientific notation
                            point[j] = Double.parseDouble(tokens[j]) * factor;
                        }
                        pointData.add(new PointParticleRecord(point));
                    } catch (NumberFormatException e) {
                        // Skip line
                    }
                }
            }

            br.close();

        } catch (Exception e) {
            logger.error(e);
            return null;
        }
        // Nothing

        return pointData;
    }

    public void setFileNumberCap(int cap) {
    }

    @Override
    public void setStarNumberCap(int cap) {

    }

    @Override
    public void setProviderParams(Map<String, Object> params) {
    }

    @Override
    public List<IParticleRecord> loadDataMapped(String file, double factor) {
        // TODO Auto-generated method stub
        return null;
    }
}
