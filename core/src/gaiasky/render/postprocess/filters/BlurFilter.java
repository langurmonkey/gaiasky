/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.filters;

import gaiasky.render.postprocess.util.PingPongBuffer;
import net.jafama.FastMath;

import java.util.HashMap;
import java.util.Map;

public final class BlurFilter extends MultipassFilter {
    // fbo, textures
    private final float invWidth;
    private final float invHeight;

    // @formatter:on
    private final Map<Integer, Convolve2DFilter> convolve = new HashMap<>(Tap.values().length);
    // blur
    private BlurType type;
    private float amount;
    private int passes;

    public BlurFilter(int width, int height) {
        // precompute constants
        this.invWidth = 1f / (float) width;
        this.invHeight = 1f / (float) height;

        this.passes = 1;
        this.amount = 1f;

        // create filters
        for (Tap tap : Tap.values()) {
            convolve.put(tap.radius, new Convolve2DFilter(tap.radius));
        }

        setType(BlurType.Gaussian5x5);
    }

    public void dispose() {
        for (Convolve2DFilter c : convolve.values()) {
            c.dispose();
        }
    }

    public int getPasses() {
        return passes;
    }

    public void setPasses(int passes) {
        this.passes = passes;
    }

    public BlurType getType() {
        return type;
    }

    public void setType(BlurType type) {
        if (this.type != type) {
            this.type = type;
            computeBlurWeightings();
        }
    }

    // not all blur types support custom amounts at this time
    public float getAmount() {
        return amount;
    }

    // not all blur types support custom amounts at this time
    public void setAmount(float amount) {
        this.amount = amount;
        computeBlurWeightings();
    }

    @Override
    public void render(PingPongBuffer buffer) {
        Convolve2DFilter c = convolve.get(this.type.tap.radius);

        for (int i = 0; i < this.passes; i++) {
            c.render(buffer);
        }
    }

    private void computeBlurWeightings() {
        boolean hasdata = true;
        Convolve2DFilter c = convolve.get(this.type.tap.radius);

        float[] outWeights = c.weights;
        float[] outOffsetsH = c.offsetsHor;
        float[] outOffsetsV = c.offsetsVert;

        float dx = this.invWidth;
        float dy = this.invHeight;

        switch (this.type) {
            case Gaussian3x3:
            case Gaussian5x5:
            case Gaussian7x7:
                computeKernel(this.type.tap.radius, this.amount, outWeights);
                computeOffsets(this.type.tap.radius, this.invWidth, this.invHeight, outOffsetsH, outOffsetsV);
                break;

            case Gaussian3x3b:
                // weights and offsets are computed from a binomial distribution
                // and reduced to be used *only* with bilinearly-filtered texture lookups
                //
                // with radius = 1f

                // weights
                outWeights[0] = 0.352941f;
                outWeights[1] = 0.294118f;
                outWeights[2] = 0.352941f;

                // horizontal offsets
                outOffsetsH[0] = -1.33333f;
                outOffsetsH[1] = 0f;
                outOffsetsH[2] = 0f;
                outOffsetsH[3] = 0f;
                outOffsetsH[4] = 1.33333f;
                outOffsetsH[5] = 0f;

                // vertical offsets
                outOffsetsV[0] = 0f;
                outOffsetsV[1] = -1.33333f;
                outOffsetsV[2] = 0f;
                outOffsetsV[3] = 0f;
                outOffsetsV[4] = 0f;
                outOffsetsV[5] = 1.33333f;

                // scale offsets from binomial space to screen space
                for (int i = 0; i < c.length * 2; i++) {
                    outOffsetsH[i] *= dx;
                    outOffsetsV[i] *= dy;
                }

                break;

            case Gaussian5x5b:

                // weights and offsets are computed from a binomial distribution
                // and reduced to be used *only* with bilinearly-filtered texture lookups
                //
                // with radius = 2f

                // weights
                outWeights[0] = 0.0702703f;
                outWeights[1] = 0.316216f;
                outWeights[2] = 0.227027f;
                outWeights[3] = 0.316216f;
                outWeights[4] = 0.0702703f;

                // horizontal offsets
                outOffsetsH[0] = -3.23077f;
                outOffsetsH[1] = 0f;
                outOffsetsH[2] = -1.38462f;
                outOffsetsH[3] = 0f;
                outOffsetsH[4] = 0f;
                outOffsetsH[5] = 0f;
                outOffsetsH[6] = 1.38462f;
                outOffsetsH[7] = 0f;
                outOffsetsH[8] = 3.23077f;
                outOffsetsH[9] = 0f;

                // vertical offsets
                outOffsetsV[0] = 0f;
                outOffsetsV[1] = -3.23077f;
                outOffsetsV[2] = 0f;
                outOffsetsV[3] = -1.38462f;
                outOffsetsV[4] = 0f;
                outOffsetsV[5] = 0f;
                outOffsetsV[6] = 0f;
                outOffsetsV[7] = 1.38462f;
                outOffsetsV[8] = 0f;
                outOffsetsV[9] = 3.23077f;

                // scale offsets from binomial space to screen space
                for (int i = 0; i < c.length * 2; i++) {
                    outOffsetsH[i] *= dx;
                    outOffsetsV[i] *= dy;
                }

                break;
            default:
                hasdata = false;
                break;
        }

        if (hasdata) {
            c.upload();
        }
    }

    private void computeKernel(int blurRadius, float blurAmount, float[] outKernel) {
        float twoSigmaSquare = 2.0f * blurAmount * blurAmount;
        float sigmaRoot = (float) FastMath.sqrt(twoSigmaSquare * FastMath.PI);
        float total = 0.0f;
        float distance;
        int index;

        for (int i = -blurRadius; i <= blurRadius; ++i) {
            distance = i * i;
            index = i + blurRadius;
            outKernel[index] = (float) FastMath.exp(-distance / twoSigmaSquare) / sigmaRoot;
            total += outKernel[index];
        }

        int size = (blurRadius * 2) + 1;
        for (int i = 0; i < size; ++i) {
            outKernel[i] /= total;
        }
    }

    private void computeOffsets(int blurRadius, float dx, float dy, float[] outOffsetH, float[] outOffsetV) {

        final int X = 0, Y = 1;
        for (int i = -blurRadius, j = 0; i <= blurRadius; ++i, j += 2) {
            outOffsetH[j + X] = i * dx;
            outOffsetH[j + Y] = 0;

            outOffsetV[j + X] = 0;
            outOffsetV[j + Y] = i * dy;
        }
    }

    @Override
    public void rebind() {
        computeBlurWeightings();
    }

    // @formatter:off
    protected enum Tap {
        Tap3x3(1),
        Tap5x5(2),
        Tap7x7(3);

        public final int radius;

        Tap(int radius) {
            this.radius = radius;
        }
    }

    public enum BlurType {
        Gaussian3x3(Tap.Tap3x3),
        Gaussian3x3b(Tap.Tap3x3),
        Gaussian5x5(Tap.Tap5x5),
        Gaussian5x5b(Tap.Tap5x5),
        Gaussian7x7(Tap.Tap7x7);

        public final Tap tap;

        BlurType(Tap tap) {
            this.tap = tap;
        }
    }
}
