/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.scene3d.attributes;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import gaiasky.render.gdx.shader.attribute.Attribute;

public class MirrorSourceAttribute extends Attribute
{
	public static final String TypeAlias = "mirrorSource";
	public static final int Type = register(TypeAlias);
	
	public final TextureDescriptor<Texture> textureDescription = new TextureDescriptor<Texture>();
	public final Vector3 normal = new Vector3();
	
	public MirrorSourceAttribute() {
		super(Type);
	}

	@Override
	public int compareTo(Attribute o) {
		if (index != o.index)
			return index - o.index;
		MirrorSourceAttribute other = (MirrorSourceAttribute)o;
		int c = textureDescription.compareTo(other.textureDescription);
		if (c != 0) return c;
		Vector3 otherNormal = other.normal;
		if(!MathUtils.isEqual(normal.x, otherNormal.x)) return normal.x < otherNormal.x ? -1 : 1;
		if(!MathUtils.isEqual(normal.y, otherNormal.y)) return normal.y < otherNormal.y ? -1 : 1;
		if(!MathUtils.isEqual(normal.z, otherNormal.z)) return normal.z < otherNormal.z ? -1 : 1;
		return 0;
	}

	@Override
	public Attribute copy() {
		return set(textureDescription, normal);
	}

	public MirrorSourceAttribute set(TextureDescriptor<Texture> textureDescription, Vector3 normal) {
		this.textureDescription.set(textureDescription);
		this.normal.set(normal);
		return this;
	}

}
