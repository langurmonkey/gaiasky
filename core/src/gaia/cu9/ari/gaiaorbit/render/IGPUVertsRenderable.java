package gaia.cu9.ari.gaiaorbit.render;

import com.badlogic.gdx.math.Matrix4;
import gaia.cu9.ari.gaiaorbit.data.util.PointCloudData;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode;

public interface IGPUVertsRenderable extends IRenderable {
    public boolean inGpu();

    public void markForUpdate();

    public int getOffset();

    public int getCount();

    public PointCloudData getPointCloud();

    public float[] getColor();

    public double getAlpha();

    public Matrix4 getLocalTransform();

    public SceneGraphNode getParent();

    public void setInGpu(boolean inGpu);

    public void setOffset(int offset);

    public void setCount(int count);

    public boolean isClosedLoop();

    public void setClosedLoop(boolean closedLoop);

    /**
     * Line width for lines, point size for points
     * @param size The size
     */
    public void setPrimitiveSize(float size);

    public float getPrimitiveSize();
}
