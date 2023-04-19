/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.shader;

import com.badlogic.gdx.graphics.g3d.environment.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import gaiasky.util.gdx.shader.attribute.Attributes;
import gaiasky.util.gdx.shader.attribute.DirectionalLightsAttribute;
import gaiasky.util.gdx.shader.attribute.PointLightsAttribute;
import gaiasky.util.gdx.shader.attribute.SpotLightsAttribute;

public class Environment extends Attributes {

    /** Shadow map used to render shadows */
    public ShadowMap shadowMap;

    public Environment() {
        super();
    }

    public Environment add(final BaseLight... lights) {
        for (final BaseLight light : lights)
            add(light);
        return this;
    }

    public Environment add(final Array<BaseLight> lights) {
        for (final BaseLight light : lights)
            add(light);
        return this;
    }

    public Environment add(BaseLight light) {
        if (light instanceof DirectionalLight)
            add((DirectionalLight) light);
        else if (light instanceof PointLight) {
            add((PointLight) light);
        } else if (light instanceof SpotLight)
            add((SpotLight) light);
        else
            throw new GdxRuntimeException("Unknown light type");
        return this;
    }

    public Environment add(DirectionalLight light) {
        DirectionalLightsAttribute dirLights = ((DirectionalLightsAttribute) get(DirectionalLightsAttribute.Type));
        if (dirLights == null)
            set(dirLights = new DirectionalLightsAttribute());
        dirLights.lights.add(light);
        return this;
    }

    public Environment add(PointLight light) {
        PointLightsAttribute pointLights = ((PointLightsAttribute) get(PointLightsAttribute.Type));
        if (pointLights == null)
            set(pointLights = new PointLightsAttribute());
        pointLights.lights.add(light);
        return this;
    }

    public Environment add(SpotLight light) {
        SpotLightsAttribute spotLights = ((SpotLightsAttribute) get(SpotLightsAttribute.Type));
        if (spotLights == null)
            set(spotLights = new SpotLightsAttribute());
        spotLights.lights.add(light);
        return this;
    }

    public Environment remove(final BaseLight... lights) {
        for (final BaseLight light : lights)
            remove(light);
        return this;
    }

    public Environment remove(final Array<BaseLight> lights) {
        for (final BaseLight light : lights)
            remove(light);
        return this;
    }

    public Environment remove(BaseLight light) {
        if (light instanceof DirectionalLight)
            remove((DirectionalLight) light);
        else if (light instanceof PointLight)
            remove((PointLight) light);
        else if (light instanceof SpotLight)
            remove((SpotLight) light);
        else
            throw new GdxRuntimeException("Unknown light type");
        return this;
    }

    public Environment remove(DirectionalLight light) {
        if (has(DirectionalLightsAttribute.Type)) {
            DirectionalLightsAttribute dirLights = ((DirectionalLightsAttribute) get(DirectionalLightsAttribute.Type));
            dirLights.lights.removeValue(light, false);
            if (dirLights.lights.size == 0)
                remove(DirectionalLightsAttribute.Type);
        }
        return this;
    }

    public Environment remove(PointLight light) {
        if (has(PointLightsAttribute.Type)) {
            PointLightsAttribute pointLights = ((PointLightsAttribute) get(PointLightsAttribute.Type));
            pointLights.lights.removeValue(light, false);
            if (pointLights.lights.size == 0)
                remove(PointLightsAttribute.Type);
        }
        return this;
    }

    public Environment remove(SpotLight light) {
        if (has(SpotLightsAttribute.Type)) {
            SpotLightsAttribute spotLights = ((SpotLightsAttribute) get(SpotLightsAttribute.Type));
            spotLights.lights.removeValue(light, false);
            if (spotLights.lights.size == 0)
                remove(SpotLightsAttribute.Type);
        }
        return this;
    }
}
