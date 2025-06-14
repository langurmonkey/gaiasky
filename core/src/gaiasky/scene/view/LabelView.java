/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.view;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector3;
import gaiasky.GaiaSky;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.RenderingContext;
import gaiasky.render.RenderingContext.CubemapSide;
import gaiasky.render.api.I3DTextRenderable;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.*;
import gaiasky.scene.system.render.draw.TextRenderer;
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;
import gaiasky.util.Constants;
import gaiasky.util.DecalUtils;
import gaiasky.util.GlobalResources;
import gaiasky.util.Settings;
import gaiasky.util.gdx.g2d.BitmapFont;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import gaiasky.util.gravwaves.RelativisticEffectsManager;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector3D;
import net.jafama.FastMath;

public class LabelView extends RenderView implements I3DTextRenderable {

    private final Vector3D D31 = new Vector3D();
    private final Vector3D D32 = new Vector3D();
    private final Vector3 F31 = new Vector3();
    private final Vector3 F32 = new Vector3();

    public Label label;
    public GraphNode graph;
    public SolidAngle sa;
    public Cluster cluster;
    public BillboardSet bbSet;
    public Constel constel;
    public Mesh mesh;
    public Ruler ruler;
    public LocationMark loc;

    private final LabelEntityRenderSystem renderSystem;

    public LabelView() {
        renderSystem = new LabelEntityRenderSystem();
    }

    @Override
    protected void entityCheck(Entity entity) {
        super.entityCheck(entity);
        check(entity, Mapper.graph, GraphNode.class);
    }

    @Override
    protected void entityChanged() {
        super.entityChanged();
        this.label = Mapper.label.get(entity);
        this.graph = Mapper.graph.get(entity);
        this.sa = Mapper.sa.get(entity);
        this.cluster = Mapper.cluster.get(entity);
        this.bbSet = Mapper.billboardSet.get(entity);
        this.constel = Mapper.constel.get(entity);
        this.mesh = Mapper.mesh.get(entity);
        this.ruler = Mapper.ruler.get(entity);
        this.loc = Mapper.loc.get(entity);
    }

    @Override
    protected void entityCleared() {
        this.label = null;
        this.graph = null;
        this.sa = null;
        this.cluster = null;
        this.bbSet = null;
        this.constel = null;
        this.mesh = null;
        this.ruler = null;
    }

    public boolean renderTextCelestial() {
        return base.names != null
                && renderTextBase()
                && (label.forceLabel || FastMath.pow(body.solidAngleApparent, label.solidAnglePow) >= sa.thresholdLabel / label.labelBias);
    }

    public boolean renderTextParticle() {
        return extra.computedSize > 0 &&
                renderTextBase() &&
                body.solidAngleApparent >= (sa.thresholdLabel / (label.labelBias * GaiaSky.instance.cameraManager.getFovFactor()));
    }

    public boolean renderTextLocation() {
        if (renderTextBase()
                && (
                        (body.solidAngle >= LocationMark.LOWER_LIMIT
                            && (body.solidAngle <= LocationMark.UPPER_LIMIT || loc.ignoreSolidAngleLimit))
                        || label.forceLabel)
                ){
            Vector3D aux = D31;
            graph.translation.put(aux).sub(body.pos).scl(-1);
            // Make sure we don't render locations if the normal at the point points away from the camera.
            return aux.add(loc.location3d).nor().dot(GaiaSky.instance.cameraManager.getDirection().nor()) < -0.3;
        } else {
            return false;
        }
    }

    public boolean renderTextKeyframe() {
        var kf = Mapper.keyframes.get(entity);
        return kf.selected != null;
    }

    public boolean renderTextRuler() {
        return renderTextBase() && ruler.rulerOk;
    }

    public boolean renderTextBackgroundModel() {
        return renderTextBase() && label.label;
    }

    public boolean renderTextTitle() {
        return !Settings.settings.program.modeCubemap.active;
    }

    public boolean renderTextTrajectory() {
        return renderTextBase() && label.forceLabel;
    }

    public boolean renderTextGridRec() {
        return label.label;
    }

    public boolean renderTextEssential() {
        return base.names != null && renderTextBase() && base.opacity > 0;
    }

    public boolean renderTextBase() {
        return GaiaSky.instance.isOn(ComponentType.Labels);
    }

    @Override
    public boolean renderText() {
        return label.renderFunction == null || label.renderFunction.apply(this);
    }

    @Override
    public void render(ExtSpriteBatch batch, ExtShaderProgram shader, TextRenderer sys, RenderingContext rc, ICamera camera) {
        if (label.renderConsumer != null && renderText()) {
            label.renderConsumer.apply(renderSystem, this, batch, shader, sys, rc, camera);
        }
    }

    @Override
    public float textSize() {
        if (constel != null) {
            return .2e7f * (float) Constants.DISTANCE_SCALE_FACTOR;
        } else if (loc != null) {
            return body.size * label.labelFactor;
        } else if (Mapper.keyframes.has(entity)) {
            return label.labelMax * (float) Constants.DISTANCE_SCALE_FACTOR;
        }
        return (float) (label.labelMax * body.distToCamera * label.labelFactor);
    }

    @Override
    public float textScale() {
        if (starSet != null || particleSet != null) {
            // Star and particle sets
            return label.textScale / Settings.settings.scene.label.size;
        } else if (constel != null) {
            return .2f / Settings.settings.scene.label.size;
        } else if (loc != null) {
            return loc.sizeKm * label.textScale / textSize() * (float) Constants.DISTANCE_SCALE_FACTOR;
        } else {
            // Rest
            return label.textScale >= 0 ? label.textScale : (float) FastMath.atan(label.labelMax) * label.labelFactor * 4e2f;
        }
    }

    private final double rad02 = FastMath.toRadians(2);
    private final double rad40 = FastMath.toRadians(40);

    /**
     * Text position for single objects (models, single stars, etc.).
     *
     * @param cam The camera.
     * @param out The out parameter with the result.
     */
    @Override
    public void textPosition(ICamera cam, Vector3D out) {
        if (label != null && label.labelPosition != null) {
            out.set(label.labelPosition).add(cam.getInversePos());
        } else {
            if (ruler == null) {
                graph.translation.put(out);
            } else {
                out.set(ruler.m);
            }
            double distToCamera = out.len();
            out.clamp(0, distToCamera - getRadius()).scl(0.9f);

            // Offset label a bit to the bottom-right of the position.
            Vector3D offset = D32;
            offset.set(cam.getUp());
            offset.crs(out).nor();

            float displacement = (float) MathUtilsDouble.flint(body.solidAngleApparent, rad02, rad40, 1, 20);
            float dist = -0.015f * displacement * cam.getFovFactor() * (float) out.len();
            offset.add(cam.getUp()).nor().scl(dist);

            out.add(offset);
        }

        GlobalResources.applyRelativisticAberration(out, cam);
        RelativisticEffectsManager.getInstance().gravitationalWavePos(out);
    }

    @Override
    public String text() {
        if (ruler != null) {
            return ruler.dist;
        } else if (loc != null) {
            return loc.displayName;
        } else {
            return base.getLocalizedName();
        }
    }

    @Override
    public void textDepthBuffer() {
        label.depthBufferConsumer.accept(this);
    }

    public void defaultTextDepthBuffer() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(false);
    }

    public void noTextDepthBuffer() {
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthMask(false);
    }

    public void emptyTextDepthBuffer() {
    }

    @Override
    public boolean isLabel() {
        return label == null || label.label;
    }

    public boolean isLocation() {
        return loc != null;
    }

    @Override
    public float getTextOpacity() {
        return base.opacity;
    }

    protected void render2DLabel(ExtSpriteBatch batch, ExtShaderProgram shader, RenderingContext rc, BitmapFont font, ICamera camera, String label, Vector3D pos3d) {
        Vector3 p = F31;
        pos3d.setVector3(p);

        camera.getCamera().project(p);
        p.x += 15;
        p.y -= 15;

        shader.setUniformf("scale", 1f);
        DecalUtils.drawFont2D(font, batch, label, p);
    }

    protected void render2DLabel(ExtSpriteBatch batch, ExtShaderProgram shader, RenderingContext rc, BitmapFont font, ICamera camera, String label, float x, float y, float scale, int align) {
        shader.setUniformf("u_scale", scale);
        DecalUtils.drawFont2D(font, batch, rc, label, x, y, scale, align);
    }

    protected void render2DLabel(ExtSpriteBatch batch, ExtShaderProgram shader, RenderingContext rc, BitmapFont font, ICamera camera, String label, float x, float y) {
        render2DLabel(batch, shader, rc, font, camera, label, x, y, 1f);
    }

    protected void render2DLabel(ExtSpriteBatch batch, ExtShaderProgram shader, RenderingContext rc, BitmapFont font, ICamera camera, String label, float x, float y, float scale) {
        render2DLabel(batch, shader, rc, font, camera, label, x, y, scale, -1);
    }

    protected void render3DLabel(ExtSpriteBatch batch, ExtShaderProgram shader, BitmapFont font, ICamera camera, RenderingContext rc, String label, Vector3D pos, double distToCamera, float scale, double size, double radius, boolean forceLabel) {
        render3DLabel(batch, shader, font, camera, rc, label, pos, distToCamera, scale, size, radius, -1, -1, forceLabel);
    }

    protected void render3DLabel(ExtSpriteBatch batch, ExtShaderProgram shader, BitmapFont font, ICamera camera, RenderingContext rc, String label, Vector3D pos, double distToCamera, float scale, double size, double radius, float minSizeDegrees, float maxSizeDegrees, boolean forceLabel) {
        // The smoothing scale must be set according to the distance
        shader.setUniformf("u_scale", Settings.settings.scene.label.size * scale / camera.getFovFactor());

        if (forceLabel || radius == 0 || distToCamera > radius * 2d) {

            size *= Settings.settings.scene.label.size;

            float rot = 0;
            if (rc.cubemapSide == CubemapSide.SIDE_UP || rc.cubemapSide == CubemapSide.SIDE_DOWN) {
                Vector3 v1 = F31;
                Vector3 v2 = F32;
                camera.getCamera().project(v1.set((float) pos.x, (float) pos.y, (float) pos.z));
                v1.z = 0f;
                v2.set(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f, 0f);
                rot = GlobalResources.angle2d(v1, v2) + (rc.cubemapSide == CubemapSide.SIDE_UP ? 90f : -90f);
            }

            shader.setUniformf("u_pos", pos);

            // Enable or disable blending
            ((I3DTextRenderable) this).textDepthBuffer();

            DecalUtils.drawFont3D(font, batch, label, (float) pos.x, (float) pos.y, (float) pos.z, size, rot, camera, !rc.isCubemap(), minSizeDegrees, maxSizeDegrees);
        }
    }
}
