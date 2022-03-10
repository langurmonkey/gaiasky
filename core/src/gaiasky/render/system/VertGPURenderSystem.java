/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.utils.Array;
import gaiasky.data.util.PointCloudData;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.IGPUVertsRenderable;
import gaiasky.render.IRenderable;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.scenegraph.Orbit;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Settings;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.Vector3d;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * Renders vertices using a VBO.
 */
public class VertGPURenderSystem<T extends IGPUVertsRenderable> extends ImmediateModeRenderSystem implements IObserver {
    protected ICamera camera;
    protected boolean lines;
    protected int coordOffset;

    public VertGPURenderSystem(RenderGroup rg, float[] alphas, ExtShaderProgram[] shaders, boolean lines) {
        super(rg, alphas, shaders);
        this.lines = lines;
        EventManager.instance.subscribe(this, Event.MARK_FOR_UPDATE);
    }

    public boolean isLine() {
        return lines;
    }

    public boolean isPoint() {
        return !lines;
    }

    @Override
    protected void initShaderProgram() {
        if (isLine()) {
            Gdx.gl.glEnable(GL30.GL_LINE_SMOOTH);
            Gdx.gl.glEnable(GL30.GL_LINE_WIDTH);
            Gdx.gl.glHint(GL30.GL_NICEST, GL30.GL_LINE_SMOOTH_HINT);
        } else if (isPoint()) {
            Gdx.gl.glEnable(GL30.GL_POINT_SPRITE);
            Gdx.gl.glEnable(GL30.GL_VERTEX_PROGRAM_POINT_SIZE);
        }
    }

    @Override
    protected void initVertices() {
        meshes = new Array<>();
    }

    /**
     * Adds a new mesh data to the meshes list and increases the mesh data index
     *
     * @param nVertices The max number of vertices this mesh data can hold
     * @return The index of the new mesh data
     */
    private int addMeshData(int nVertices) {
        int mdi = createMeshData();
        curr = meshes.get(mdi);

        VertexAttribute[] attributes = buildVertexAttributes();
        curr.mesh = new IntMesh(false, nVertices, 0, attributes);

        curr.vertexSize = curr.mesh.getVertexAttributes().vertexSize / 4;
        curr.colorOffset = curr.mesh.getVertexAttribute(Usage.ColorPacked) != null ? curr.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
        coordOffset = curr.mesh.getVertexAttribute(Usage.Generic) != null ? curr.mesh.getVertexAttribute(Usage.Generic).offset / 4 : 0;
        return mdi;
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        if (isLine()) {
            // Enable GL_LINE_SMOOTH
            Gdx.gl20.glEnable(GL11.GL_LINE_SMOOTH);
            Gdx.gl.glHint(GL20.GL_NICEST, GL11.GL_LINE_SMOOTH_HINT);
            // Enable GL_LINE_WIDTH
            Gdx.gl20.glEnable(GL20.GL_LINE_WIDTH);
        }

        this.camera = camera;
        renderables.forEach(r -> {
            T renderable = (T) r;

            /*
             * ADD LINES
             */
            if (!inGpu(renderable)) {
                // Remove previous line data if present
                if (getOffset(renderable) >= 0) {
                    clearMeshData(getOffset(renderable));
                    setOffset(renderable, -1);
                }

                // Actually add data
                PointCloudData od = renderable.getPointCloud();
                int nPoints = od.getNumPoints();

                // Initialize or fetch mesh data
                if (getOffset(renderable) < 0) {
                    setOffset(renderable,addMeshData(nPoints));
                } else {
                    curr = meshes.get(getOffset(renderable));
                    // Check we still have capacity, otherwise, reinitialize.
                    if (curr.numVertices != od.getNumPoints()) {
                        curr.clear();
                        curr.mesh.dispose();
                        meshes.set(getOffset(renderable), null);
                        setOffset(renderable,addMeshData(nPoints));
                    }
                }
                // Coord maps time
                long t0 = od.getDate(0).getEpochSecond();
                long t1 = od.getDate(od.getNumPoints() - 1).getEpochSecond();
                long t01 = t1 - t0;

                // Ensure vertices capacity
                ensureTempVertsSize((nPoints + 2) * curr.vertexSize);
                curr.vertices = tempVerts;
                float[] cc = renderable.getColor();
                for (int point_i = 0; point_i < nPoints; point_i++) {
                    coord((float) ((double) (od.getDate(point_i).getEpochSecond() - t0) / (double) t01));
                    color(cc[0], cc[1], cc[2], 1.0);
                    vertex((float) od.getX(point_i), (float) od.getY(point_i), (float) od.getZ(point_i));
                }

                // Close loop
                if (renderable.isClosedLoop()) {
                    coord(1f);
                    color(cc[0], cc[1], cc[2], 1.0);
                    vertex((float) od.getX(0), (float) od.getY(0), (float) od.getZ(0));
                }

                int count = nPoints * curr.vertexSize;
                setCount(renderable, count);
                curr.mesh.setVertices(curr.vertices, 0, count);
                curr.vertices = null;

                setInGpu(renderable, true);
            }
            curr = meshes.get(getOffset(renderable));

            /*
             * RENDER
             */
            ExtShaderProgram shaderProgram = getShaderProgram();

            shaderProgram.begin();

            // Regular
            if (isLine())
                Gdx.gl.glLineWidth(renderable.getPrimitiveSize() * Settings.settings.scene.lineWidth);
            if (isPoint())
                shaderProgram.setUniformf("u_pointSize", renderable.getPrimitiveSize());

            shaderProgram.setUniformMatrix("u_worldTransform", renderable.getLocalTransform());
            shaderProgram.setUniformMatrix("u_projView", camera.getCamera().combined);
            shaderProgram.setUniformf("u_alpha", (float) (renderable.getAlpha()) * getAlpha(renderable));
            shaderProgram.setUniformf("u_coordPos", renderable instanceof Orbit ? (float) ((Orbit) renderable).coord : 1f);
            shaderProgram.setUniformf("u_period", renderable instanceof Orbit && ((Orbit) renderable).oc != null ? (float) ((Orbit) renderable).oc.period : 0f);
            if (renderable.getParent() != null) {
                Vector3d urp = renderable.getParent().getUnrotatedPos();
                if (urp != null)
                    shaderProgram.setUniformf("u_parentPos", (float) urp.x, (float) urp.y, (float) urp.z);
                else
                    shaderProgram.setUniformf("u_parentPos", 0, 0, 0);

            }

            // Rel, grav, z-buffer
            addEffectsUniforms(shaderProgram, camera);
            curr.mesh.render(shaderProgram, renderable.getGlPrimitive());

            shaderProgram.end();
        });
    }

    private void coord(float value) {
        curr.vertices[curr.vertexIdx + coordOffset] = value;
    }

    protected VertexAttribute[] buildVertexAttributes() {
        Array<VertexAttribute> attributes = new Array<>();
        attributes.add(new VertexAttribute(Usage.Position, 3, ExtShaderProgram.POSITION_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.ColorPacked, 4, ExtShaderProgram.COLOR_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.Generic, 1, "a_coord"));

        VertexAttribute[] array = new VertexAttribute[attributes.size];
        for (int i = 0; i < attributes.size; i++)
            array[i] = attributes.get(i);
        return array;
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if(event == Event.MARK_FOR_UPDATE) {
            IRenderable renderable = (IRenderable) source;
            RenderGroup rg = (RenderGroup) data[0];
            if(rg == RenderGroup.LINE_GPU || rg == RenderGroup.POINT_GPU) {
                setInGpu(renderable, false);
            }
        }
    }
}
