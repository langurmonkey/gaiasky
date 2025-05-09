/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.filter.attrib;

import gaiasky.scene.api.IParticleRecord;
import gaiasky.util.math.Vector3D;

public sealed interface IAttribute permits AttributeAbsmag, AttributeAppmag, AttributeColorBlue, AttributeColorGreen, AttributeColorRed, AttributeDEC,
        AttributeDistance, AttributeEclLatitude, AttributeEclLongitude, AttributeGalLatitude, AttributeGalLongitude, AttributeMualpha, AttributeMudelta,
        AttributeRA, AttributeRadvel, AttributeUCD {
    Vector3D aux1 = new Vector3D();
    Vector3D aux2 = new Vector3D();

    /**
     * Gets the value of this attribute.
     *
     * @param bean The particle or star bean.
     *
     * @return The value.
     */
    Object get(IParticleRecord bean);

    /**
     * Gets the number value of this attribute.
     *
     * @param bean The particle or star bean.
     *
     * @return The number value.
     */
    double getNumber(IParticleRecord bean);

    /**
     * Gets the unit in string.
     *
     * @return The unit of this attribute.
     */
    String getUnit();

    /**
     * Gets the name.
     *
     * @return The name.
     */
    String toString();

    /**
     * Check whether the attribute contains a numeric value.
     * @return True if the attribute has a numeric value.
     */
    boolean isNumberAttribute();

}
