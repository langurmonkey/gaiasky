/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.attributes;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import gaiasky.util.gdx.shader.attribute.Attribute;

public class ClippingPlaneAttribute extends Attribute
{
	public static final String TypeAlias = "clippingPlane";
	public static final int Type = register(TypeAlias);
	
	public final Plane plane;
	
	public ClippingPlaneAttribute(Plane plane) {
		super(Type);
		this.plane = plane;
	}

	public ClippingPlaneAttribute(Vector3 normal, float d) {
		super(Type);
		this.plane = new Plane(normal, d);
	}

	@Override
	public int compareTo(Attribute o) {
		if (index != o.index)
			return index - o.index;
		ClippingPlaneAttribute other = (ClippingPlaneAttribute)o;
		Vector3 normal = plane.normal;
		Vector3 otherNormal = other.plane.normal;
		if(!MathUtils.isEqual(normal.x, otherNormal.x)) return normal.x < otherNormal.x ? -1 : 1;
		if(!MathUtils.isEqual(normal.y, otherNormal.y)) return normal.y < otherNormal.y ? -1 : 1;
		if(!MathUtils.isEqual(normal.z, otherNormal.z)) return normal.z < otherNormal.z ? -1 : 1;
		if(!MathUtils.isEqual(plane.d, other.plane.d)) return plane.d < other.plane.d ? -1 : 1;
		return 0;
	}

	@Override
	public Attribute copy() {
		return new ClippingPlaneAttribute(plane.normal, plane.d);
	}

}
