/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import com.badlogic.gdx.math.Matrix4;
import gaiasky.util.math.Matrix4d;

/**
 * Represents a generic matrix transformation
 */
public interface ITransform {
    void apply(Matrix4 mat);

    void apply(Matrix4d mat);
}
