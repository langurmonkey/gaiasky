package gaiasky.scene.system;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;

/**
 * A simple EntitySystem that iterates over each entity in parallel and calls processEntity() for each entity every time the EntitySystem is
 * updated. The main difference with {@link IteratingSystem} is that this system processes the entities in the
 * family in parallel. There is an overhead each time the system is called to copy the list of entities to
 * a Java streams-ready collection for parallel streaming.
 *
 * @author Toni Sagrista
 */
public abstract class ParallelSystem extends EntitySystem {
    private Family family;
    private ImmutableArray<Entity> entities;

    private final ForkJoinPool forkJoinPool;

    private List<Callable<Integer>> runnableTasks;

    /**
     * Instantiates a system that will iterate over the entities described by the Family.
     *
     * @param family The family of entities iterated over in this System
     */
    public ParallelSystem(Family family) {
        this(family, 0);
    }

    /**
     * Instantiates a system that will iterate over the entities described by the Family, with a specific priority.
     *
     * @param family   The family of entities iterated over in this System
     * @param priority The priority to execute this system with (lower means higher priority)
     */
    public ParallelSystem(Family family, int priority) {
        super(priority);
        this.family = family;

        forkJoinPool = ForkJoinPool.commonPool();
    }

    @Override
    public void addedToEngine(Engine engine) {
        entities = engine.getEntitiesFor(family);
        runnableTasks = new ArrayList<>(entities.size());
        for (Entity e : entities) {
            runnableTasks.add(() -> {
                this.processEntity(e, 0f);
                return 0;
            });
        }
    }

    @Override
    public void removedFromEngine(Engine engine) {
        entities = null;
    }

    @Override
    public void update(float deltaTime) {
        forkJoinPool.invokeAll(runnableTasks);
    }

    /**
     * @return set of entities processed by the system
     */
    public ImmutableArray<Entity> getEntities() {
        return entities;
    }

    /**
     * @return the Family used when the system was created
     */
    public Family getFamily() {
        return family;
    }

    /**
     * This method is called on every entity on every update call of the EntitySystem. Override this to implement your system's
     * specific processing.
     *
     * @param entity    The current Entity being processed
     * @param deltaTime The delta time between the last and current frame
     */
    protected abstract void processEntity(Entity entity, float deltaTime);
}
