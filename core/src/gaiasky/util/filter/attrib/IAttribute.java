/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.filter.attrib;

import gaiasky.scenegraph.ParticleGroup.ParticleBean;
import gaiasky.util.math.Vector3d;

/**
 * Represents an attribute
 */
public interface IAttribute<T extends ParticleBean> {
    Vector3d aux1 = new Vector3d();
    Vector3d aux2 = new Vector3d();
    /**
     * Gets the value of this attribute
     * @bean The particle or star bean
     * @return The value
     */
    double get(T bean);

    /**
     * Gets the unit in string
     * @return The unit of this attribute
     */
    String getUnit();

}
