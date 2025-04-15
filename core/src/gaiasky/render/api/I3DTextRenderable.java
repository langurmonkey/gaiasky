/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.api;

import gaiasky.render.RenderingContext;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.system.render.draw.TextRenderer;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.Vector3d;

public interface I3DTextRenderable extends IRenderable {

    /**
     * Tells whether the text must be rendered or not for this entity
     *
     * @return True if text must be rendered
     */
    boolean renderText();

    /**
     * Renders the text
     *
     * @param batch  The sprite batch
     * @param shader The shader
     * @param sys    The text renderer
     * @param rc     The render context
     * @param camera The camera
     */
    void render(ExtSpriteBatch batch, ExtShaderProgram shader, TextRenderer sys, RenderingContext rc, ICamera camera);

    /**
     * Returns an array with the text colour in the fashion [r, g, b, a]
     *
     * @return Array with the colour
     */
    float[] textColour();

    /**
     * Returns the text size
     *
     * @return The text size
     */
    float textSize();

    /**
     * Returns the text scale for the scale varying in the shader
     *
     * @return The scale
     */
    float textScale();

    /**
     * Sets the position of this text in the out vector
     *
     * @param out The out parameter with the result
     */
    void textPosition(ICamera cam, Vector3d out);

    /**
     * Returns the text
     *
     * @return The text
     */
    String text();

    /**
     * Executes the blending for the text
     */
    void textDepthBuffer();

    /**
     * Is it a label or another kind of text?
     *
     * @return Whether this is a label
     */
    boolean isLabel();

    /**
     * Gets the text opacity
     *
     * @return Text opacity
     */
    float getTextOpacity();

}
