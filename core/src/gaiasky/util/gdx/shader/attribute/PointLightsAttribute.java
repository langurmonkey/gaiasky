/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader.attribute;

import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.utils.Array;

public class PointLightsAttribute extends Attribute {
    public final static String Alias = "pointLights";
    public final static int Type = register(Alias);

    public final Array<PointLight> lights;

    public PointLightsAttribute() {
        super(Type);
        lights = new Array<PointLight>(1);
    }

    public PointLightsAttribute(final PointLightsAttribute copyFrom) {
        this();
        lights.addAll(copyFrom.lights);
    }

    @Override
    public PointLightsAttribute copy() {
        return new PointLightsAttribute(this);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        for (PointLight light : lights)
            result = 1231 * result + (light == null ? 0 : light.hashCode());
        return result;
    }

    @Override
    public int compareTo(Attribute o) {
        if (index != o.index)
            return index < o.index ? -1 : 1;
        return 0; // FIXME implement comparing
    }
}
