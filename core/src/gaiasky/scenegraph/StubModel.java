package gaiasky.scenegraph;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ILineRenderable;
import gaiasky.render.IModelRenderable;
import gaiasky.render.RenderingContext;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.render.system.LineRenderSystem;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Constants;
import gaiasky.util.gdx.IntModelBatch;
import gaiasky.util.gdx.model.IntModelInstance;
import gaiasky.util.gdx.shader.Environment;
import gaiasky.util.gdx.shader.Material;
import gaiasky.util.gdx.shader.attribute.BlendingAttribute;
import gaiasky.util.time.ITimeFrameProvider;
import gaiasky.vr.openvr.VRContext.VRDevice;

public class StubModel extends SceneGraphNode implements IModelRenderable, ILineRenderable {

    public IntModelInstance instance;
    private final Environment env;
    private final VRDevice device;
    private final Vector3 beamP0;
    private final Vector3 beamP1;

    public StubModel(VRDevice device, Environment env) {
        super();
        this.env = env;
        this.instance = device.getModelInstance();
        this.device = device;
        beamP0 = new Vector3();
        beamP1 = new Vector3();
        this.cc = new float[] { 1f, 0f, 0f };
        setCt("Others");
    }

    @Override
    public ComponentTypes getComponentType() {
        return ct;
    }

    @Override
    public double getDistToCamera() {
        return 0;
    }

    @Override
    public float getOpacity() {
        return 0;
    }

    public void addToRenderLists(RenderGroup rg) {
        if (this.shouldRender()) {
            if (rg != null) {
                addToRender(this, rg);
            }
            addToRender(this, RenderGroup.LINE);
        }
    }

    @Override
    protected void addToRenderLists(ICamera camera) {
    }

    @Override
    public void render(IntModelBatch modelBatch, float alpha, double t, RenderingContext rc, RenderGroup group) {
        setTransparency(alpha);
        modelBatch.render(instance, env);
    }

    /**
     * Occlusion rendering
     */
    public void renderOpaque(IntModelBatch modelBatch, float alpha, double t) {
        setTransparency(alpha);
        modelBatch.render(instance, env);
    }

    @Override
    public void render(LineRenderSystem renderer, ICamera camera, float alpha) {
        Matrix4 transform = instance.transform;
        beamP0.set(0, -0.01f, 0).mul(transform);
        beamP1.set(0, (float) -(Constants.MPC_TO_U - Constants.PC_TO_U), (float) -Constants.MPC_TO_U).mul(transform);
        renderer.addLine(this, beamP0.x, beamP0.y, beamP0.z, beamP1.x, beamP1.y - 0.1f, beamP1.z, 1f, 0, 0, 1f);
    }

    @Override
    public int getGlPrimitive() {
        return GL20.GL_LINES;
    }

    public void setTransparency(float alpha) {
        if (instance != null) {
            int n = instance.materials.size;
            for (int i = 0; i < n; i++) {
                Material mat = instance.materials.get(i);
                BlendingAttribute ba = null;
                if (mat.has(BlendingAttribute.Type)) {
                    ba = (BlendingAttribute) mat.get(BlendingAttribute.Type);
                } else {
                    ba = new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
                    mat.set(ba);
                }
                ba.opacity = alpha;
            }
        }
    }

    @Override
    public boolean hasAtmosphere() {
        return false;
    }

    @Override
    public void updateLocalValues(ITimeFrameProvider time, ICamera camera) {

    }

    public VRDevice getDevice() {
        return device;
    }

    /**
     * Gets the initial point of the controller beam in camera space
     *
     * @return Initial point of controller beam
     */
    public Vector3 getBeamP0() {
        return beamP0;
    }

    /**
     * Gets the end point of the controller beam in camera space
     *
     * @return End point of controller beam
     */
    public Vector3 getBeamP1() {
        return beamP1;
    }

    @Override
    public float getLineWidth() {
        return 2;
    }

}
