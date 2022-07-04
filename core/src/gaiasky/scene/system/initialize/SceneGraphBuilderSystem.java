package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.scene.Index;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.GraphNode;
import gaiasky.util.i18n.I18n;

/**
 * Builds the scene graph once all nodes are in the index.
 */
public class SceneGraphBuilderSystem extends InitSystem {

    /** The index reference. **/
    private Index index;

    public SceneGraphBuilderSystem(final Index index, Family family, int priority) {
        super(false, family, priority);
        this.index = index;
    }

    @Override
    public void initializeEntity(Entity entity) {
        var graph = entity.getComponent(GraphNode.class);
        if (graph.parentName != null)  {
            var parent = index.getEntity(graph.parentName);
            if(parent != null) {
                addChild(parent, entity, true);
            } else {
                throw new RuntimeException(I18n.msg("error.parent.notfound", Mapper.base.get(entity).getName(), graph.parentName));
            }
        }

    }

    @Override
    public void setUpEntity(Entity entity) {

    }

    /**
     * Adds a child to the given node and updates the number of children in this
     * node and in all ancestors.
     *
     * @param child               The child node to add.
     * @param updateAncestorCount Whether to update the ancestors number of children.
     */
    public final void addChild(Entity parent, Entity child, boolean updateAncestorCount) {
        var graph = Mapper.graph.get(child);
        var parentGraph = Mapper.graph.get(parent);
        if (parentGraph.children == null) {
            parentGraph.initChildren(parentGraph.parent == null ? 100 : 1);
        }
        parentGraph.children.add(child);
        graph.parent = parent;
        parentGraph.numChildren++;

        if (updateAncestorCount) {
            // Update num children in ancestors
            var ancestor = parentGraph.parent;
            while (ancestor != null) {
                var ancestorGraph = Mapper.graph.get(ancestor);
                ancestorGraph.numChildren++;
                ancestor = ancestorGraph.parent;
            }
        }
    }

    /**
     * Adds the given {@link Entity} list as children to this node.
     *
     * @param children The children nodes to add.
     */
    public final void add(Entity parent, Entity... children) {
        var parentGraph = Mapper.graph.get(parent);
        if (parentGraph.children == null) {
            parentGraph.initChildren(parentGraph.parent == null || Mapper.octree.has(parent) ? 300000 : children.length * 5);
        }
        for (Entity child : children) {
            var graph = Mapper.graph.get(child);
            parentGraph.children.add(child);
            graph.parent = parent;
        }
        parentGraph.numChildren += children.length;
    }
}
