/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.filters;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import gaiasky.render.postprocess.effects.CubmeapProjectionEffect.CubemapProjection;
import gaiasky.render.util.ShaderLoader;

public final class CubemapProjectionsFilter extends Filter<CubemapProjectionsFilter> {

    private final ShaderProgram[] programs;
    private final Vector2 viewport;
    private float planetariumAperture, planetariumAngle, celestialSphereIndexOfRefraction;
    private CubemapProjection projection;
    private TextureData[] cubemapSides;
    private int cmId = Integer.MIN_VALUE;
    public CubemapProjectionsFilter(float w, float h) {
        super(null);
        this.viewport = new Vector2(w, h);

        ShaderProgram equirectangular = ShaderLoader.fromFile("screenspace", "cubemapprojections", "#define equirectangular");
        ShaderProgram cylindrical = ShaderLoader.fromFile("screenspace", "cubemapprojections", "#define cylindrical");
        ShaderProgram hammeraitoff = ShaderLoader.fromFile("screenspace", "cubemapprojections", "#define hammer");
        ShaderProgram azimuthal = ShaderLoader.fromFile("screenspace", "cubemapprojections", "#define azimuthal");
        ShaderProgram orthographic = ShaderLoader.fromFile("screenspace", "cubemapprojections", "#define orthographic");
        ShaderProgram orthosphere = ShaderLoader.fromFile("screenspace", "cubemapprojections", "#define orthosphere");
        ShaderProgram orthosphere_crosseye = ShaderLoader.fromFile("screenspace", "cubemapprojections", "#define orthosphere_crosseye");

        programs = new ShaderProgram[7];
        programs[0] = equirectangular;
        programs[1] = cylindrical;
        programs[2] = hammeraitoff;
        programs[3] = orthographic;
        programs[4] = orthosphere;
        programs[5] = orthosphere_crosseye;
        programs[6] = azimuthal;

        super.program = equirectangular;
        rebind();

    }

    public CubemapProjection getProjection() {
        return this.projection;
    }

    /**
     * Sets the projection to use
     *
     * @param proj Cubemap projection
     */
    public void setProjection(CubemapProjection proj) {
        this.projection = proj;
        switch (proj) {
        case EQUIRECTANGULAR -> {
            super.program = programs[0];
            rebind();
        }
        case CYLINDRICAL -> {
            super.program = programs[1];
            rebind();
        }
        case HAMMER -> {
            super.program = programs[2];
            rebind();
        }
        case ORTHOGRAPHIC -> {
            super.program = programs[3];
            rebind();
        }
        case ORTHOSPHERE -> {
            super.program = programs[4];
            rebind();
        }
        case ORTHOSPHERE_CROSSEYE -> {
            super.program = programs[5];
            rebind();
        }
        case AZIMUTHAL_EQUIDISTANT -> {
            super.program = programs[6];
            rebind();
        }
        default -> {
        }
        }
    }

    public void setSides(FrameBuffer xpositive, FrameBuffer xnegative, FrameBuffer ypositive, FrameBuffer ynegative, FrameBuffer zpositive, FrameBuffer znegative) {
        cubemapSides = new TextureData[6];
        cubemapSides[0] = xpositive.getColorBufferTexture().getTextureData();
        cubemapSides[1] = xnegative.getColorBufferTexture().getTextureData();
        cubemapSides[2] = ypositive.getColorBufferTexture().getTextureData();
        cubemapSides[3] = ynegative.getColorBufferTexture().getTextureData();
        cubemapSides[4] = zpositive.getColorBufferTexture().getTextureData();
        cubemapSides[5] = znegative.getColorBufferTexture().getTextureData();

        FrameBuffer[] fbos = new FrameBuffer[6];
        fbos[0] = xpositive;
        fbos[1] = xnegative;
        fbos[2] = ypositive;
        fbos[3] = ynegative;
        fbos[4] = zpositive;
        fbos[5] = znegative;

        if (cmId < 0)
            cmId = Gdx.gl.glGenTexture();

        // Make active
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0 + u_texture1);
        Gdx.gl.glBindTexture(GL20.GL_TEXTURE_CUBE_MAP, cmId);

        // Call glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i) for all sides
        for (int i = 0; i < cubemapSides.length; i++) {
            if (cubemapSides[i].getType() == TextureData.TextureDataType.Custom) {
                cubemapSides[i].consumeCustomData(GL20.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i);
            }
        }

        for (int i = 0; i < cubemapSides.length; i++) {
            fbos[i].begin();
            Gdx.gl.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL20.GL_COLOR_ATTACHMENT0, GL20.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, cmId, 0);
            fbos[i].end();
        }

        Gdx.gl.glTexParameteri(GL20.GL_TEXTURE_CUBE_MAP, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_LINEAR);
        Gdx.gl.glTexParameteri(GL20.GL_TEXTURE_CUBE_MAP, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_LINEAR);
        Gdx.gl.glTexParameteri(GL20.GL_TEXTURE_CUBE_MAP, GL20.GL_TEXTURE_WRAP_S, GL20.GL_CLAMP_TO_EDGE);
        Gdx.gl.glTexParameteri(GL20.GL_TEXTURE_CUBE_MAP, GL20.GL_TEXTURE_WRAP_T, GL20.GL_CLAMP_TO_EDGE);
        Gdx.gl.glBindTexture(GL20.GL_TEXTURE_CUBE_MAP, 0);

        setParam(Param.Cubemap, u_texture1);

    }

    public void setViewportSize(float width, float height) {
        this.viewport.set(width, height);
        setParam(Param.Viewport, this.viewport);
    }

    public float getPlanetariumAperture() {
        return this.planetariumAperture;
    }

    public void setPlanetariumAperture(float ap) {
        this.planetariumAperture = ap;
        setParam(Param.PlanetariumAperture, ap);
    }

    public float getPlanetariumAngle() {
        return this.planetariumAngle;
    }

    public void setPlanetariumAngle(float angle) {
        this.planetariumAngle = angle;
        setParam(Param.PlanetariumAngle, angle);
    }

    public float getCelestialSphereIndexOfRefraction() {
        return this.celestialSphereIndexOfRefraction;
    }

    public void setCelestialSphereIndexOfRefraction(float ior) {
        this.celestialSphereIndexOfRefraction = ior;
        setParam(Param.CelestialSphereIndexOfRefraction, ior);
    }

    @Override
    public void rebind() {
        // reimplement super to batch every parameter
        setParams(Param.Cubemap, u_texture1);
        setParams(Param.Viewport, viewport);
        setParams(Param.PlanetariumAperture, planetariumAperture);
        setParams(Param.PlanetariumAngle, planetariumAngle);
        setParams(Param.CelestialSphereIndexOfRefraction, celestialSphereIndexOfRefraction);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        // Bind cubemap
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0 + u_texture1);
        Gdx.gl.glBindTexture(GL20.GL_TEXTURE_CUBE_MAP, cmId);
    }

    public enum Param implements Parameter {
        // @formatter:off
        Cubemap("u_cubemap", 0),
        Viewport("u_viewport", 2),
        PlanetariumAperture("u_planetariumAperture", 0),
        PlanetariumAngle("u_planetariumAngle", 0),
        CelestialSphereIndexOfRefraction("u_celestialSphereIndexOfRefraction", 1);
        // @formatter:on

        private final String mnemonic;
        private final int elementSize;

        Param(String mnemonic, int arrayElementSize) {
            this.mnemonic = mnemonic;
            this.elementSize = arrayElementSize;
        }

        @Override
        public String mnemonic() {
            return this.mnemonic;
        }

        @Override
        public int arrayElementSize() {
            return this.elementSize;
        }
    }
}
