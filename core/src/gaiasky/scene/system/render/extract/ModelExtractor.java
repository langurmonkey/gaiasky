package gaiasky.scene.system.render.extract;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.render.ComponentTypes;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.*;
import gaiasky.util.Settings;
import gaiasky.util.math.MathUtilsDouble;
import net.jafama.FastMath;

/**
 * Extracts model object data to feed to the render stage.
 */
public class ModelExtractor extends AbstractExtractSystem {

    protected static double BBGAL_TH = Math.toRadians(0.9);

    public ModelExtractor(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        extractEntity(entity);
    }

    public void extractEntity(Entity entity) {
        addToRenderLists(entity, camera);
    }

    protected void addToRenderLists(Entity entity, ICamera camera) {
        var base = Mapper.base.get(entity);
        var graph = Mapper.graph.get(entity);
        var coord = Mapper.coordinates.get(entity);
        Coordinates parentCoord = Mapper.coordinates.get(graph.parent);

        if (mustRender(base) && isValidPosition(coord) && isValidPosition(parentCoord)) {
            var body = Mapper.body.get(entity);
            var model = Mapper.model.get(entity);
            var sa = Mapper.sa.get(entity);
            var scaffolding = Mapper.modelScaffolding.get(entity);
            var atmosphere = Mapper.atmosphere.get(entity);
            var cloud = Mapper.cloud.get(entity);
            var render = Mapper.render.get(entity);

            camera.checkClosestBody(entity);

            if (Mapper.tagBillboardGalaxy.has(entity)) {
                // Billboard galaxies.
                double thPoint = (BBGAL_TH * camera.getFovFactor()) / scaffolding.sizeScaleFactor;
                if (body.solidAngleApparent >= thPoint) {
                    addToRender(render, RenderGroup.MODEL_DIFFUSE);
                } else if (base.opacity > 0) {
                    addToRender(render, RenderGroup.BILLBOARD_GAL);
                }

                if (renderText(base, body, sa)) {
                    addToRender(render, RenderGroup.FONT_LABEL);
                }
            } else if (Mapper.tagQuatOrientation.has(entity)) {
                // Simple billboards.
                if (body.solidAngleApparent >= sa.thresholdNone) {
                    addToRender(render, RenderGroup.MODEL_DIFFUSE);
                    addToRender(render, RenderGroup.FONT_LABEL);
                }
            } else {
                // Rest of models.
                double thPoint = (sa.thresholdPoint * camera.getFovFactor()) / scaffolding.sizeScaleFactor;
                if (body.solidAngleApparent >= thPoint) {
                    double thQuad2 = sa.thresholdQuad * camera.getFovFactor() * 2 / scaffolding.sizeScaleFactor;
                    double thQuad1 = thQuad2 / 8.0 / scaffolding.sizeScaleFactor;
                    if (body.solidAngleApparent < thPoint * 4) {
                        scaffolding.fadeOpacity = (float) MathUtilsDouble.lint(body.solidAngleApparent, thPoint, thPoint * 4, 1, 0);
                    } else {
                        scaffolding.fadeOpacity = (float) MathUtilsDouble.lint(body.solidAngleApparent, thQuad1, thQuad2, 0, 1);
                    }

                    if (body.solidAngleApparent < thQuad1) {
                        addToRender(render, RenderGroup.BILLBOARD_SSO);
                    } else if (body.solidAngleApparent > thQuad2) {
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
    }

    private void addToRenderModel(Render render, Model model) {
        RenderGroup rg;
        var rt = Mapper.renderType.get(render.entity);
        if (rt != null && rt.renderGroup != null) {
            rg = rt.renderGroup;
        } else {
            rg = renderTessellated(model) ? RenderGroup.MODEL_PIX_TESS : RenderGroup.MODEL_PIX;
        }
        addToRender(render, rg);
    }

    private boolean renderTessellated(Model model) {
        return Settings.settings.scene.renderer.elevation.type.isTessellation() && model.model.hasHeight();
    }

    private boolean renderText(Base base, Body body, SolidAngle sa) {
        return base.names != null
                && renderer.isOn(ComponentTypes.ComponentType.Labels)
                && (base.forceLabel || FastMath.pow(body.solidAngleApparent, getViewAnglePow()) >= sa.thresholdLabel);
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
