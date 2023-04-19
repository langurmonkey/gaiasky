/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.camera.rec;

import gaiasky.util.math.Vector3d;

public class Keyframe {
    public String name;
    public Vector3d pos, dir, up;
    public long time;
    public double seconds;
    /**
     * Is it a seam? (breaks splines)
     **/
    public boolean seam;

    public Keyframe(String name, Vector3d pos, Vector3d dir, Vector3d up, long time, double secs, boolean seam) {
        this.name = name;
        this.pos = pos;
        this.dir = dir;
        this.up = up;
        this.time = time;
        this.seconds = secs;
        this.seam = seam;
    }
}
