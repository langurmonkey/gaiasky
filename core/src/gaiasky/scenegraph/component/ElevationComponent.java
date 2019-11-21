/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph.component;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Vector2;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Pair;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.noise.NoiseUtils;
import gaiasky.util.noise.OpenSimplexNoise;

/**
 * Contains the parameters and functions for procedural elevation
 */
public class ElevationComponent {
    private static Log logger = Logger.getLogger(ElevationComponent.class);

    public enum NoiseType {
        PERLIN,
        OPENSIMPLEX
    }

    private NoiseType noiseType;
    private Float noiseSize = 10f;
    /* Octave frequencies and amplitudes */
    private double[][] noiseOctaves;
    private double noisePower = 1d;

    private OpenSimplexNoise osn;

    public ElevationComponent() {
        super();
    }

    private void initNoise() {
        switch (noiseType) {
        case OPENSIMPLEX:
            if (osn == null)
                osn = new OpenSimplexNoise(12345l);
            break;
        }
    }


    public Pair<float[][], Pixmap> generateElevation(int N, int M, float heightScale) {
        // Construct RAM height map from noise algorithms
        logger.info("Generating procedural " + N + "x" + M + " elevation data");
        initNoise();
        initOctaves();
        double[] freqs = noiseOctaves[0];
        double[] amps = noiseOctaves[1];
        Pixmap pixmap = new Pixmap(N, M, Pixmap.Format.RGBA8888);
        float[][] partialData = new float[N][M];
        float wsize = 0f / (float) N;
        float hsize = 0f / (float) M;
        Vector2 size = new Vector2(noiseSize * 2f, noiseSize);
        Vector2 coord = new Vector2();
        for (int i = 0; i < N; i++) {
            float u = (i + wsize / 2f) / (float) N;
            for (int j = 0; j < M; j++) {
                float v = (j + hsize / 2f) / (float) M;

                coord.set(u * size.x, v * size.y);
                double frequency = 6.0d;
                double n = 0.0f;

                for (int o = 0; o < freqs.length; o++) {
                    float f = (float) (frequency * freqs[o]);
                    if (noiseType.equals(ElevationComponent.NoiseType.OPENSIMPLEX)) {
                        // Open simplex noise
                        n += amps[o] * Math.abs(osn.eval(coord.x * f, coord.y * f) * 1.25);
                    } else if (noiseType.equals(ElevationComponent.NoiseType.PERLIN)) {
                        // Perlin noise
                        n += amps[o] * Math.abs(NoiseUtils.psnoise(new Vector2(coord.x * f, coord.y * f), new Vector2(size.x * f, size.y * f)));
                    }
                }

                n = MathUtilsd.clamp(1d - Math.pow(1d - MathUtilsd.clamp(n, 0d, 1d), noisePower), 0d, 1d);
                float nf = (float) n;

                partialData[i][j] = (1.0f - nf) * heightScale;

                // Pixamp
                pixmap.drawPixel(i, j, Color.rgba8888(nf, nf, nf, 1f));
            }
        }
        return new Pair<>(partialData, pixmap);
    }

    /**
     * Initialize the octave frequencies and amplitudes with the default values if needed
     */
    public void initOctaves() {
        if (noiseOctaves == null) {
            // Frequencies, amplitudes
            noiseOctaves = new double[][] { { 1d, 4d, 8d }, { 1d, 0.25d, 0.125d } };
        }
    }

    public void setNoiseType(String noiseType) {
        try {
            this.noiseType = NoiseType.valueOf(noiseType.toUpperCase());
        } catch (Exception e) {
            this.noiseType = NoiseType.OPENSIMPLEX;
        }
    }

    /**
     * Only if height is {@link MaterialComponent#GEN_HEIGHT_KEYWORD}
     *
     * @param noiseSize Size of the sampling area
     */
    public void setNoiseSize(Double noiseSize) {
        this.noiseSize = noiseSize.floatValue();
    }

    /**
     * Sets the noiseOctaves as a matrix of [frequency,amplitude]
     *
     * @param octaves The noiseOctaves
     */
    public void setNoiseOctaves(double[][] octaves) {
        this.noiseOctaves = octaves;
    }

    public void setNoisePower(Double power) {
        this.noisePower = power;
    }
}
