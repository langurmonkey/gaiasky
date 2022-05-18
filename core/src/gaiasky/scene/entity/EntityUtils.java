package gaiasky.scene.entity;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import gaiasky.GaiaSky;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.*;
import gaiasky.util.Settings;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Vector3b;

/**
 * This class contains some general utilities applicable to all entities.
 */
public class EntityUtils {

    /**
     * Returns the absolute position of this entity in the native coordinates
     * (equatorial system) and internal units.
     *
     * @param out Auxiliary vector to put the result in.
     *
     * @return The vector with the position.
     */
    public static Vector3b getAbsolutePosition(Entity entity, Vector3b out) {
        synchronized (entity) {
            Body body = Mapper.body.get(entity);
            out.set(body.pos);
            Entity e = entity;
            GraphNode graph = Mapper.graph.get(e);
            while (graph.parent != null) {
                e = graph.parent;
                graph = Mapper.graph.get(e);
                out.add(Mapper.body.get(e).pos);
            }
            return out;
        }
    }

    /**
     * Checks whether the given entity has a {@link gaiasky.scene.component.Coordinates} component,
     * and the current time is out of range.
     *
     * @return Whether the entity is in time overflow.
     */
    public static boolean isCoordinatesTimeOverflow(Entity entity) {
        return Mapper.coordinates.has(entity) && Mapper.coordinates.get(entity).timeOverflow;
    }

    /**
     * Prepares the blending OpenGL state given a {@link Verts} component.
     *
     * @param verts The verts component.
     */
    public static void blend(Verts verts) {
        if (verts.blend) {
            Gdx.gl20.glEnable(GL20.GL_BLEND);
            if (verts.additive) {
                Gdx.gl20.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE);
            } else {
                Gdx.gl20.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            }
        } else {
            Gdx.gl20.glDisable(GL20.GL_BLEND);
        }
    }

    /**
     * Prepares the depth test OpenGL state given an {@link Verts} component.
     *
     * @param verts The verts component.
     */
    public static void depth(Verts verts) {
        Gdx.gl20.glDepthMask(verts.depth);
        if (verts.depth) {
            Gdx.gl20.glEnable(GL20.GL_DEPTH_TEST);
        } else {
            Gdx.gl20.glDisable(GL20.GL_DEPTH_TEST);
        }
    }

    /**
     * Sets the pale color of this body.
     *
     * @param body      The body component.
     * @param celestial The celestial component.
     * @param plus      The addition.
     */
    public static void setColor2Data(final Body body, final Celestial celestial, final float plus) {
        celestial.colorPale = new float[] { Math.min(1, body.color[0] + plus), Math.min(1, body.color[1] + plus), Math.min(1, body.color[2] + plus) };
    }

}
