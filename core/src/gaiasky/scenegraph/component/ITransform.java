/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph.component;

import com.badlogic.gdx.math.Matrix4;

/**
 * Represents a generic matrix transformation
 * @author tsagrista
 *
 */
public interface ITransform {
    void apply(Matrix4 mat);
}
