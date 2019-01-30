package gaia.cu9.ari.gaiaorbit.scenegraph;

import com.badlogic.gdx.graphics.g3d.ModelBatch;
import gaia.cu9.ari.gaiaorbit.render.ComponentType;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.ComponentTypes;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

/**
 * The sole purpose of this class is to act as an invisible focus.
 * 
 * @author tsagrista
 *
 */
public class Invisible extends CelestialBody {

    public Invisible() {
        super();
        this.parentName = "Universe";
        this.size = 500 * (float) Constants.M_TO_U;
        this.ct = new ComponentTypes(ComponentType.Invisible);
    }

    public Invisible(String name) {
        super();
        this.name = name;
        this.parentName = "Universe";
        this.size = 500 * (float) Constants.M_TO_U;
        this.ct = new ComponentTypes(ComponentType.Invisible);
    }

    @Override
    public void render(ModelBatch modelBatch, float alpha, double t) {
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
    }

    @Override
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {
    }

}
