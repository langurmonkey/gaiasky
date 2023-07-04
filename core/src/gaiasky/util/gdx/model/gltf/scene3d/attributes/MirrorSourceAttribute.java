/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.attributes;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import gaiasky.util.gdx.shader.attribute.Attribute;

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
		final int c = textureDescription.compareTo(other.textureDescription);
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
