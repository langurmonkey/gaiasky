package gaiasky.render;

import com.badlogic.gdx.utils.Bits;

/**
 * Describes to which render group this node belongs at a particular time
 * step
 */
public enum RenderGroup {
    /**
     * Using normal shader for per-pixel lighting.
     **/
    MODEL_PIX,
    /**
     * Using normal shader for per-pixel lighting, rendered late for items with transparency.
     **/
    MODEL_PIX_TRANSPARENT,
    /**
     * Using default shader, no normal map
     **/
    MODEL_BG,
    /**
     * IntShader - stars
     **/
    BILLBOARD_STAR,
    /**
     * IntShader - galaxies
     **/
    BILLBOARD_GAL,
    /**
     * IntShader - front (planets, satellites...)
     **/
    BILLBOARD_SSO,
    /**
     * Billboard with custom texture
     **/
    BILLBOARD_TEX,
    /**
     * Single pixel
     **/
    POINT_STAR,
    /**
     * Line
     **/
    LINE,
    /**
     * Line late
     **/
    LINE_LATE,
    /**
     * Annotations
     **/
    FONT_ANNOTATION,
    /**
     * Atmospheres of planets
     **/
    MODEL_ATM,
    /**
     * Label
     **/
    FONT_LABEL,
    /**
     * Model star
     **/
    MODEL_VERT_STAR,
    /**
     * Group of billboard datasets
     **/
    BILLBOARD_GROUP,
    /**
     * Model close up
     **/
    MODEL_CLOSEUP,
    /**
     * Beams
     **/
    MODEL_VERT_BEAM,
    /**
     * Particle group
     **/
    PARTICLE_GROUP,
    /**
     * Star group
     **/
    STAR_GROUP,
    /**
     * Shapes
     **/
    SHAPE,
    /**
     * Regular billboard sprite
     **/
    BILLBOARD_SPRITE,
    /**
     * Line GPU
     **/
    LINE_GPU,
    /**
     * A particle defined by orbital elements
     **/
    ORBITAL_ELEMENTS_PARTICLE,
    /**
     * A particle group defined by orbital elements
     **/
    ORBITAL_ELEMENTS_GROUP,
    /**
     * Transparent additive-blended meshes
     **/
    MODEL_VERT_ADDITIVE,
    /**
     * Grids shader
     **/
    MODEL_VERT_GRID,
    /**
     * Clouds
     **/
    MODEL_CLOUD,
    /**
     * Point
     **/
    POINT,
    /**
     * Point GPU
     **/
    POINT_GPU,
    /**
     * Opaque meshes (dust, etc.)
     **/
    MODEL_PIX_DUST,
    /**
     * Tessellated model
     **/
    MODEL_PIX_TESS,
    /**
     * Only diffuse
     **/
    MODEL_DIFFUSE,
    /**
     * Recursive grid
     */
    MODEL_VERT_RECGRID,
    /**
     * Thrusters
     */
    MODEL_VERT_THRUSTER,
    /**
     * Variable star group
     **/
    VARIABLE_GROUP,
    /**
     * Per-pixel lighting (early in the rendering pipeline)
     **/
    MODEL_PIX_EARLY,
    /**
     * Per-vertex lighting (early in the rendering pipeline)
     **/
    MODEL_VERT_EARLY,
    /**
     * A skybox rendered with a cubemap
     */
    SKYBOX,
    /**
     * None
     **/
    NONE;

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
