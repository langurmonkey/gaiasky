/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import gaiasky.scenegraph.ParticleGroup.ParticleRecord;
import gaiasky.util.Constants;
import gaiasky.util.GlobalConf;
import gaiasky.util.I18n;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.parse.Parser;
import gaiasky.util.units.Position;
import gaiasky.util.units.Position.PositionType;

import java.io.*;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads SDSS data from a text file with a series of [ra, dec, z]
 */
public class SDSSDataProvider implements IParticleGroupDataProvider {
    private static final Log logger = Logger.getLogger(SDSSDataProvider.class);

    public List<ParticleRecord> loadData(String file) {
        return loadData(file, 1d);
    }

    public List<ParticleRecord> loadData(String file, double factor) {
        List<ParticleRecord> pointData = loadDataMapped(GlobalConf.data.dataFile(file), factor);
        if (pointData != null)
            logger.info(I18n.bundle.format("notif.nodeloader", pointData.size(), file));

        return pointData;
    }

    @Override
    public List<ParticleRecord> loadData(InputStream is, double factor) {
        List<ParticleRecord> pointData = new ArrayList<>();

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            loadFromBufferedReader(br, pointData);
            br.close();

        } catch (Exception e) {
            logger.error(e);
            return null;
        }

        return pointData;
    }

    private void loadFromBufferedReader(BufferedReader br, List<ParticleRecord> pointData) throws IOException {
        String line;
        int tokenslen;
        while ((line = br.readLine()) != null) {
            if (!line.isEmpty() && !line.startsWith("#")) {
                // Read line
                String[] tokens = line.split(",");
                tokenslen = tokens.length;
                double[] point = new double[tokenslen];
                double ra = Parser.parseDouble(tokens[0]);
                double dec = Parser.parseDouble(tokens[1]);
                double z = Parser.parseDouble(tokens[2]);
                if (z >= 0) {
                    // Dist in MPC
                    // double dist = redshiftToDistance(0.272, 0.0000812,
                    // 0.728, 70.4, z);
                    double dist = ((z * 299792.46) / 71);
                    if (dist > 16) {
                        // Convert position
                        Position p = new Position(ra, "deg", dec, "deg", dist, "mpc", PositionType.EQ_SPH_DIST);
                        p.gsposition.scl(Constants.PC_TO_U);
                        point[0] = p.gsposition.x;
                        point[1] = p.gsposition.y;
                        point[2] = p.gsposition.z;
                        pointData.add(new ParticleRecord(point));
                    }
                }
            }
        }
    }

    public void setFileNumberCap(int cap) {
    }

    @Override
    public List<ParticleRecord> loadDataMapped(String file, double factor) {
        List<ParticleRecord> pointData = new ArrayList<>();

        try {
            FileChannel fc = new RandomAccessFile(file, "r").getChannel();
            MappedByteBuffer mem = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());

            if (mem != null) {
                CharBuffer charBuffer = StandardCharsets.UTF_8.decode(mem);
                BufferedReader br = new BufferedReader(new StringReader(charBuffer.toString()));
                loadFromBufferedReader(br, pointData);
                br.close();
            }
            fc.close();
        } catch (Exception e) {
            logger.error(e);
            return null;
        }
        return pointData;
    }
}
