package gaiasky.scene.system.render.pass;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.render.RenderGroup;
import gaiasky.render.RenderingContext;
import gaiasky.render.api.IRenderable;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Render;
import gaiasky.scene.entity.EntityUtils;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.scene.system.render.draw.model.ModelEntityRenderSystem;
import gaiasky.util.Constants;
import gaiasky.util.Settings;
import gaiasky.util.math.IntersectorDouble;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;

import java.util.*;

import static gaiasky.render.RenderGroup.MODEL_PIX;

/**
 * Render pass for shadow maps.
 */
public class ShadowMapRenderPass {
    /** The scene renderer object. **/
    private final SceneRenderer sceneRenderer;
    /** Contains the code to render models. **/
    private final ModelEntityRenderSystem modelRenderer;
    // Camera at light position, with same direction. For shadow mapping
    private Camera cameraLight;
    /** Contains the candidates for regular and tessellated shadow maps. **/
    private List<Entity> shadowCandidates/*, shadowCandidatesTess */;
    // Dimension 1: number of shadows, dimension 2: number of lights
    private Matrix4[][] shadowMapCombined;
    // Dimension 1: number of shadows, dimension 2: number of lights
    public FrameBuffer[][] shadowMapFb;
    /** Map containing the shadow map for each model body. **/
    public Map<Entity, Texture> smTexMap;
    /** Map containing the combined matrix for each model body. **/
    public Map<Entity, Matrix4> smCombinedMap;

    private Vector3 aux1;
    private Vector3d aux1d, aux2d, aux3d;
    private Vector3b aux1b;

    public ShadowMapRenderPass(final SceneRenderer sceneRenderer) {
        this.sceneRenderer = sceneRenderer;
        this.modelRenderer = new ModelEntityRenderSystem(sceneRenderer);
    }

    public void initialize() {
        // Shadow map camera
        cameraLight = new PerspectiveCamera(0.5f, Settings.settings.scene.renderer.shadow.resolution, Settings.settings.scene.renderer.shadow.resolution);

        // Aux vectors
        aux1 = new Vector3();
        aux1d = new Vector3d();
        aux2d = new Vector3d();
        aux3d = new Vector3d();
        aux1b = new Vector3b();

        // Build frame buffers and arrays
        buildShadowMapData();
    }

    /**
     * Builds the shadow map data; frame buffers, arrays, etc.
     */
    public void buildShadowMapData() {
        if (shadowMapFb != null) {
            for (FrameBuffer[] frameBufferArray : shadowMapFb)
                for (FrameBuffer fb : frameBufferArray) {
                    if (fb != null)
                        fb.dispose();
                }
        }

        // Shadow map frame buffer
        shadowMapFb = new FrameBuffer[Settings.settings.scene.renderer.shadow.number][Constants.N_DIR_LIGHTS];
        // Shadow map combined matrices
        shadowMapCombined = new Matrix4[Settings.settings.scene.renderer.shadow.number][Constants.N_DIR_LIGHTS];
        // Init
        for (int i = 0; i < Settings.settings.scene.renderer.shadow.number; i++) {
            for (int j = 0; j < 1; j++) {
                shadowMapFb[i][j] = new FrameBuffer(Pixmap.Format.RGBA8888, Settings.settings.scene.renderer.shadow.resolution, Settings.settings.scene.renderer.shadow.resolution, true);
                shadowMapCombined[i][j] = new Matrix4();
            }
        }
        if (smTexMap == null)
            smTexMap = new HashMap<>();
        smTexMap.clear();

        if (smCombinedMap == null)
            smCombinedMap = new HashMap<>();
        smCombinedMap.clear();

        if (shadowCandidates == null) {
            shadowCandidates = new ArrayList<>(Settings.settings.scene.renderer.shadow.number);
            //shadowCandidatesTess = new ArrayList<>(Settings.settings.scene.renderer.shadow.number);
        }
        shadowCandidates.clear();
        //shadowCandidatesTess.clear();
    }

    private void renderShadowMapCandidates(List<Entity> candidates, int shadowNRender, ICamera camera) {
        var renderAssets = sceneRenderer.getRenderAssets();
        int i = 0;
        int j = 0;
        // Normal bodies
        for (Entity candidate : candidates) {
            var body = Mapper.body.get(candidate);
            var model = Mapper.model.get(candidate);
            var scaffolding = Mapper.modelScaffolding.get(candidate);

            Vector3 camDir = aux1.set(model.model.directional(0).direction);
            // Direction is that of the light
            cameraLight.direction.set(camDir);

            double radius = (body.size / 2.0) * scaffolding.sizeScaleFactor;
            // Distance from camera to object, radius * sv[0]
            double distance = radius * scaffolding.shadowMapValues[0];
            // Position, factor of radius
            Vector3b objPos = EntityUtils.getAbsolutePosition(candidate, aux1b);
            objPos.sub(camera.getPos()).sub(camDir.nor().scl((float) distance));
            objPos.put(cameraLight.position);
            // Up is perpendicular to dir
            if (cameraLight.direction.y != 0 || cameraLight.direction.z != 0)
                aux1.set(1, 0, 0);
            else
                aux1.set(0, 1, 0);
            cameraLight.up.set(cameraLight.direction).crs(aux1);

            // Near is sv[1]*radius before the object
            cameraLight.near = (float) (distance - radius * scaffolding.shadowMapValues[1]);
            // Far is sv[2]*radius after the object
            cameraLight.far = (float) (distance + radius * scaffolding.shadowMapValues[2]);

            // Update cam
            cameraLight.update(false);

            // Render model depth map to frame buffer
            shadowMapFb[i][j].begin();
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

            // No tessellation
            renderAssets.mbPixelLightingDepth.begin(cameraLight);
            modelRenderer.render(candidate, renderAssets.mbPixelLightingDepth, camera, 1, 0, null, RenderGroup.MODEL_PIX, true);
            renderAssets.mbPixelLightingDepth.end();

            // Save frame buffer and combined matrix
            scaffolding.shadow = shadowNRender;
            shadowMapCombined[i][j].set(cameraLight.combined);
            smCombinedMap.put(candidate, shadowMapCombined[i][j]);
            smTexMap.put(candidate, shadowMapFb[i][j].getColorBufferTexture());

            shadowMapFb[i][j].end();
            i++;
        }
    }

    private void addCandidates(List<IRenderable> models, List<Entity> candidates) {
        if (candidates != null) {
            candidates.clear();
            int num = 0;
            for (IRenderable model : models) {
                Render render = (Render) model;
                var scaffolding = Mapper.modelScaffolding.get(render.entity);
                if (scaffolding != null) {
                    if (scaffolding.isShadow()) {
                        candidates.add(num, render.entity);
                        scaffolding.shadow = 0;
                        num++;
                        if (num == Settings.settings.scene.renderer.shadow.number)
                            break;
                    }
                }
            }
        }
    }

    @SuppressWarnings("unused")
    private void renderShadowMapCandidatesTess(Array<Entity> candidates, int shadowNRender, ICamera camera, RenderingContext rc) {
        int i = 0;
        int j = 0;
        var renderAssets = sceneRenderer.getRenderAssets();
        // Normal bodies
        for (Entity candidate : candidates) {
            var body = Mapper.body.get(candidate);
            var model = Mapper.model.get(candidate);
            var scaffolding = Mapper.modelScaffolding.get(candidate);

            double radius = (body.size / 2.0) * scaffolding.sizeScaleFactor;
            // Only render when camera very close to surface
            if (body.distToCamera < radius * 1.1) {
                scaffolding.shadow = shadowNRender;

                Vector3 shadowCameraDir = aux1.set(model.model.directional(0).direction);

                // Shadow camera direction is that of the light
                cameraLight.direction.set(shadowCameraDir);

                Vector3 shadowCamDir = aux1.set(model.model.directional(0).direction);
                // Direction is that of the light
                cameraLight.direction.set(shadowCamDir);

                // Distance from camera to object, radius * sv[0]
                float distance = (float) (radius * scaffolding.shadowMapValues[0] * 0.01);
                // Position, factor of radius
                Vector3b objPos = EntityUtils.getAbsolutePosition(candidate, aux1b);
                Vector3b camPos = camera.getPos();
                Vector3d camDir = aux3d.set(camera.getDirection()).nor().scl(100 * Constants.KM_TO_U);
                boolean intersect = IntersectorDouble.checkIntersectSegmentSphere(camPos.tov3d(), aux3d.set(camPos).add(camDir), objPos.put(aux1d), radius);
                if (intersect) {
                    // Use height
                    camDir.nor().scl(body.distToCamera - radius);
                }
                Vector3d objCam = aux2d.set(camPos).sub(objPos).nor().scl(-(body.distToCamera - radius)).add(camDir);

                objCam.add(shadowCamDir.nor().scl(-distance));
                objCam.put(cameraLight.position);

                // Shadow camera up is perpendicular to dir
                if (cameraLight.direction.y != 0 || cameraLight.direction.z != 0)
                    aux1.set(1, 0, 0);
                else
                    aux1.set(0, 1, 0);
                cameraLight.up.set(cameraLight.direction).crs(aux1);

                // Near is sv[1]*radius before the object
                cameraLight.near = distance * 0.98f;
                // Far is sv[2]*radius after the object
                cameraLight.far = distance * 1.02f;

                // Update cam
                cameraLight.update(false);

                // Render model depth map to frame buffer
                shadowMapFb[i][j].begin();
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

                // Tessellation
                renderAssets.mbPixelLightingDepthTessellation.begin(cameraLight);
                modelRenderer.render(candidate, renderAssets.mbPixelLightingDepthTessellation, camera, 1, 0, rc, RenderGroup.MODEL_PIX, true);
                renderAssets.mbPixelLightingDepthTessellation.end();

                // Save frame buffer and combined matrix
                scaffolding.shadow = shadowNRender;
                shadowMapCombined[i][j].set(cameraLight.combined);
                smCombinedMap.put(candidate, shadowMapCombined[i][j]);
                smTexMap.put(candidate, shadowMapFb[i][j].getColorBufferTexture());

                shadowMapFb[i][j].end();
                i++;
            } else {
                scaffolding.shadow = -1;
            }
        }
    }

    public void render(ICamera camera) {
        /*
         * Shadow mapping here?
         * <ul>
         * <li>Extract model bodies (front)</li>
         * <li>Work out light direction</li>
         * <li>Set orthographic camera at set distance from bodies,
         * direction of light, clip planes</li>
         * <li>Render depth map to frame buffer (fb)</li>
         * <li>Send frame buffer texture in to ModelBatchRenderSystem along
         * with light position, direction, clip planes and light camera
         * combined matrix</li>
         * <li>Compare real distance from light to texture sample, render
         * shadow if different</li>
         * </ul>
         */
        List<IRenderable> models = sceneRenderer.getRenderLists().get(MODEL_PIX.ordinal());
        //List<IRenderable> modelsTess = renderLists.get(MODEL_PIX_TESS.ordinal());
        models.sort(Comparator.comparingDouble(IRenderable::getDistToCamera));

        final int shadowNRender = (Settings.settings.program.modeStereo.active || Settings.settings.runtime.openVr) ? 2 : Settings.settings.program.modeCubemap.active ? 6 : 1;

        if (shadowMapFb != null && smCombinedMap != null) {
            addCandidates(models, shadowCandidates);
            //addCandidates(modelsTess, shadowCandidatesTess);

            // Clear maps
            smTexMap.clear();
            smCombinedMap.clear();

            renderShadowMapCandidates(shadowCandidates, shadowNRender, camera);
            //renderShadowMapCandidatesTess(shadowCandidatesTess, shadowNRender, camera);
        }
    }
}
