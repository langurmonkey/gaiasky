/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import gaiasky.data.api.IParticleGroupDataProvider;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.record.PointParticleRecord;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3d;
import gaiasky.util.math.Vector3dTransformer;
import gaiasky.util.parse.Parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Generic data provider for point clouds that reads cartesian positions (XYZ) from a text file.
 * Consider using {@link BinaryPointDataProvider} instead, which includes many more features and is more compact.
 */
public class PointDataProvider implements IParticleGroupDataProvider {
    private static final Log logger = Logger.getLogger(PointDataProvider.class);

    private Matrix4d transform;
    private Vector3dTransformer transformer;
    private final Vector3d aux = new Vector3d();

    public List<IParticleRecord> loadData(String file) {
        return loadData(file, 1d);
    }

    public List<IParticleRecord> loadData(String file,
                                          double factor) {
        InputStream is = Settings.settings.data.dataFileHandle(file).read();

        if (file.endsWith(".gz")) {
            try {
                is = new GZIPInputStream(Settings.settings.data.dataFileHandle(file).read());
            } catch (IOException e) {
                logger.error("File ends with '.gz' (" + file + ") but is not a Gzipped file!", e);
            }
        }

        List<IParticleRecord> pointData = loadData(is, factor);

        if (pointData != null)
            logger.info(I18n.msg("notif.nodeloader", pointData.size(), file));

        return pointData;
    }

    @Override
    public List<IParticleRecord> loadData(InputStream is,
                                          double factor) {
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
                            point[j] = Parser.parseDouble(tokens[j]) * factor;
                        }
                        if (transform != null) {
                            aux.set(point);
                            aux.mul(transform);
                            point[0] = aux.x;
                            point[1] = aux.y;
                            point[2] = aux.z;
                        }
                        if (transformer != null) {
                            aux.set(point);
                            transformer.transform(aux);
                            point[0] = aux.x;
                            point[1] = aux.y;
                            point[2] = aux.z;
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
    public void setTransformMatrix(Matrix4d matrix) {
        this.transform = matrix;
    }

    public void setVector3dTransformer(Vector3dTransformer transformer) {
        this.transformer = transformer;
    }

    @Override
    public List<IParticleRecord> loadDataMapped(String file,
                                                double factor) {
        return null;
    }
}
