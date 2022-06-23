/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.draw;

import com.badlogic.ashley.core.Entity;
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
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.render.system.ImmediateModeRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Render;
import gaiasky.scene.view.RenderView;
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

/**
 * Renders single points. Gathers all celestial entities that are to be
 * represented using point primitives into a mesh and renders them with
 * a single draw call.
 */
public class SinglePointRenderer extends ImmediateModeRenderSystem implements IObserver {
    protected static final Log logger = Logger.getLogger(SinglePointRenderer.class);

    private final double BRIGHTNESS_FACTOR = 10;

    Vector3 aux;
    int sizeOffset, pmOffset;
    ComponentType ct;
    private float[] opacityLimits;
    private float[] alphaSizeBrRc;

    private Texture starTex;

    private final RenderView view;

    boolean initializing;

    public SinglePointRenderer(RenderGroup rg, float[] alphas, ExtShaderProgram[] shaders, ComponentType ct) {
        super(rg, alphas, shaders);

        this.view = new RenderView();
        this.ct = ct;

        this.alphaSizeBrRc = new float[4];
        initializing = true;
        setStarTexture(Settings.settings.scene.star.getStarTexture());

        EventManager.instance.subscribe(this, Event.STAR_MIN_OPACITY_CMD, Event.BILLBOARD_TEXTURE_IDX_CMD);
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
        VertexAttribute[] attribs = buildVertexAttributes();
        createNewMesh(50, new VertexAttributes(attribs));
    }

    private void createNewMesh(int numVertices, VertexAttributes attributes) {
        curr.mesh = new IntMesh(false, numVertices, 0, attributes);

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

    public void ensureMeshSize(int desiredNumVertices) {
        VertexAttributes attributes = curr.mesh.getVertexAttributes();
        if (curr.mesh.getMaxVertices() < desiredNumVertices) {
            int newNumVertices = (int) (desiredNumVertices * 1.2);
            logger.info("Buffer capacity too small (" + curr.mesh.getMaxVertices() + " v " + desiredNumVertices + "), growing to " + newNumVertices);
            // Dispose old and create new mesh.
            curr.mesh.dispose();
            createNewMesh(newNumVertices, attributes);
        }
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        curr.clear();

        ensureTempVertsSize(renderables.size * curr.vertexSize);
        ensureMeshSize(renderables.size);
        renderables.forEach(r -> {
            Entity entity = ((Render) r).entity;
            view.setEntity(entity);
            var base = Mapper.base.get(entity);
            var body = Mapper.body.get(entity);
            var pm = Mapper.pm.get(entity);
            float[] col = body.color;

            // COLOR
            tempVerts[curr.vertexIdx + curr.colorOffset] = Color.toFloatBits(col[0], col[1], col[2], base.opacity);

            // SIZE
            tempVerts[curr.vertexIdx + sizeOffset] = (float) view.getRadius();

            // POSITION
            aux.set(body.pos.x.floatValue(), body.pos.y.floatValue(), body.pos.z.floatValue());
            final int idx = curr.vertexIdx;
            tempVerts[idx] = aux.x;
            tempVerts[idx + 1] = aux.y;
            tempVerts[idx + 2] = aux.z;

            // PROPER MOTION
            tempVerts[curr.vertexIdx + pmOffset] = pm != null ? pm.pm.x : 0;
            tempVerts[curr.vertexIdx + pmOffset + 1] = pm != null ? pm.pm.y : 0;
            tempVerts[curr.vertexIdx + pmOffset + 2] = pm != null ? pm.pm.z : 0;

            curr.vertexIdx += curr.vertexSize;
        });
        curr.mesh.setVertices(tempVerts, 0, curr.vertexIdx);

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
        case BILLBOARD_TEXTURE_IDX_CMD -> GaiaSky.postRunnable(() -> setStarTexture(Settings.settings.scene.star.getStarTexture()));
        default -> {
        }
        }
    }
}
