/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.utils.Array;
import gaiasky.util.math.Vector3d;
import gaiasky.util.tree.IPosition;
import net.jafama.FastMath;

public class Constel implements Component {
    public double deltaYears;

    public float alpha;
    public boolean allLoaded = false;
    public Vector3d posd;

    /** List of pairs of HIP identifiers **/
    public Array<int[]> ids;
    /**
     * The lines themselves as pairs of positions
     **/
    public IPosition[][] lines;

    public void setIds(double[][] ids) {
        this.ids = new Array<>(ids.length);
        for (double[] dd : ids) {
            int[] ii = new int[dd.length];
            for (int j = 0; j < dd.length; j++)
                ii[j] = (int) FastMath.round(dd[j]);
            this.ids.add(ii);
        }
    }
}
