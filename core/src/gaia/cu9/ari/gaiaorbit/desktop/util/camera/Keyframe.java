package gaia.cu9.ari.gaiaorbit.desktop.util.camera;

import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

/**
 * Represents a keyframe and contains the camera state, the simulation time, the frame and a descriptor name.
 */
public class Keyframe {
    public String name;
    public Vector3d pos, dir, up;
    public long time;
    public double seconds;
    /**
     * Is it a seam? (breaks splines)
     **/
    public boolean seam = false;

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
