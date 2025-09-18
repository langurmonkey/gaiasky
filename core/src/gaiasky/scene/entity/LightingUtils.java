/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.entity;

import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Body;
import gaiasky.scene.component.GraphNode;
import gaiasky.scene.component.Model;
import gaiasky.util.Constants;
import gaiasky.util.TLV3;
import gaiasky.util.camera.Proximity;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector3Q;
import net.jafama.FastMath;

/**
 * This class updates the lights for each body according to their separation.
 */
public class LightingUtils {

    /** Distance at which the light reaches maximum intensity. **/
    private static final double LIGHT_X0 = 0.1 * Constants.AU_TO_U;
    /** Distance at which the light is nil. **/
    private static final double LIGHT_X1 = 500.0 * Constants.AU_TO_U;

    private static final TLV3 F31 = new TLV3();

    public static void updateLights(Model model, Body body, ICamera camera) {
        if (model.model != null && !model.model.isStaticLight() && body.distToCamera <= LIGHT_X1) {
            // We use point lights for stars.
            for (int i = 0; i < Constants.N_POINT_LIGHTS; i++) {
                var lightSource = camera.getCloseLightSource(i);
                if (lightSource != null) {
                    if (lightSource instanceof Proximity.NearbyRecord nr) {
                        var pointLight = model.model.pointLight(i);
                        if (pointLight != null) {
                            if (nr.isStar() || nr.isStarGroup()) {
                                // Only stars illuminate.
                                var color = nr.getColor();
                                var closestDistance = nr.getClosestDistToCamera();
                                // Dim light with distance.
                                var colorFactor = (float) FastMath.pow(MathUtilsDouble.flint(closestDistance, LIGHT_X0, LIGHT_X1, 1.0, 0.0), 2.0);
                                pointLight.position.set(nr.pos.put(F31.get()));
                                pointLight.color.set(color[0] * colorFactor, color[1] * colorFactor, color[2] * colorFactor, colorFactor);
                                pointLight.intensity = 1;
                            } else {
                                Vector3Q campos = camera.getPos();
                                pointLight.position.set(campos.x.floatValue(), campos.y.floatValue(), campos.z.floatValue());
                                pointLight.color.set(0f, 0f, 0f, 0f);
                                pointLight.intensity = 0;
                            }
                        }
                    }
                } else {
                    // Disable light.
                    var pointLight = model.model.pointLight(i);
                    if (pointLight != null) {
                        pointLight.color.set(0f, 0f, 0f, 0f);
                        pointLight.intensity = 0f;
                    }
                }
            }
        }
    }
}
