/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render;

import com.badlogic.gdx.utils.Bits;

public enum RenderGroup {
    /**
     * None
     **/
    NONE(-100),
    /**
     * A skybox rendered with a cubemap
     */
    SKYBOX(0),
    /**
     * Using default shader, no normal map
     **/
    MODEL_BG(100),
    /**
     * Grids shader
     **/
    MODEL_VERT_GRID(200),
    /**
     * Single pixel
     **/
    POINT_STAR(300),
    /**
     * Annotations
     **/
    FONT_ANNOTATION(400),
    /**
     * Opaque meshes (dust, etc.)
     **/
    MODEL_PIX_DUST(500),
    /**
     * Per-pixel lighting (early in the rendering pipeline)
     **/
    MODEL_PIX_EARLY(600),
    /**
     * Per-vertex lighting (early in the rendering pipeline)
     **/
    MODEL_VERT_EARLY(700),
    /**
     * Group of billboard datasets
     **/
    BILLBOARD_GROUP(800),
    /**
     * Star billboards
     **/
    BILLBOARD_STAR(900),
    /**
     * Particle group
     **/
    PARTICLE_GROUP(1000),
    /**
     * Particle group (extended)
     **/
    PARTICLE_GROUP_EXT(1100),
    /**
     * Particle group (extended, model)
     **/
    PARTICLE_GROUP_EXT_SPHERE(1200),
    /**
     * Star group
     **/
    STAR_GROUP(1300),
    /**
     * Variable star group
     **/
    VARIABLE_GROUP(1400),
    /**
     * A particle defined by orbital elements
     **/
    ORBITAL_ELEMENTS_PARTICLE(1500),
    /**
     * A particle group defined by orbital elements
     **/
    ORBITAL_ELEMENTS_GROUP(1600),
    /**
     * Models with only diffuse lighting
     **/
    MODEL_DIFFUSE(1700),
    /**
     * Using normal shader for per-pixel lighting.
     **/
    MODEL_PIX(1800),
    /**
     * Tessellated model
     **/
    MODEL_PIX_TESS(1900),
    /**
     * Beams
     **/
    MODEL_VERT_BEAM(2000),
    /**
     * Model star
     **/
    MODEL_VERT_STAR(2100),
    /**
     * Label
     **/
    FONT_LABEL(2200),
    /**
     * Regular billboard sprite
     **/
    BILLBOARD_SPRITE(2300),
    /**
     * IntShader - galaxies
     **/
    BILLBOARD_GAL(2400),
    /**
     * Recursive grid
     */
    MODEL_VERT_RECGRID(2500),
    /**
     * Point
     **/
    POINT(3000),
    /**
     * Point GPU
     **/
    POINT_GPU(3100),
    /**
     * Line
     **/
    LINE(4000),
    /**
     * Line GPU
     **/
    LINE_GPU(4100),
    /**
     * IntShader - front (planets, satellites...)
     **/
    BILLBOARD_SSO(5000),
    /**
     * Atmospheres of planets
     **/
    MODEL_ATM(6000),
    /**
     * Clouds
     **/
    MODEL_CLOUD(6500),
    /**
     * Using normal shader for per-pixel lighting, rendered late for items with transparency.
     **/
    MODEL_PIX_TRANSPARENT(7000),
    /**
     * Line late
     **/
    LINE_LATE(8000),
    /**
     * Shapes
     **/
    SHAPE(9000),
    /**
     * Particle effects
     */
    PARTICLE_EFFECTS(10000),
    /**
     * Transparent additive-blended meshes
     **/
    MODEL_VERT_ADDITIVE(11000);

    public final int priority;

    RenderGroup(int priority) {
       this.priority = priority;
    }

    /**
     * Adds the given render groups to the given Bits mask
     *
     * @param renderGroupMask The bit mask
     * @param rgs             The render groups
     *
     * @return The bits instance
     */
    public static Bits add(Bits renderGroupMask, RenderGroup... rgs) {
        for (RenderGroup rg : rgs) {
            renderGroupMask.set(rg.ordinal());
        }
        return renderGroupMask;
    }

    /**
     * Sets the given Bits mask to the given render groups
     *
     * @param renderGroupMask The bit mask
     * @param rgs             The render groups
     *
     * @return The bits instance
     */
    public static Bits set(Bits renderGroupMask, RenderGroup... rgs) {
        renderGroupMask.clear();
        return add(renderGroupMask, rgs);
    }

    public boolean is(Bits renderGroupMask) {
        return renderGroupMask.get(ordinal());
    }

}
