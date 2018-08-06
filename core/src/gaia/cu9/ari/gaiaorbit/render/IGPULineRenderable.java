package gaia.cu9.ari.gaiaorbit.render;

import com.badlogic.gdx.math.Matrix4;

import gaia.cu9.ari.gaiaorbit.data.orbit.PolylineData;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode;

public interface IGPULineRenderable extends IRenderable {

    public boolean inGpu();

    public void markForUpdate();

    public int getOffset();

    public int getCount();

    public PolylineData getPolyline();

    public float[] getColor();

    public double getAlpha();

    public Matrix4 getLocalTransform();

    public SceneGraphNode getParent();

    public void setInGpu(boolean inGpu);

    public void setOffset(int offset);

    public void setCount(int count);

    public boolean hasMeshData();

    public void setLineWidth(float lineWidth);

    public float getLineWidth();

}
