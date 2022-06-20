package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import gaiasky.scene.Mapper;
import gaiasky.util.math.MathUtilsd;

/**
 * Updates star clusters.
 */
public class ClusterUpdater extends IteratingSystem implements EntityUpdater {

    public static final double TH_ANGLE = Math.toRadians(0.5);
    public static final double TH_ANGLE_OVERLAP = Math.toRadians(0.7);

    public ClusterUpdater(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        var base = Mapper.base.get(entity);
        var body = Mapper.body.get(entity);
        var cluster = Mapper.cluster.get(entity);

        base.opacity *= 0.1f * base.getVisibilityOpacityFactor();
        cluster.fadeAlpha = (float) MathUtilsd.lint(body.viewAngleApparent, TH_ANGLE, TH_ANGLE_OVERLAP, 0f, 1f);
    }
}
