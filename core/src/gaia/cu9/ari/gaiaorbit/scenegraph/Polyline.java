package gaia.cu9.ari.gaiaorbit.scenegraph;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;

import gaia.cu9.ari.gaiaorbit.data.orbit.PolylineData;
import gaia.cu9.ari.gaiaorbit.render.IGPULineRenderable;
import gaia.cu9.ari.gaiaorbit.render.system.LineRenderSystem;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;

/**
 * Represents a polyline which is sent to the GPU
 * @author tsagrista
 *
 */
public class Polyline extends LineObject implements IGPULineRenderable {

    /** GPU rendering attributes **/
    protected boolean inGpu = false;
    protected int offset;
    protected int count;

    protected PolylineData polylineData;

    public Polyline() {
        super();
        this.localTransform = new Matrix4();
    }

    @Override
    public void render(LineRenderSystem renderer, ICamera camera, float alpha) {
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
        addToRender(this, RenderGroup.LINE_GPU);
    }

    @Override
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {
        transform.getMatrix(localTransform);
    }

    /**
     * Sets the 3D points of the line in the internal reference system.
     * @param points Vector with the points. If length is not multiple of 3, some points are discarded.
     */
    public void setPoints(double[] points) {
        int n = points.length;
        if (n % 3 != 0) {
            n = n - n % 3;
        }
        int npoints = n / 3;
        polylineData = new PolylineData(npoints);
        Array<Double> x = polylineData.x;
        Array<Double> y = polylineData.y;
        Array<Double> z = polylineData.z;
        for (int i = 0; i < npoints; i++) {
            x.add(points[i * 3]);
            y.add(points[i * 3 + 1]);
            z.add(points[i * 3 + 2]);
        }
    }

    @Override
    public boolean inGpu() {
        return inGpu;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public PolylineData getPolyline() {
        return polylineData;
    }

    @Override
    public float[] getColor() {
        return cc;
    }

    @Override
    public double getAlpha() {
        return cc[3];
    }

    @Override
    public Matrix4 getLocalTransform() {
        return localTransform;
    }

    @Override
    public SceneGraphNode getParent() {
        return parent;
    }

    @Override
    public void setInGpu(boolean inGpu) {
        this.inGpu = inGpu;
    }

    @Override
    public void setOffset(int offset) {
        this.offset = offset;
    }

    @Override
    public void setCount(int count) {
        this.count = count;
    }

}
