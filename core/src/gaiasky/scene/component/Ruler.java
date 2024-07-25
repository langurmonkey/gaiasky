/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.util.math.Vector3d;

public class Ruler implements Component {
    public final double[] pos0 = new double[3];
    public final double[] pos1 = new double[3];
    public final Vector3d p0 = new Vector3d();
    public final Vector3d p1 = new Vector3d();
    public final Vector3d m = new Vector3d();
    public String name0, name1;
    public boolean rulerOk = false;
    public String dist;

    public String getName0() {
        return name0;
    }

    public void setName0(String name0) {
        this.name0 = name0;
    }

    public String getName1() {
        return name1;
    }

    public void setName1(String name1) {
        this.name1 = name1;
    }

    public boolean rulerOk() {
        return rulerOk;
    }

    /**
     * Returns true if the ruler is attached to at least one object.
     *
     * @return True if the ruler is attached.
     */
    public boolean hasAttached() {
        return name0 != null || name1 != null;
    }

    public boolean hasObject0() {
        return name0 != null && !name0.isEmpty();
    }

    public boolean hasObject1() {
        return name1 != null && !name1.isEmpty();
    }
}
