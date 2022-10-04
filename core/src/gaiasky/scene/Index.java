package gaiasky.scene;

import com.badlogic.ashley.core.Entity;
import gaiasky.scene.component.*;
import gaiasky.scene.view.PositionView;
import gaiasky.scene.record.Position;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.util.Logger;
import gaiasky.util.i18n.I18n;
import gaiasky.util.tree.IPosition;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Contains the index of objects. For each object name, the index keeps a reference to the
 * object itself. It also contains the Hipparcos index, where each HIP id is linked to
 * a star.
 */
public class Index {
    private static final Logger.Log logger = Logger.getLogger(Index.class);

    /** Quick lookup map. Name to node. **/
    protected final Map<String, Entity> index;

    /**
     * Map from integer to position with all Hipparcos stars, for the
     * constellations.
     **/
    protected final Map<Integer, IPosition> hipMap;

    /** The archetypes container. **/
    protected Archetypes archetypes;

    /**
     * Creates a new index with the given archetypes and initial number of entities.
     *
     * @param archetypes     The archetypes.
     * @param numberEntities The initial number of entities in the index.
     */
    public Index(Archetypes archetypes, int numberEntities) {
        this.archetypes = archetypes;

        // String-to-node map. The number of objects is a first approximation, as
        // some nodes actually contain multiple objects.
        index = new ConcurrentHashMap<>((int) (numberEntities * 1.25));

        // HIP map with 121k * 1.25
        hipMap = new ConcurrentHashMap<>(151250);
    }

    /**
     * Returns the entity identified with the given name, or null if
     * it is not found.
     *
     * @param name The name of the entity.
     *
     * @return The entity, or null if it does not exist.
     */
    public Entity getEntity(String name) {
        name = name.toLowerCase().strip();
        return index.get(name);
    }

    /**
     * Checks whether the index contains an entity with the given name.
     *
     * @param name The name of the entity.
     *
     * @return True if the index contains an entity with the given name. False otherwise.
     */
    public boolean containsEntity(String name) {
        return index.containsKey(name.toLowerCase().trim());
    }

    /**
     * Adds the given node to the index. Returns false if it was not added due to a naming conflict (name already exists)
     * with the same object (same class and same names).
     *
     * @param entity The entity to add.
     *
     * @return False if the object already exists.
     */
    public boolean addToIndex(Entity entity) {
        boolean ok = true;
        Base base;
        if ((base = Mapper.base.get(entity)) != null) {
            if (base.names != null) {
                synchronized (index) {
                    if (mustAddToIndex(entity)) {
                        for (String name : base.names) {
                            String nameLowerCase = name.toLowerCase().trim();
                            if (!index.containsKey(nameLowerCase)) {
                                index.put(nameLowerCase, entity);
                            } else if (!nameLowerCase.isEmpty()) {
                                Entity conflict = index.get(nameLowerCase);
                                var conflictBase = Mapper.base.get(conflict);
                                var entityArchetype = conflictBase.archetype;
                                var conflictArchetype = Mapper.base.get(conflict).archetype;
                                logger.debug(I18n.msg("error.name.conflict", name + " (" + entityArchetype.getName().toLowerCase() + ")", conflictBase.getName() + " (" + conflictArchetype.getName().toLowerCase() + ")"));
                                String[] names1 = base.names;
                                String[] names2 = conflictBase.names;
                                boolean same = names1.length == names2.length;
                                if (same) {
                                    for (int i = 0; i < names1.length; i++) {
                                        same = same && names1[i].equals(names2[i]);
                                    }
                                }
                                if (same) {
                                    same = entityArchetype == conflictArchetype;
                                }
                                ok = !same;
                            }
                        }

                        // Id
                        Id id = Mapper.id.get(entity);
                        if (id != null && id.id > 0) {
                            String idString = String.valueOf(id.id);
                            index.put(idString, entity);
                        }
                    }

                    // Special cases

                    // HIP stars add "HIP + hipID"
                    Archetype starArchetype = archetypes.get("gaiasky.scenegraph.Star");
                    if (starArchetype.matches(entity)) {
                        // Hip
                        Hip hip = Mapper.hip.get(entity);
                        if (hip.hip > 0) {
                            String hipid = "hip " + hip.hip;
                            index.put(hipid, entity);
                        }
                    }

                    // Particle/star sets add names of each contained particle.
                    addParticleSet(entity, Mapper.particleSet.get(entity));
                    addParticleSet(entity, Mapper.starSet.get(entity));

                }
            }
        }
        if (!ok) {
            logger.warn(I18n.msg("error.object.exists", base.getName() + "(" + archetypes.findArchetype(entity).getName() + ")"));
        }
        return ok;
    }

    private void addParticleSet(Entity entity, ParticleSet particleSet) {
        if (particleSet != null) {
            if (particleSet.index != null) {
                Set<String> keys = particleSet.index.keySet();
                for (String key : keys) {
                    index.put(key, entity);
                }
            }
        }
    }

    public Map<Integer, IPosition> getHipMap() {
        return hipMap;
    }

    public void addToHipMap(Entity entity) {
        if (Mapper.octree.has(entity)) {
            var octree = Mapper.octree.get(entity);
            Set<Entity> set = octree.parenthood.keySet();
            for (Entity e : set)
                addToHipMap(e);
        } else {
            Archetype starArchetype = archetypes.get("gaiasky.scenegraph.Star");
            if (starArchetype.matches(entity)) {
                Hip hip = Mapper.hip.get(entity);
                if (hip.hip > 0) {
                    synchronized (hipMap) {
                        if (hipMap.containsKey(hip.hip)) {
                            logger.debug(I18n.msg("error.id.hip.duplicate", hip.hip));
                        } else {
                            hipMap.put(hip.hip, new PositionView(entity));
                        }
                    }
                }
            } else if (Mapper.starSet.has(entity)) {
                StarSet starSet = Mapper.starSet.get(entity);
                List<IParticleRecord> stars = starSet.data();
                synchronized (hipMap) {
                    for (IParticleRecord pb : stars) {
                        if (pb.hip() > 0) {
                            hipMap.put(pb.hip(), new Position(pb.x(), pb.y(), pb.z(), pb.pmx(), pb.pmy(), pb.pmz()));
                        }
                    }
                }
            }
        }
    }

    private boolean mustAddToIndex(Entity entity) {
        // All entities except the ones who have perimeter, location mark and particle or star set
        return entity.getComponent(Perimeter.class) == null && entity.getComponent(LocationMark.class) == null && entity.getComponent(ParticleSet.class) == null && entity.getComponent(StarSet.class) == null;
    }

    /** Removes the given key from the index. **/
    public void remove(String key) {
        index.remove(key);
    }

    /**
     * Removes the given entity from the index.
     *
     * @param entity The entity to remove.
     */
    public void remove(Entity entity) {
        var base = Mapper.base.get(entity);
        if (base.names != null) {
            synchronized (index) {
                for (String name : base.names) {
                    index.remove(name.toLowerCase().trim());
                }

                // Id
                if (base.id > 0) {
                    String id = String.valueOf(base.id);
                    index.remove(id);
                }

                // HIP
                if (Mapper.hip.has(entity)) {
                    var hip = Mapper.hip.get(entity);
                    hipMap.remove(hip.hip);
                }

                // Special cases
                if (Mapper.particleSet.has(entity)) {
                    var set = Mapper.particleSet.get(entity);
                    removeFromIndex(set);
                }
                if (Mapper.starSet.has(entity)) {
                    var set = Mapper.starSet.get(entity);
                    removeFromIndex(set);
                }
            }
        }
    }

    /** Removes the entities in the given particle set from this index. **/
    public void removeFromIndex(ParticleSet set) {
        if (set.index != null) {
            Set<String> keys = set.index.keySet();
            for (String key : keys) {
                index.remove(key);
            }
        }
    }

    /**
     * Returns focus entities in this index matching the given string by name, to a maximum
     * of <code>maxResults</code>. The <code>abort</code> atomic boolean can be used to stop
     * the computation.
     *
     * @param name       The name.
     * @param results    The set where the results are to be stored.
     * @param maxResults The maximum number of results.
     * @param abort      To enable abortion mid-computation.
     */
    public void matchingFocusableNodes(String name, SortedSet<String> results, int maxResults, AtomicBoolean abort) {
        synchronized (index) {
            Set<String> keys = index.keySet();
            name = name.toLowerCase().trim();

            int i = 0;
            // Starts with
            for (String key : keys) {
                if (abort != null && abort.get())
                    return;
                Entity entity = index.get(key);
                if (Mapper.focus.has(entity) && key.startsWith(name)) {
                    results.add(key);
                    i++;
                }
                if (i >= maxResults)
                    return;
            }
            // Contains
            for (String key : keys) {
                if (abort != null && abort.get())
                    return;
                Entity entity = index.get(key);
                if (Mapper.focus.has(entity) && key.contains(name)) {
                    results.add(key);
                    i++;
                }
                if (i >= maxResults)
                    return;
            }
        }
    }
}
