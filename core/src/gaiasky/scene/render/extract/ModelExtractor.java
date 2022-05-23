package gaiasky.scene.render.extract;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.GaiaSky;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.*;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Settings;
import gaiasky.util.math.MathUtilsd;
import net.jafama.FastMath;

/**
 * Extracts model object data to feed to the render stage.
 */
public class ModelExtractor extends AbstractExtractSystem {

    public ModelExtractor(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        addToRenderLists(entity, camera);
    }

    protected void addToRenderLists(Entity entity, ICamera camera) {
        var base = Mapper.base.get(entity);
        var graph = Mapper.graph.get(entity);
        var coord = Mapper.coordinates.get(entity);
        Coordinates parentCoord = Mapper.coordinates.get(graph.parent);

        if (shouldRender(base) && isValidPosition(coord) && isValidPosition(parentCoord)) {
            var body = Mapper.body.get(entity);
            var model = Mapper.model.get(entity);
            var sa = Mapper.sa.get(entity);
            var scaffolding = Mapper.modelScaffolding.get(entity);
            var atmosphere = Mapper.atmosphere.get(entity);
            var cloud = Mapper.cloud.get(entity);
            var render = Mapper.render.get(entity);
            // TODO restore
            // camera.checkClosestBody(this);
            double thPoint = (sa.thresholdPoint * camera.getFovFactor()) / scaffolding.sizeScaleFactor;
            if (body.viewAngleApparent >= thPoint) {
                double thQuad2 = sa.thresholdQuad * camera.getFovFactor() * 2 / scaffolding.sizeScaleFactor;
                double thQuad1 = thQuad2 / 8.0 / scaffolding.sizeScaleFactor;
                if (body.viewAngleApparent < thPoint * 4) {
                    scaffolding.fadeOpacity = (float) MathUtilsd.lint(body.viewAngleApparent, thPoint, thPoint * 4, 1, 0);
                } else {
                    scaffolding.fadeOpacity = (float) MathUtilsd.lint(body.viewAngleApparent, thQuad1, thQuad2, 0, 1);
                }

                if (body.viewAngleApparent < thQuad1) {
                    addToRender(render, RenderGroup.BILLBOARD_SSO);
                } else if (body.viewAngleApparent > thQuad2) {
                    addToRenderModel(render, model);
                } else {
                    // Both
                    addToRender(render, RenderGroup.BILLBOARD_SSO);
                    addToRenderModel(render, model);
                }
                if (renderText(base, body, sa)) {
                    addToRender(render, RenderGroup.FONT_LABEL);
                }
            }
            if (!isInRender(render, RenderGroup.FONT_LABEL) && base.forceLabel) {
                addToRender(render, RenderGroup.FONT_LABEL);
            }


            // Atmosphere (only planets)
            if (atmosphere != null &&
                    atmosphere.atmosphere != null &&
                    isInRender(render, RenderGroup.MODEL_PIX, RenderGroup.MODEL_PIX_TESS) &&
                    !coord.timeOverflow) {
                addToRender(render, RenderGroup.MODEL_ATM);
            }

            // Cloud (only planets)
            if (cloud != null &&
                    cloud.cloud != null &&
                    isInRender(render, RenderGroup.MODEL_PIX, RenderGroup.MODEL_PIX_TESS) &&
                    !coord.timeOverflow) {
                addToRender(render, RenderGroup.MODEL_CLOUD);
            }
        }
    }

    private void addToRenderModel(Render render, Model model) {
        RenderGroup rg = renderTessellated(model) ? RenderGroup.MODEL_PIX_TESS : RenderGroup.MODEL_PIX;
        addToRender(render, rg);
    }

    private boolean renderTessellated(Model model) {
        return Settings.settings.scene.renderer.elevation.type.isTessellation() && model.model.hasHeight();
    }

    private boolean renderText(Base base, Body body, SolidAngle sa) {
        double thOverFactor = sa.thresholdPoint / Settings.settings.scene.label.number;
        return base.names != null
                && GaiaSky.instance.isOn(ComponentTypes.ComponentType.Labels)
                && (base.forceLabel || FastMath.pow(body.viewAngleApparent, getViewAnglePow()) >= sa.thresholdLabel);
    }

    private float getThOverFactorScl(Base base) {
        return base.ct.get(ComponentTypes.ComponentType.Moons.ordinal()) ? 2500f : 25f;
    }

    private float getViewAnglePow() {
        return 1f;
    }

    private boolean isValidPosition(Coordinates coord) {
        return coord == null || !coord.timeOverflow;
    }
}
