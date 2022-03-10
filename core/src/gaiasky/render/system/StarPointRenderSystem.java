/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.IRenderable;
import gaiasky.render.SceneGraphRenderer.RenderGroup;
import gaiasky.scenegraph.CelestialBody;
import gaiasky.scenegraph.camera.CameraManager;
import gaiasky.scenegraph.camera.FovCamera;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.Settings.SceneSettings.StarSettings;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import org.lwjgl.opengl.GL30;

public class StarPointRenderSystem extends ImmediateModeRenderSystem implements IObserver {
    protected static final Log logger = Logger.getLogger(StarPointRenderSystem.class);

    private final double BRIGHTNESS_FACTOR = 10;

    Vector3 aux;
    int sizeOffset, pmOffset;
    ComponentType ct;
    private float[] opacityLimits;
    private float[] alphaSizeBrRc;

    private Texture starTex;

    boolean initializing;

    private boolean pointUpdateFlag = true;

    public StarPointRenderSystem(RenderGroup rg, float[] alphas, ExtShaderProgram[] shaders, ComponentType ct) {
        super(rg, alphas, shaders);
        EventManager.instance.subscribe(this,  Event.STAR_MIN_OPACITY_CMD, Event.STAR_TEXTURE_IDX_CMD, Event.STAR_POINT_UPDATE_FLAG);
        this.ct = ct;
        this.alphaSizeBrRc = new float[4];
        initializing = true;
        setStarTexture(Settings.settings.scene.star.getStarTexture());
    }

    public void setStarTexture(String starTexture) {
        starTex = new Texture(Settings.settings.data.dataFileHandle(starTexture), true);
        starTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    @Override
    protected void initShaderProgram() {
        Gdx.gl.glEnable(GL30.GL_POINT_SPRITE);
        Gdx.gl.glEnable(GL30.GL_VERTEX_PROGRAM_POINT_SIZE);

        opacityLimits = new float[] { Settings.settings.scene.star.opacity[0], Settings.settings.scene.star.opacity[1] };

        ExtShaderProgram shaderProgram = getShaderProgram();
        shaderProgram.begin();
        // Uniforms that rarely change
        shaderProgram.setUniformf("u_thAnglePoint", 1e-10f, 1.5e-8f);
        shaderProgram.end();

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
        if (pointUpdateFlag) {
            // Reset variables
            curr.clear();

            ensureTempVertsSize(renderables.size * curr.vertexSize);
            renderables.forEach(r -> {
                // 2 FPS gain
                CelestialBody cb = (CelestialBody) r;
                float[] col = cb.cc;

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
                tempVerts[curr.vertexIdx + pmOffset] = (float) cb.getPmX();
                tempVerts[curr.vertexIdx + pmOffset + 1] = (float) cb.getPmY();
                tempVerts[curr.vertexIdx + pmOffset + 2] = (float) cb.getPmZ();

                curr.vertexIdx += curr.vertexSize;

            });
            curr.mesh.setVertices(tempVerts, 0, curr.vertexIdx);
            // Put flag down
            pointUpdateFlag = false;
        }
        ExtShaderProgram shaderProgram = getShaderProgram();

        shaderProgram.begin();
        shaderProgram.setUniformMatrix("u_projView", camera.getCamera().combined);
        shaderProgram.setUniformf("u_camPos", camera.getPos().put(aux));
        shaderProgram.setUniformf("u_camDir", camera.getCamera().direction);
        shaderProgram.setUniformi("u_cubemap", Settings.settings.program.modeCubemap.active ? 1 : 0);
        shaderProgram.setUniformf("u_brightnessPower", Settings.settings.scene.star.power);
        shaderProgram.setUniformf("u_ar", Settings.settings.program.modeStereo.isStereoHalfWidth() ? 2f : 1f);
        addEffectsUniforms(shaderProgram, camera);
        // Update projection if fovMode is 3
        int fovMode = camera.getMode().getGaiaFovMode();
        if (fovMode == 3) {
            // Cam is Fov1 & Fov2
            FovCamera cam = ((CameraManager) camera).fovCamera;
            // Update combined
            PerspectiveCamera[] cams = camera.getFrontCameras();
            shaderProgram.setUniformMatrix("u_projView", cams[cam.dirIndex].combined);
        }
        alphaSizeBrRc[2] = (float) (Settings.settings.scene.star.brightness * BRIGHTNESS_FACTOR);
        alphaSizeBrRc[3] = rc.scaleFactor;
        if (starTex != null) {
            starTex.bind(0);
            shaderProgram.setUniformi("u_starTex", 0);
        }

        shaderProgram.setUniform2fv("u_opacityLimits", opacityLimits, 0, 2);

        alphaSizeBrRc[0] = alphas[ComponentType.Stars.ordinal()];
        alphaSizeBrRc[1] = ((fovMode == 0 ? (Settings.settings.program.modeStereo.isStereoFullWidth() ? 1f : 2f) : 10f) * StarSettings.getStarPointSize() * rc.scaleFactor) / camera.getFovFactor();
        shaderProgram.setUniform4fv("u_alphaSizeBrRc", alphaSizeBrRc, 0, 4);

        // Days since epoch
        // Emulate double with floats, for compatibility
        double curRt = AstroUtils.getDaysSince(GaiaSky.instance.time.getTime(), AstroUtils.JD_J2015);
        float curRt2 = (float) (curRt - (double) ((float) curRt));
        shaderProgram.setUniformf("u_t", (float) curRt, curRt2);

        try {
            curr.mesh.render(shaderProgram, GL20.GL_POINTS);
        } catch (IllegalArgumentException e) {
            logger.error("Render exception");
        }
        shaderProgram.end();

    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
        case STAR_MIN_OPACITY_CMD -> opacityLimits[0] = (float) data[0];
        case STAR_TEXTURE_IDX_CMD -> GaiaSky.postRunnable(() -> setStarTexture(Settings.settings.scene.star.getStarTexture()));
        case STAR_POINT_UPDATE_FLAG -> pointUpdateFlag = (Boolean) data[0];
        default ->{}
        }
    }
}
