/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.scenegraph.component;

import com.badlogic.gdx.math.Matrix4;

/**
 * Represents a generic matrix transformation
 * @author tsagrista
 *
 */
public interface ITransform {
    public void apply(Matrix4 mat);
}
