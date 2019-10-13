/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.utils.Array;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.RenderingContext;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.util.Constants;
import gaiasky.util.gdx.IntModelBatch;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;

/**
 * The sole purpose of this class is to act as an invisible focus.
 *
 * @author tsagrista
 */
public class Invisible extends CelestialBody {

    /**
     * Needed for reflection in {@link AbstractPositionEntity#getSimpleCopy()}
     **/
    @SuppressWarnings("unused")
    public Invisible() {
    }

    public Invisible(String name) {
        this(name, 500 * Constants.M_TO_U);
    }

    public Invisible(String name, double size) {
        super();
        this.name = name;
        this.parentName = "Universe";
        this.size = (float) size;
        this.ct = new ComponentTypes(ComponentType.Invisible);
    }

    @Override
    public void render(IntModelBatch modelBatch, float alpha, double t, RenderingContext rc) {
    }

    @Override
    public double THRESHOLD_NONE() {
        return 0;
    }

    @Override
    public double THRESHOLD_QUAD() {
        return 0;
    }

    @Override
    public double THRESHOLD_POINT() {
        return 0;
    }

    @Override
    public float getInnerRad() {
        return 0;
    }

    @Override
    protected float labelFactor() {
        return 0;
    }

    @Override
    protected float labelMax() {
        return 0;
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        if (name != null && name.length() > 0) {
            camera.checkClosestBody(this);
        }
    }

    @Override
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {

    }

	@Override
	public void addHit(Vector3d p0, Vector3d p1, NaturalCamera camera, Array<IFocus> hits) {
	}

}
