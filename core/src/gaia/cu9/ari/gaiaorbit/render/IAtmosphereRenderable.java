package gaia.cu9.ari.gaiaorbit.render;

import com.badlogic.gdx.graphics.g3d.ModelBatch;

import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

/**
 * To be implemented by all entities wanting to render an atmosphere.
 *
 * @author Toni Sagrista
 */
public interface IAtmosphereRenderable extends IRenderable {

    /**
     * Renders the atmosphere.
     *
     * @param modelBatch The model batch to use.
     * @param alpha      The opacity.
     * @param t          The time in seconds since the start.
     * @param vroffset   Positional offset in vr mode, if any.
     */
    void renderAtmosphere(ModelBatch modelBatch, float alpha, double t, Vector3d vroffset);
}
