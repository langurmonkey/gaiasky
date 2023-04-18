package gaiasky.scene.system.render.extract;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.entity.EntityUtils;
import gaiasky.scene.entity.TrajectoryUtils;
import gaiasky.util.Settings;
import gaiasky.util.math.MathUtilsDouble;

/**
 * Extracts trajectory and orbit data to feed to the render stages.
 */
public class TrajectoryExtractor extends AbstractExtractSystem {

    /**
     * Special overlap factor
     */
    protected static final float SHADER_MODEL_OVERLAP_FACTOR = 20f;

    private final TrajectoryUtils utils;

    public TrajectoryExtractor(Family family, int priority) {
        super(family, priority);
        this.utils = new TrajectoryUtils();
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

        if (this.mustRender(base)) {
            var trajectory = Mapper.trajectory.get(entity);
            var body = Mapper.body.get(entity);
            var render = Mapper.render.get(entity);
            var label = Mapper.label.get(entity);

            if (!trajectory.onlyBody) {
                // If there is overflow, return.
                if (trajectory.body != null && EntityUtils.isCoordinatesTimeOverflow(trajectory.body))
                    return;

                boolean added = false;
                float angleLimit = (float) Settings.settings.scene.renderer.orbitSolidAngleThreshold * camera.getFovFactor();
                if (body.solidAngle > angleLimit) {
                    // Fade the orbit using its solid angle and the threshold in the settings.
                    if (body.solidAngle < angleLimit * SHADER_MODEL_OVERLAP_FACTOR) {
                        trajectory.alpha = MathUtilsDouble.lint(body.solidAngle, angleLimit, angleLimit * SHADER_MODEL_OVERLAP_FACTOR, 0, body.color[3]);
                    } else {
                        trajectory.alpha = body.color[3];
                    }

                    RenderGroup rg = Settings.settings.scene.renderer.isQuadLineRenderer() ? RenderGroup.LINE : RenderGroup.LINE_GPU;

                    if (trajectory.body == null) {
                        // There is no body, always render.
                        addToRender(render, rg);
                        added = true;
                    } else {
                        var bodyBody = Mapper.body.get(trajectory.body);
                        // For orbits with a body objects, we fade it out as we move closer to the body, using the distUp and distDown, which are in
                        // body radius units.
                        if (bodyBody.distToCamera > trajectory.distDown) {
                            if (bodyBody.distToCamera < trajectory.distUp)
                                trajectory.alpha *= MathUtilsDouble.lint(bodyBody.distToCamera, trajectory.distDown / camera.getFovFactor(), trajectory.distUp / camera.getFovFactor(), 0, 1);
                            addToRender(render, rg);
                            added = true;
                        }
                    }
                }

                var verts = Mapper.verts.get(entity);
                if (verts.pointCloudData == null || added) {
                    utils.refreshOrbit(trajectory, verts, false);
                }
            }
            // Orbital element renderer.
            if (trajectory.body == null && !trajectory.isInOrbitalElementsGroup && base.ct.get(ComponentType.Asteroids.ordinal()) && renderer.isOn(ComponentType.Asteroids)) {
                addToRender(render, RenderGroup.ORBITAL_ELEMENTS_PARTICLE);
            }
            if (label.forceLabel) {
                addToRender(render, RenderGroup.FONT_LABEL);
            }
        }

    }
}
