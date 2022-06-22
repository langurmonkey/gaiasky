package gaiasky.scene.render.extract;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.GaiaSky;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.*;
import gaiasky.scenegraph.camera.FovCamera;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Settings;

/**
 * Extracts single particle and star data to feed to the render stages.
 */
public class ParticleExtractor extends AbstractExtractSystem {


    public ParticleExtractor(Family family, int priority) {
        super(family, priority);
    }


    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        addToRenderLists(entity, camera);
    }

    private void addToRenderLists(Entity entity, ICamera camera) {
        var base = Mapper.base.get(entity);
        //camera.checkClosestParticle(this);

        if (this.shouldRender(base)) {
            var body = Mapper.body.get(entity);
            var render = Mapper.render.get(entity);
            var renderType = Mapper.renderType.get(entity);
            var hip = Mapper.hip.get(entity);

            addToRender(render, RenderGroup.POINT_STAR);

            if (hip == null) {
                addToRenderParticle(camera, entity, body, render, renderType);
            } else {
                addToRenderStar(camera, entity, body, render, renderType);
            }
        }
    }

    private void addToRenderParticle(ICamera camera, Entity entity, Body body, Render render, RenderType renderType) {
        if (!(camera.getCurrent() instanceof FovCamera)) {
            addToRender(render, renderType.renderGroup);

            boolean hasPm = Mapper.pm.has(entity) && Mapper.pm.get(entity).hasPm;
            if (body.viewAngleApparent >= Settings.settings.scene.star.threshold.point / Settings.settings.scene.properMotion.number && hasPm) {
                addToRender(render, RenderGroup.LINE);
            }
        }
        if (renderText(body, Mapper.sa.get(entity), Mapper.extra.get(entity))) {
            addToRender(render, RenderGroup.FONT_LABEL);
        }
    }

    private void addToRenderStar(ICamera camera, Entity entity, Body body, Render render, RenderType renderType) {
        if (camera.getCurrent() instanceof FovCamera) {
            // Render as point, do nothing
            addToRender(render, RenderGroup.BILLBOARD_STAR);
        } else {
            if (body.viewAngleApparent >= Settings.settings.scene.star.threshold.point) {
                addToRender(render, RenderGroup.BILLBOARD_STAR);
                if (body.distToCamera < Mapper.distance.get(entity).distance) {
                    //camera.checkClosestBody(this);
                    addToRender(render, RenderGroup.MODEL_VERT_STAR);
                }
            }
            boolean hasPm = Mapper.pm.has(entity) && Mapper.pm.get(entity).hasPm;
            if (hasPm && body.viewAngleApparent >= Settings.settings.scene.star.threshold.point / Settings.settings.scene.properMotion.number) {
                addToRender(render, RenderGroup.LINE);
            }
        }

        if ((renderText(body, Mapper.sa.get(entity), Mapper.extra.get(entity)) || camera.getCurrent() instanceof FovCamera)) {
            addToRender(render, RenderGroup.FONT_LABEL);
        }

    }

    private boolean renderText(Body body, SolidAngle sa, ParticleExtra particleExtra) {
        return particleExtra.computedSize > 0 &&
                GaiaSky.instance.isOn(ComponentType.Labels) &&
                body.viewAngleApparent >= (sa.thresholdLabel / GaiaSky.instance.cameraManager.getFovFactor());
    }
}
