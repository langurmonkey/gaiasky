/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import gaiasky.GaiaSky;
import gaiasky.data.api.IParticleGroupDataProvider;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.record.ParticleVector;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Matrix4D;
import gaiasky.util.math.Vector3D;
import gaiasky.util.parse.Parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.zip.GZIPInputStream;

/**
 * Generic data provider for point clouds that reads cartesian positions (XYZ) from a text file.
 * Consider using {@link BinaryPointDataProvider} instead, which includes many more features and is more compact.
 */
public class PointDataProvider implements IParticleGroupDataProvider {
    private static final Log logger = Logger.getLogger(PointDataProvider.class);

    private Matrix4D transform;
    private final Vector3D aux = new Vector3D();

    public List<IParticleRecord> loadData(String file,
                                          Runnable preCallback,
                                          BiConsumer<Long, Long> updateCallback,
                                          Runnable postCallback) {
        return loadData(file, 1d, preCallback, updateCallback, postCallback);
    }

    public List<IParticleRecord> loadData(String file,
                                          double factor,
                                          Runnable preCallback,
                                          BiConsumer<Long, Long> updateCallback,
                                          Runnable postCallback) {
        InputStream is = GaiaSky.settings().data.dataFileHandle(file).read();

        if (file.endsWith(".gz")) {
            try {
                is = new GZIPInputStream(GaiaSky.settings().data.dataFileHandle(file).read());
            } catch (IOException e) {
                logger.error("File ends with '.gz' (" + file + ") but is not a Gzipped file!", e);
            }
        }

        List<IParticleRecord> pointData = loadData(is, factor, preCallback, updateCallback, postCallback);

        if (pointData != null)
            logger.info(I18n.msg("notif.nodeloader", pointData.size(), file));

        return pointData;
    }

    @Override
    public List<IParticleRecord> loadData(InputStream is,
                                          double factor,
                                          Runnable preCallback,
                                          BiConsumer<Long, Long> updateCallback,
                                          Runnable postCallback) {
        List<IParticleRecord> pointData = new ArrayList<>();

        if (preCallback != null)
            preCallback.run();

        try (is) {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            // Load all valid lines into memory.
            // This presupposes that the file is not too large.
            List<String> validLines = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isEmpty() && !line.startsWith("#")) {
                    validLines.add(line);
                }
            }

            long totalLines = validLines.size();

            // Process lines with progress updates
            for (long currentLine = 0; currentLine < validLines.size(); currentLine++) {
                try {
                    String[] tokens = validLines.get((int) currentLine).split("\\s+");
                    int tokensLength = tokens.length;
                    double[] point = new double[tokensLength];

                    for (int j = 0; j < tokensLength; j++) {
                        if (j < 3) {
                            point[j] = Parser.parseDouble(tokens[j]) * factor;
                        } else {
                            point[j] = Parser.parseDouble(tokens[j]);
                        }
                    }

                    if (transform != null) {
                        aux.set(point);
                        aux.mul(transform);
                        point[0] = aux.x;
                        point[1] = aux.y;
                        point[2] = aux.z;
                    }

                    pointData.add(new ParticleVector(point));

                    if (updateCallback != null) {
                        updateCallback.accept(currentLine + 1, totalLines);
                    }

                } catch (NumberFormatException e) {
                    // Skip line
                }
            }

        } catch (Exception e) {
            logger.error(e);
            return null;
        } finally {
            if (postCallback != null)
                postCallback.run();
        }

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
    public void setTransformMatrix(Matrix4D matrix) {
        this.transform = matrix;
    }

    @Override
    public List<IParticleRecord> loadDataMapped(String file,
                                                double factor,
                                                Runnable preCallback,
                                                BiConsumer<Long, Long> updateCallback,
                                                Runnable postCallback) {
        logger.warn("loadDataMapped(file, factor, pre, update, post): This method should not be used!");
        return null;
    }
}
