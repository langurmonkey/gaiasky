package gaia.cu9.ari.gaiaorbit.util;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Pool;

import gaia.cu9.ari.gaiaorbit.scenegraph.Gaia;
import gaia.cu9.ari.gaiaorbit.scenegraph.Particle;
import gaia.cu9.ari.gaiaorbit.scenegraph.Planet;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode;
import gaia.cu9.ari.gaiaorbit.scenegraph.Star;
import gaia.cu9.ari.gaiaorbit.scenegraph.octreewrapper.OctreeWrapper;
import gaia.cu9.ari.gaiaorbit.util.math.Matrix4d;
import gaia.cu9.ari.gaiaorbit.util.math.Vector2d;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

public class SimplePool<T> extends Pool<T> {

    private static final int vector3 = Vector3.class.hashCode();
    private static final int vector3d = Vector3d.class.hashCode();
    private static final int vector2d = Vector2d.class.hashCode();
    private static final int planet = Planet.class.hashCode();
    private static final int particle = Particle.class.hashCode();
    private static final int star = Star.class.hashCode();
    private static final int gaia = Gaia.class.hashCode();
    private static final int sgn = SceneGraphNode.class.hashCode();
    private static final int matrix4d = Matrix4d.class.hashCode();
    private static final int octreewrapper = OctreeWrapper.class.hashCode();

    final Integer typeHash;

    public SimplePool(Class<T> type) {
	this(type, 16, Integer.MAX_VALUE);
    }

    public SimplePool(Class<T> type, int initialCapacity) {
	this(type, initialCapacity, Integer.MAX_VALUE);
    }

    public SimplePool(Class<T> type, int initialCapacity, int max) {
	super(initialCapacity, max);
	typeHash = type.hashCode();
    }

    @Override
    protected T newObject() {
	if (typeHash == vector3)
	    return (T) new Vector3();
	if (typeHash == vector3d)
	    return (T) new Vector3d();
	if (typeHash == vector2d)
	    return (T) new Vector2d();
	if (typeHash == planet)
	    return (T) new Planet();
	if (typeHash == star)
	    return (T) new Star();
	if (typeHash == particle)
	    return (T) new Particle();
	if (typeHash == gaia)
	    return (T) new Gaia();
	if (typeHash == sgn)
	    return (T) new SceneGraphNode();
	if (typeHash == matrix4d)
	    return (T) new Matrix4d();
	if (typeHash == octreewrapper)
	    return (T) new OctreeWrapper();

	Logger.warn("Class " + typeHash + " is not poolable!");
	return null;
    }
}
