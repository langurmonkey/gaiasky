package gaiasky.scene.entity;

import com.badlogic.ashley.core.Entity;
import gaiasky.render.ComponentTypes;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.component.tag.TagNoProcess;
import gaiasky.scenegraph.VertsObject;

public class KeyframeUtils {

    private Scene scene;

    public KeyframeUtils(Scene scene) {
        this.scene = scene;
    }

    public Entity newVerts(String name, ComponentTypes ct, float[] color, RenderGroup rg, boolean closedLoop, float primitiveSize) {
        var entity = scene.archetypes().get(VertsObject.class.getName()).createEntity();
        entity.add(new TagNoProcess());

        var base = Mapper.base.get(entity);
        base.setName(name);
        base.ct = new ComponentTypes();
        base.ct.allSetLike(ct);

        var body = Mapper.body.get(entity);
        body.setColor(color);

        var verts = Mapper.verts.get(entity);
        verts.renderGroup = rg;
        verts.closedLoop = closedLoop;
        verts.primitiveSize = primitiveSize;

        return entity;
    }

    public Entity newVerts(String name, ComponentTypes ct, float[] color, RenderGroup rg, boolean closedLoop, float primitiveSize, boolean blend, boolean depth) {
        var entity = newVerts(name, ct, color, rg, closedLoop, primitiveSize);

        var verts = Mapper.verts.get(entity);
        verts.blend = blend;
        verts.depth = depth;

        return entity;
    }
}
