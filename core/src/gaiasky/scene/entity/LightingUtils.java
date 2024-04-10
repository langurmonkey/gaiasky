/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.entity;

import com.badlogic.gdx.math.Vector3;
import gaiasky.scene.api.IFocus;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Body;
import gaiasky.scene.component.GraphNode;
import gaiasky.scene.component.Model;
import gaiasky.util.Constants;
import gaiasky.util.TLV3;
import gaiasky.util.camera.Proximity;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector3b;

public class LightingUtils {

    // At what distance the light has the maximum intensity.
    private static final double LIGHT_X0 = 0.1 * Constants.AU_TO_U;
    // At what distance the light is 0.
    private static final double LIGHT_X1 = 5e4 * Constants.AU_TO_U;

    private static final ThreadLocal<Vector3> F31 = new TLV3();

    public static void updateLights(Model model, Body body, GraphNode graph, ICamera camera) {
        if (model.model != null && !model.model.isStaticLight() && body.distToCamera <= LIGHT_X1) {
            // We use point lights for stars.
            for (int i = 0; i < Constants.N_POINT_LIGHTS; i++) {
                IFocus lightSource = camera.getCloseLightSource(i);
                if (lightSource != null) {
                    if (lightSource instanceof Proximity.NearbyRecord) {
                        var pointLight = model.model.pointLight(i);
                        if (pointLight != null) {
                            Proximity.NearbyRecord nr = (Proximity.NearbyRecord) lightSource;
                            if (nr.isStar() || nr.isStarGroup()) {
                                // Only stars illuminate.
                                float[] col = nr.getColor();
                                double closestDist = nr.getClosestDistToCamera();
                                // Dim light with distance.
                                float colFactor = (float) Math.pow(MathUtilsDouble.flint(closestDist, LIGHT_X0, LIGHT_X1, 1.0, 0.0), 2.0);
                                pointLight.position.set(nr.pos.put(F31.get()));
                                pointLight.color.set(col[0] * colFactor, col[1] * colFactor, col[2] * colFactor, colFactor);
                                pointLight.intensity = 1;
                            } else {
                                Vector3b campos = camera.getPos();
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
