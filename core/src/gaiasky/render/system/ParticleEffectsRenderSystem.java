/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.RenderGroup;
import gaiasky.render.RenderingContext;
import gaiasky.render.api.IRenderable;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Constants;
import gaiasky.util.Settings;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import org.lwjgl.opengl.GL30;

import java.util.Random;

public class ParticleEffectsRenderSystem extends ImmediateModeRenderSystem {
    private static final int N_PARTICLES = (Settings.settings.graphics.quality.ordinal() + 1) * 100;

    private final Random rand;
    private final Vector3 aux1f;
    private final Vector3d aux1, aux2, aux5;
    private final Vector3b aux1b;
    private int sizeOffset, tOffset;
    private final ComponentTypes ct;
    private final Vector3[] positions;
    private final Vector3[] additional;
    private final Vector3d[] camPositions;
    private final long baseTime;

    public ParticleEffectsRenderSystem(SceneRenderer sceneRenderer, RenderGroup rg, float[] alphas, ExtShaderProgram[] programs) {
        super(sceneRenderer, rg, alphas, programs);
        aux1f = new Vector3();
        aux1 = new Vector3d();
        aux2 = new Vector3d();
        aux5 = new Vector3d();
        aux1b = new Vector3b();
        rand = new Random(123);
        baseTime = System.currentTimeMillis();
        ct = new ComponentTypes(ComponentType.valueOf("Effects"));
        positions = new Vector3[N_PARTICLES * 2];
        additional = new Vector3[N_PARTICLES * 2];
        camPositions = new Vector3d[N_PARTICLES];
        float colFade = Color.toFloatBits(0.f, 0.f, 0.f, 0.f);
        float ctm = System.currentTimeMillis() / 1000f;
        for (int i = 0; i < N_PARTICLES * 2; i++) {
            if (i % 2 == 0) {
                // First in the pair
                positions[i] = new Vector3((float) (rand.nextFloat() * Constants.PC_TO_U), 0f, (float) (rand.nextFloat() * Constants.PC_TO_U));
                additional[i] = new Vector3(Color.toFloatBits(1f, 1f, 1f, 1f), 3 + rand.nextInt() % 8, ctm);
                camPositions[i / 2] = new Vector3d();
            } else {
                // Companion, start with same positions
                positions[i] = new Vector3(positions[i - 1]);
                additional[i] = new Vector3(colFade, additional[i - 1].y, ctm);
            }
        }
    }

    private float getT() {
        return (float) ((System.currentTimeMillis() - baseTime) / 1000d);
    }

    @Override
    protected void initShaderProgram() {
        Gdx.gl.glEnable(GL30.GL_LINE_SMOOTH);
        Gdx.gl.glEnable(GL30.GL_LINE_WIDTH);
        Gdx.gl.glHint(GL30.GL_NICEST, GL30.GL_LINE_SMOOTH_HINT);
        getShaderProgram().begin();
        getShaderProgram().setUniformf("u_ttl", 1f);
        getShaderProgram().end();

    }

    private double getFactor(double cspeed) {
        if (cspeed <= 0.4) {
            return 1;
        } else if (cspeed <= 1.0) {
            // lint(1..0.5)
            return MathUtilsd.lint(cspeed, 0.4, 1.0, 1.0, 0.5);
        } else if (cspeed <= 2.0) {
            // lint(0.5..0.3)
            return MathUtilsd.lint(cspeed, 1.0, 2.0, 0.5, 0.3);
        } else if (cspeed <= 3.0) {
            return 0.3;
        } else {
            // lint(0.3..0.1)
            return MathUtilsd.lint(cspeed, 3, 5, 0.3, 0.1);
        }
    }

    private void updatePositions(ICamera cam) {
        double tu = cam.getCurrent().speedScaling();
        double distLimit = 3500000 * tu * Constants.KM_TO_U * getFactor(Settings.settings.scene.camera.speed);
        double dist = distLimit * 0.8;
        distLimit *= distLimit;

        // If focus is very close, stop (jittering errors kick in)
        if (cam.getMode().isFocus()) {
            /*
             * Empirical fit to determine where the particle effects start to break down wrt distance to sol (since they are positioned globally)
             * <a href="https://mycurvefit.com/share/7b20c3bf-267d-4a8a-9498-844832d6509b">See curve and fit</a>
             */
            double distToSol = cam.getFocus().getAbsolutePosition(aux1b).lend() * Constants.U_TO_KM;
            double focusDistKm = cam.getFocus().getDistToCamera() * Constants.U_TO_KM;
            double cutDistKm = 11714150000000000d + (1900.228d - 11714150000000000d) / (1d + Math.pow(distToSol / 93302269999999990000d, 1.541734d));
            if (focusDistKm < cutDistKm) {
                return;
            }
        }
        Vector3d campos = aux1.set(cam.getPos());
        for (int i = 0; i < N_PARTICLES * 2; i++) {
            Vector3d pos = aux5.set(positions[i]);
            if (i % 2 == 0) {
                if (pos.dst2(campos) > distLimit) {
                    // New particle
                    pos.set(rand.nextDouble() - 0.5, rand.nextDouble() - 0.5, rand.nextDouble() - 0.5).scl(dist).add(campos);
                    pos.put(positions[i]);
                    additional[i].z = getT();
                    camPositions[i / 2].set(campos);
                }
            } else {
                // Companion, use previous camera position
                Vector3d prev_campos = camPositions[(i - 1) / 2];
                Vector3d camdiff = aux2.set(campos).sub(prev_campos);
                pos.set(positions[i - 1]).add(camdiff);
                pos.put(positions[i]);
            }
        }
    }

    @Override
    protected void initVertices() {
        meshes = new Array<>();
        addMeshData(N_PARTICLES * 2);
    }

    /**
     * Adds a new mesh data to the meshes list and increases the mesh data index
     *
     * @param nVertices The max number of vertices this mesh data can hold
     */
    private void addMeshData(int nVertices) {
        int mdi = createMeshData();
        curr = meshes.get(mdi);

        VertexAttribute[] attributes = buildVertexAttributes();
        curr.mesh = new IntMesh(false, nVertices, 0, attributes);

        curr.vertices = new float[nVertices * (curr.mesh.getVertexAttributes().vertexSize / 4)];
        curr.vertexSize = curr.mesh.getVertexAttributes().vertexSize / 4;
        curr.colorOffset = curr.mesh.getVertexAttribute(Usage.ColorPacked) != null ? curr.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
        sizeOffset = curr.mesh.getVertexAttribute(Usage.Generic) != null ? curr.mesh.getVertexAttribute(Usage.Generic).offset / 4 : 0;
        tOffset = curr.mesh.getVertexAttribute(Usage.Tangent) != null ? curr.mesh.getVertexAttribute(Usage.Tangent).offset / 4 : 0;
    }

    protected VertexAttribute[] buildVertexAttributes() {
        Array<VertexAttribute> attributes = new Array<>();
        attributes.add(new VertexAttribute(Usage.Position, 3, ExtShaderProgram.POSITION_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.ColorPacked, 4, ExtShaderProgram.COLOR_ATTRIBUTE));
        attributes.add(new VertexAttribute(Usage.Generic, 1, "a_size"));
        attributes.add(new VertexAttribute(Usage.Tangent, 1, "a_t"));

        VertexAttribute[] array = new VertexAttribute[attributes.size];
        for (int i = 0; i < attributes.size; i++)
            array[i] = attributes.get(i);
        return array;
    }

    @Override
    public void render(Array<IRenderable> renderables, ICamera camera, double t, RenderingContext rc) {
        this.rc = rc;
        run(preRunnables, renderables, camera);
        renderStud(renderables, camera, t);
        run(postRunnables, renderables, camera);
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        float alpha = getAlpha(ct);
        if (alpha > 0) {
            updatePositions(camera);

            // Regular
            Gdx.gl.glLineWidth(2f);

            if (curr != null) {
                curr.vertexIdx = 0;
                curr.numVertices = N_PARTICLES * 2;
                for (int i = 0; i < N_PARTICLES * 2; i++) {
                    // COLOR
                    curr.vertices[curr.vertexIdx + curr.colorOffset] = additional[i].x;
                    // SIZE
                    curr.vertices[curr.vertexIdx + sizeOffset] = additional[i].y;
                    // T
                    curr.vertices[curr.vertexIdx + tOffset] = additional[i].z;
                    // POSITION
                    curr.vertices[curr.vertexIdx] = positions[i].x;
                    curr.vertices[curr.vertexIdx + 1] = positions[i].y;
                    curr.vertices[curr.vertexIdx + 2] = positions[i].z;

                    curr.vertexIdx += curr.vertexSize;
                }

                // RENDER
                ExtShaderProgram shaderProgram = getShaderProgram();

                shaderProgram.begin();
                shaderProgram.setUniformMatrix("u_projView", camera.getCamera().combined);
                shaderProgram.setUniformf("u_camPos", camera.getCurrent().getPos().put(aux1f));
                shaderProgram.setUniformf("u_alpha", alpha * 0.6f);
                shaderProgram.setUniformf("u_sizeFactor", rc.scaleFactor);
                shaderProgram.setUniformf("u_t", getT());
                shaderProgram.setUniformf("u_ttl", 0.5f);

                // Relativistic effects
                addEffectsUniforms(shaderProgram, camera);

                curr.mesh.setVertices(curr.vertices, 0, N_PARTICLES * 2 * curr.vertexSize);
                curr.mesh.render(shaderProgram, GL20.GL_LINES);
                shaderProgram.end();
            }
        }
    }
}