package gaiasky.scene.entity;

import com.badlogic.ashley.core.Entity;
import gaiasky.render.ComponentTypes;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.component.tag.TagNoProcess;
import gaiasky.scenegraph.VertsObject;
import gaiasky.util.math.Vector3b;

public class KeyframeUtils {

    private Scene scene;

    public KeyframeUtils(Scene scene) {
        this.scene = scene;
    }

    public Entity newVerts(Scene scene, String name, ComponentTypes ct, Class<?> clazz, float[] color, RenderGroup rg, boolean closedLoop, float primitiveSize) {
        var entity = scene.archetypes().get(clazz.getName()).createEntity();

        var base = Mapper.base.get(entity);
        base.setName(name);
        base.ct = ct;

        var body = Mapper.body.get(entity);
        body.setColor(color);

        var verts = Mapper.verts.get(entity);
        verts.renderGroup = rg;
        verts.closedLoop = closedLoop;
        verts.primitiveSize = primitiveSize;

        var graph = Mapper.graph.get(entity);
        graph.translation = new Vector3b();

        // First, initialize it.
        scene.initializeEntity(entity);
        // Then, add no-process tag.
        entity.add(scene.engine.createComponent(TagNoProcess.class));

        return entity;
    }

    public Entity newVerts(Scene scene, String name, ComponentTypes ct, Class<?> clazz, float[] color, RenderGroup rg, boolean closedLoop, float primitiveSize, boolean blend, boolean depth) {
        var entity = newVerts(scene, name, ct, clazz, color, rg, closedLoop, primitiveSize);

        var verts = Mapper.verts.get(entity);
        verts.blend = blend;
        verts.depth = depth;

        return entity;
    }
}
