/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.attributes;

import com.badlogic.gdx.math.MathUtils;
import gaiasky.util.gdx.shader.attribute.Attribute;

public class PBRIridescenceAttribute extends Attribute
{
	public static final String Alias = "iridescence";
	public static final int Type = register(Alias);

	public float factor = 1, ior = 1.3f, thicknessMin = 100, thicknessMax = 400;
	
	public PBRIridescenceAttribute() {
		super(Type);
	}
	
	public PBRIridescenceAttribute(float factor, float ior, float thicknessMin, float thicknessMax) {
		super(Type);
		this.factor = factor;
		this.ior = ior;
		this.thicknessMin = thicknessMin;
		this.thicknessMax = thicknessMax;
	}

	@Override
	public int compareTo(Attribute o) {
		if (index != o.index)
			return index - o.index;
		PBRIridescenceAttribute other = (PBRIridescenceAttribute)o;
		if(!MathUtils.isEqual(factor, other.factor)) return factor < other.factor ? -1 : 1;
		if(!MathUtils.isEqual(ior, other.ior)) return ior < other.ior ? -1 : 1;
		if(!MathUtils.isEqual(thicknessMin, other.thicknessMin)) return thicknessMin < other.thicknessMin ? -1 : 1;
		if(!MathUtils.isEqual(thicknessMax, other.thicknessMax)) return thicknessMax < other.thicknessMax ? -1 : 1;
		return 0;
	}

	@Override
	public Attribute copy() {
		return new PBRIridescenceAttribute(factor, ior, thicknessMin, thicknessMax);
	}
}
