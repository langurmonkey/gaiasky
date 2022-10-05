package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.math.Vector3;
import gaiasky.scene.Mapper;
import gaiasky.util.math.MathUtilsd;

/**
 * Updates star clusters.
 */
public class ClusterUpdater extends AbstractUpdateSystem {

    public static final double TH_ANGLE = Math.toRadians(0.5);
    public static final double TH_ANGLE_OVERLAP = Math.toRadians(0.7);

    private final Vector3 F31;

    public ClusterUpdater(Family family, int priority) {
        super(family, priority);
        F31 = new Vector3();
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        var base = Mapper.base.get(entity);
        if (base.opacity > 0) {
            var body = Mapper.body.get(entity);
            var graph = Mapper.graph.get(entity);
            var cluster = Mapper.cluster.get(entity);

            base.opacity *= 0.1f * base.getVisibilityOpacityFactor();
            cluster.fadeAlpha = (float) MathUtilsd.lint(body.solidAngleApparent, TH_ANGLE, TH_ANGLE_OVERLAP, 0f, 1f);
            body.labelColor[3] = 8.0f * cluster.fadeAlpha;

            graph.localTransform.idt().translate(graph.translation.put(F31)).scl(body.size);
        }
    }
}
