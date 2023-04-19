/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.util.Constants;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.Vector3d;

import java.util.ArrayList;
import java.util.List;

public class Boundaries implements Component {

    public List<List<Vector3d>> boundaries;

    public void setBoundaries(List<List<Vector3d>> boundaries) {
        this.boundaries = boundaries;
    }

    public void setBoundaries(double[][][] ids) {
        this.boundaries = new ArrayList<>(ids.length);
        for (double[][] dd : ids) {
            List<Vector3d> ii = new ArrayList<>(dd.length);
            for (double[] v : dd) {
                Vector3d vec = new Vector3d(v);
                ii.add(vec);
            }
            this.boundaries.add(ii);
        }
    }

    public void setBoundariesEquatorial(double[][][] ids) {
        this.boundaries = new ArrayList<>(ids.length);
        for (double[][] dd : ids) {
            List<Vector3d> ii = new ArrayList<>(dd.length);
            for (double[] v : dd) {
                // Convert equatorial coordinates to cartesian with a default radius.
                double raRadians = Math.toRadians(v[0]);
                double decRadians = Math.toRadians(v[1]);
                Vector3d vec = Coordinates.sphericalToCartesian(raRadians, decRadians, 10 * Constants.AU_TO_U, new Vector3d());
                ii.add(vec);
            }
            this.boundaries.add(ii);
        }
    }
}
