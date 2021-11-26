/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.IRenderable;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.scenegraph.CelestialBody;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Settings;
import gaiasky.util.Settings.SceneSettings.StarSettings;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import org.lwjgl.opengl.GL30;

public class StarPointRenderSystem extends ImmediateRenderSystem implements IObserver {
    private final double BRIGHTNESS_FACTOR = 10;

    boolean starColorTransit = false;
    Vector3 aux;
    int sizeOffset, pmOffset;
    ComponentType ct;
    float[] pointAlpha, alphaSizeFovBr;

    boolean initializing;

    public StarPointRenderSystem(RenderGroup rg, float[] alphas, ExtShaderProgram[] shaders, ComponentType ct) {
        super(rg, alphas, shaders);
        EventManager.instance.subscribe(this, Events.TRANSIT_COLOUR_CMD, Events.ONLY_OBSERVED_STARS_CMD, Events.STAR_MIN_OPACITY_CMD);
        this.ct = ct;
        this.alphaSizeFovBr = new float[4];
        initializing = true;
    }

    @Override
    protected void initShaderProgram() {
        Gdx.gl.glEnable(GL30.GL_POINT_SPRITE);
        Gdx.gl.glEnable(GL30.GL_VERTEX_PROGRAM_POINT_SIZE);

        pointAlpha = new float[] { Settings.settings.scene.star.opacity[0], Settings.settings.scene.star.opacity[1]};

        for (ExtShaderProgram p : programs) {
            if (p != null) {
                p.begin();
                p.setUniform2fv("u_pointAlpha", pointAlpha, 0, 2);
                p.end();
            }
        }

    }

    @Override
    protected void initVertices() {
        meshes = new Array<>();
        curr = new MeshData();
        meshes.add(curr);

        aux = new Vector3();

        // Init renderer
        int nVertices = 30;

        VertexAttribute[] attribs = buildVertexAttributes();
        curr.mesh = new IntMesh(false, nVertices, 0, attribs);

        curr.vertexSize = curr.mesh.getVertexAttributes().vertexSize / 4;
        curr.colorOffset = curr.mesh.getVertexAttribute(Usage.ColorPacked) != null ? curr.mesh.getVertexAttribute(Usage.ColorPacked).offset / 4 : 0;
        pmOffset = curr.mesh.getVertexAttribute(Usage.Tangent) != null ? curr.mesh.getVertexAttribute(Usage.Tangent).offset / 4 : 0;
        sizeOffset = curr.mesh.getVertexAttribute(Usage.Generic) != null ? curr.mesh.getVertexAttribute(Usage.Generic).offset / 4 : 0;
    }

    protected VertexAttribute[] buildVertexAttributes() {
        Array<VertexAttribute> attribs = new Array<>();
        attribs.add(new VertexAttribute(Usage.Position, 3, ExtShaderProgram.POSITION_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.Tangent, 3, "a_pm"));
        attribs.add(new VertexAttribute(Usage.ColorPacked, 4, ExtShaderProgram.COLOR_ATTRIBUTE));
        attribs.add(new VertexAttribute(Usage.Generic, 1, "a_size"));

        VertexAttribute[] array = new VertexAttribute[attribs.size];
        for (int i = 0; i < attribs.size; i++)
            array[i] = attribs.get(i);
        return array;
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        if (POINT_UPDATE_FLAG) {
            // Reset variables
            curr.clear();
            
            ensureTempVertsSize(renderables.size * curr.vertexSize);
            renderables.forEach(r->{
                // 2 FPS gain
                CelestialBody cb = (CelestialBody) r;
                float[] col = starColorTransit ? cb.ccTransit : cb.cc;

                // COLOR
                tempVerts[curr.vertexIdx + curr.colorOffset] = Color.toFloatBits(col[0], col[1], col[2], cb.opacity);

                // SIZE
                tempVerts[curr.vertexIdx + sizeOffset] = (float) cb.getRadius();

                // POSITION
                aux.set(cb.pos.x.floatValue(), cb.pos.y.floatValue(), cb.pos.z.floatValue());
                final int idx = curr.vertexIdx;
                tempVerts[idx] = aux.x;
                tempVerts[idx + 1] = aux.y;
                tempVerts[idx + 2] = aux.z;

                // PROPER MOTION
                tempVerts[curr.vertexIdx + pmOffset] = (float) cb.getPmX() * 0f;
                tempVerts[curr.vertexIdx + pmOffset + 1] = (float) cb.getPmY() * 0f;
                tempVerts[curr.vertexIdx + pmOffset + 2] = (float) cb.getPmZ() * 0f;

                curr.vertexIdx += curr.vertexSize;

            });
            // Put flag down
            POINT_UPDATE_FLAG = false;
        }
        if (!camera.getMode().isGaiaFov()) {
            int fovMode = camera.getMode().getGaiaFovMode();

            ExtShaderProgram shaderProgram = getShaderProgram();

            shaderProgram.begin();
            shaderProgram.setUniformMatrix("u_projModelView", camera.getCamera().combined);
            shaderProgram.setUniformf("u_camPos", camera.getCurrent().getPos().put(aux));

            alphaSizeFovBr[0] = alphas[ct.ordinal()];
            alphaSizeFovBr[1] = fovMode == 0 ? (10 * StarSettings.getStarPointSize() * rc.scaleFactor * (Settings.settings.program.modeStereo.isStereoFullWidth() ? 1 : 2)) : (Settings.settings.scene.star.pointSize * rc.scaleFactor * 10);
            alphaSizeFovBr[2] = camera.getFovFactor();
            alphaSizeFovBr[3] = (float) (Settings.settings.scene.star.brightness * BRIGHTNESS_FACTOR);
            shaderProgram.setUniform4fv("u_alphaSizeFovBr", alphaSizeFovBr, 0, 4);

            shaderProgram.setUniformf("u_t", (float) AstroUtils.getMsSinceJ2000(GaiaSky.instance.time.getTime()));
            shaderProgram.setUniformf("u_ar", Settings.settings.program.modeStereo.isStereoHalfWidth() ? 0.5f : 1f);
            shaderProgram.setUniformf("u_thAnglePoint", (float) Settings.settings.scene.star.threshold.point);

            // Relativistic effects
            addEffectsUniforms(shaderProgram, camera);

            curr.mesh.setVertices(tempVerts, 0, curr.vertexIdx);
            curr.mesh.render(shaderProgram, GL30.GL_POINTS);
            shaderProgram.end();
        }

    }


    @Override
    public void notify(final Events event, final Object... data) {
        switch (event) {
        case TRANSIT_COLOUR_CMD:
            starColorTransit = (boolean) data[1];
            POINT_UPDATE_FLAG = true;
            break;
        case ONLY_OBSERVED_STARS_CMD:
            POINT_UPDATE_FLAG = true;
            break;
        case STAR_MIN_OPACITY_CMD:
            for (ExtShaderProgram p : programs) {
                if (p != null && p.isCompiled()) {
                    pointAlpha[0] = (float) data[0];
                    GaiaSky.postRunnable(() -> {
                        p.begin();
                        p.setUniform2fv("u_pointAlpha", pointAlpha, 0, 2);
                        p.end();
                    });
                }
            }
            break;
        default:
            break;
        }
    }
}
