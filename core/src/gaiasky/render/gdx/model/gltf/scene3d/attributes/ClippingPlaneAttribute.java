/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.scene3d.attributes;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import gaiasky.render.gdx.shader.attribute.Attribute;

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
