package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import gaiasky.GaiaSky;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Base;
import gaiasky.scene.component.GraphNode;
import gaiasky.util.time.ITimeFrameProvider;

public class ElementsSetUpdater extends IteratingSystem implements EntityUpdater {

    private final GraphUpdater graphUpdater;
    private final TrajectoryUpdater trajectoryUpdater;
    private final ModelUpdater modelUpdater;

    public ElementsSetUpdater(Family family, int priority) {
        super(family, priority);

        this.graphUpdater = new GraphUpdater(null, 0, GaiaSky.instance.time);
        this.trajectoryUpdater = new TrajectoryUpdater(null, 0);
        this.modelUpdater = new ModelUpdater(null, 0);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        var time = GaiaSky.instance.time;
        var graph = Mapper.graph.get(entity);
        if (graph.children != null) {
            var base = Mapper.base.get(entity);
            var set = Mapper.orbitElementsSet.get(entity);

            graphUpdater.setCamera(GaiaSky.instance.cameraManager);
            if (set.initialUpdate) {
                // Update all
                for (int i = 0; i < graph.children.size; i++) {
                    Entity child = graph.children.get(i);
                    update(child, deltaTime, time, graph, base);
                }
                set.initialUpdate = false;
            } else {
                // Update needed
                for (int i = 0; i < set.alwaysUpdate.size; i++) {
                    Entity child = set.alwaysUpdate.get(i);
                    update(child, deltaTime, time, graph, base);
                }
            }
        }
    }

    private void update(Entity entity, float deltaTime, ITimeFrameProvider time, GraphNode parentGraph, Base parentBase) {
        graphUpdater.update(entity, time, parentGraph.translation, parentBase.opacity);
        if (Mapper.trajectory.has(entity)) {
            trajectoryUpdater.updateEntity(entity, deltaTime);
        } else if (Mapper.model.has(entity)) {
            modelUpdater.updateEntity(entity, deltaTime);
        }
    }
}
