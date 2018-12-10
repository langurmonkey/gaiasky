package gaia.cu9.ari.gaiaorbit.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

import gaia.cu9.ari.gaiaorbit.render.system.FontRenderSystem;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

/**
 * Interface to be implemented by all entities that can render a text in 3d
 * space
 * 
 * @author Toni Sagrista
 *
 */
public interface I3DTextRenderable extends IRenderable {

    /**
     * Tells whether the text must be rendered or not for this entity
     * 
     * @return True if text must be rendered
     */
    public boolean renderText();

    /**
     * Renders the text
     * 
     * @param batch
     *            The sprite batch
     * @param shader
     *            The shader
     * @param sys
     *            The font render system
     * @param rc
     *            The render context
     * @param camera
     *            The camera
     */
    public void render(SpriteBatch batch, ShaderProgram shader, FontRenderSystem sys, RenderingContext rc, ICamera camera);

    /**
     * Returns an array with the text colour in the fashion [r, g, b, a]
     * 
     * @return Array with the colour
     */
    public float[] textColour();

    /**
     * Returns the text size
     * 
     * @return The text size
     */
    public float textSize();

    /**
     * Returns the text scale for the scale varying in the shader
     * 
     * @return The scale
     */
    public float textScale();

    /**
     * Sets the position of this text in the out vector
     * 
     * @param out
     *            The out parameter with the result
     */
    public void textPosition(ICamera cam, Vector3d out);

    /**
     * Returns the text
     * 
     * @return The text
     */
    public String text();

    /**
     * Executes the blending for the text
     */
    public void textDepthBuffer();

    /**
     * Is it a label or another kind of text?
     * 
     * @return Whether this is a label
     */
    public boolean isLabel();

    /**
     * Gets the text opacity
     * @return Text opacity
     */
    public float getTextOpacity();

}
