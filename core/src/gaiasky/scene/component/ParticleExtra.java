/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;

/**
 * Component that holds extra information for particles, which are not
 * present in the {@link gaiasky.scene.record.Particle} record.
 * <p>
 * This component includes information such as the radius, effective temperature,
 * and primitive render scale for the particle. This information is often
 * used during rendering or for information display in the GUI.
 */
public class ParticleExtra implements Component, ICopy {

    public double computedSize;
    /** Radius of the object. **/
    public double radius;
    /** Effective temperature of the star, in Kelvin. **/
    public double tEff = -1;
    public double primitiveRenderScale;

    public void setTEff(Double tEff) {
        this.tEff = tEff;
    }
    public void setteff(Double tEff) {
        setTEff(tEff);
    }
    public void setPrimitiveRenderScale(Double primitiveRenderScale) {
        this.primitiveRenderScale = primitiveRenderScale;
    }

    @Override
    public Component getCopy(Engine engine) {
        var copy = engine.createComponent(this.getClass());
        copy.computedSize = computedSize;
        copy.tEff = tEff;
        copy.radius = radius;
        copy.primitiveRenderScale = primitiveRenderScale;
        return copy;
    }
}
