/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw;

import com.badlogic.gdx.graphics.Texture;
import gaiasky.GaiaSky;
import gaiasky.scene.component.Highlight;
import gaiasky.util.Constants;
import gaiasky.util.Settings;
import gaiasky.util.gdx.shader.ExtShaderProgram;

public class StarSetQuadComponent {

    protected float[] alphaSizeBr, opacityLimits, opacityLimitsHlShowAll;
    protected float starPointSize, brightnessPower;
    protected float minQuadSolidAngle;
    protected int fovMode;
    protected Texture starTex;

    public void setStarTexture(String starTexture) {
        starTex = new Texture(Settings.settings.data.dataFileHandle(starTexture), true);
        starTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    protected void initShaderProgram(ExtShaderProgram shaderProgram) {
        this.alphaSizeBr = new float[3];
        this.opacityLimits = new float[2];
        this.opacityLimitsHlShowAll = new float[]{0.95f, Settings.settings.scene.star.opacity[1]};

        updateMinQuadSolidAngle(Settings.settings.graphics.backBufferResolution);
        updateStarBrightness(Settings.settings.scene.star.brightness);
        updateBrightnessPower(Settings.settings.scene.star.power);
        updateStarPointSize(Settings.settings.scene.star.pointSize);
        updateStarOpacityLimits(Settings.settings.scene.star.opacity[0], Settings.settings.scene.star.opacity[1]);

        shaderProgram.begin();
        // Uniforms that rarely change
        shaderProgram.setUniformf("u_thAnglePoint", 1.0e-10f, 1.5e-8f);
        shaderProgram.setUniformf("u_solidAngleMap", 1.0e-10f, 1.3e-9f);
        starParameterUniforms(shaderProgram);
        shaderProgram.end();
    }

    private void updateSolidAngleMap(ExtShaderProgram shaderProgram) {
    }

    protected void starParameterUniforms(ExtShaderProgram shaderProgram) {
        shaderProgram.setUniformf("u_minQuadSolidAngle", minQuadSolidAngle);
        shaderProgram.setUniform3fv("u_alphaSizeBr", alphaSizeBr, 0, 3);
        shaderProgram.setUniformf("u_brightnessPower", brightnessPower);
        shaderProgram.setUniformf("u_pixelScale", minQuadSolidAngle);
    }

    protected void touchStarParameters(ExtShaderProgram shaderProgram) {
        GaiaSky.postRunnable(() -> {
            shaderProgram.begin();
            // Uniforms that rarely change
            starParameterUniforms(shaderProgram);
            shaderProgram.end();
        });
    }

    protected void updateMinQuadSolidAngle(int[] backBufferSize) {
        // Adjust to calibrated 2K resolution.
        minQuadSolidAngle = 1.0e-9f * (float) 1440 / (float) backBufferSize[1];
    }

    protected void updateStarBrightness(float br) {
        // Remap brightness to [0,2]
        alphaSizeBr[2] = (br - Constants.MIN_STAR_BRIGHTNESS) / (Constants.MAX_STAR_BRIGHTNESS - Constants.MIN_STAR_BRIGHTNESS) * 4f;
    }

    protected void updateBrightnessPower(float bp) {
        brightnessPower = bp;
    }

    protected void updateStarPointSize(float ps) {
        starPointSize = ps * 0.4f;
    }

    protected void updateStarOpacityLimits(float min, float max) {
        opacityLimits[0] = min;
        opacityLimits[1] = max;
    }

    protected void setOpacityLimitsUniform(ExtShaderProgram shaderProgram, Highlight highlight) {
        if (highlight != null && highlight.isHighlighted() && highlight.isHlAllVisible()) {
            opacityLimitsHlShowAll[0] = 0.95f;
            opacityLimitsHlShowAll[1] = Settings.settings.scene.star.opacity[1];
            shaderProgram.setUniform2fv("u_opacityLimits", opacityLimitsHlShowAll, 0, 2);
        } else {
            shaderProgram.setUniform2fv("u_opacityLimits", opacityLimits, 0, 2);
        }
    }
}
