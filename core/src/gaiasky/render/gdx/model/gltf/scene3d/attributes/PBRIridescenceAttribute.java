/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.scene3d.attributes;

import com.badlogic.gdx.math.MathUtils;
import gaiasky.render.gdx.shader.attribute.Attribute;

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
