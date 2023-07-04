/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.attributes;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import gaiasky.util.gdx.shader.attribute.Attribute;

public class FogAttribute  extends Attribute
{
	public static final String FogEquationAlias = "fogEquation";
	public static final int FogEquation = register(FogEquationAlias);
	
	public static FogAttribute createFog(float near, float far, float exponent){
		return new FogAttribute(FogEquation).set(near, far, exponent);
	}
	
	public final Vector3 value = new Vector3();

	public FogAttribute (int index) {
		super(index);
	}

	public Attribute set(Vector3 value) {
		this.value.set(value);
		return this;
	}
	
	public FogAttribute set(float near, float far, float exponent) {
		this.value.set(near, far, exponent);
		return this;
	}
	
	@Override
	public Attribute copy () {
		return new FogAttribute(index).set(value);
	}

	@Override
	public int compareTo (Attribute o) {
		if (index != o.index)
			return index - o.index;
		FogAttribute other = (FogAttribute)o;
		if(!MathUtils.isEqual(value.x, other.value.x)) return value.x < other.value.x ? -1 : 1;
		if(!MathUtils.isEqual(value.y, other.value.y)) return value.y < other.value.y ? -1 : 1;
		if(!MathUtils.isEqual(value.z, other.value.z)) return value.z < other.value.z ? -1 : 1;
		return 0;
	}

}
