package gaiasky.scene.render.extract;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.GaiaSky;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;
import gaiasky.scene.entity.EntityUtils;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Settings;
import gaiasky.util.math.MathUtilsd;

/**
 * Extracts trajectory and orbit data to feed to the render stages.
 */
public class TrajectoryExtractor extends AbstractExtractSystem {

    /**
     * Threshold solid angle for trajectories.
     **/
    protected static float SOLID_ANGLE_THRESHOLD = (float) Math.toRadians(1.5);
    public static void setSolidAngleThreshold(float angleDeg) {
        SOLID_ANGLE_THRESHOLD = (float) Math.toRadians(angleDeg);
    }

    /**
     * Special overlap factor
     */
    protected static final float SHADER_MODEL_OVERLAP_FACTOR = 20f;

    public TrajectoryExtractor(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        addToRenderLists(entity, camera);
    }

    protected void addToRenderLists(Entity entity, ICamera camera) {
        var base = Mapper.base.get(entity);

        if (this.shouldRender(base)) {
            var trajectory = Mapper.trajectory.get(entity);
            var body = Mapper.body.get(entity);
            var render = Mapper.render.get(entity);

            if (!trajectory.onlyBody) {
                // If there is overflow, return
                if (trajectory.body != null && EntityUtils.isCoordinatesTimeOverflow(trajectory.body))
                    return;

                boolean added = false;
                float angleLimit = SOLID_ANGLE_THRESHOLD * camera.getFovFactor();
                if (body.viewAngle > angleLimit) {
                    if (body.viewAngle < angleLimit * SHADER_MODEL_OVERLAP_FACTOR) {
                        trajectory.alpha = MathUtilsd.lint(body.viewAngle, angleLimit, angleLimit * SHADER_MODEL_OVERLAP_FACTOR, 0, body.color[3]);
                    } else {
                        trajectory.alpha = body.color[3];
                    }

                    RenderGroup rg = Settings.settings.scene.renderer.isQuadLineRenderer() ? RenderGroup.LINE : RenderGroup.LINE_GPU;

                    if (body == null) {
                        // There is no body, always render
                        addToRender(render, rg);
                        added = true;
                    } else if (body.distToCamera > trajectory.distDown) {
                        // Body, disappear slowly
                        if (body.distToCamera < trajectory.distUp)
                            trajectory.alpha *= MathUtilsd.lint(body.distToCamera, trajectory.distDown / camera.getFovFactor(), trajectory.distUp / camera.getFovFactor(), 0, 1);
                        addToRender(render, rg);
                        added = true;
                    }
                }

                var verts = Mapper.verts.get(entity);
                if (verts.pointCloudData == null || added) {
                    trajectory.refreshOrbit(verts, false);
                }
            }
            // Orbital elements renderer
            if (body == null && !trajectory.isInOrbitalElementsGroup && base.ct.get(ComponentType.Asteroids.ordinal()) && GaiaSky.instance.isOn(ComponentType.Asteroids)) {
                addToRender(render, RenderGroup.ORBITAL_ELEMENTS_PARTICLE);
            }
            if (base.forceLabel) {
                addToRender(render, RenderGroup.FONT_LABEL);
            }
        }

    }
}
