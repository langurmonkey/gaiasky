/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.camera.rec;

import gaiasky.util.math.Vector3D;

import java.time.Instant;

/**
 * Represents a single camera keyframe.
 */
public class Keyframe {
    // Keyframe name.
    public String name;
    // Camera state.
    public Vector3D pos, dir, up;
    // Point of interest. May be null (camera not in focus mode).
    public Vector3D target;
    // Keyframe start time.
    public Instant time;
    // Keyframe duration.
    public double seconds;
    /**
     * Is it a seam? (breaks splines)
     **/
    public boolean seam;

    public Keyframe(String name,
                    Vector3D pos,
                    Vector3D dir,
                    Vector3D up,
                    Vector3D target,
                    Instant time,
                    double secs,
                    boolean seam) {
        this.name = name;
        this.pos = pos;
        this.dir = dir;
        this.up = up;
        this.target = target;
        this.time = time;
        this.seconds = secs;
        this.seam = seam;
    }

    public Keyframe(String name,
                    Vector3D pos,
                    Vector3D dir,
                    Vector3D up,
                    Instant time,
                    double secs,
                    boolean seam) {
        this(name, pos, dir, up, null, time, secs, seam);
    }
}
