/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.scene3d.model;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class WeightVector {

	public int count;
	public float [] values;

	public WeightVector() {
		this(0, 8);
	}
	
	public WeightVector(int count) {
		this(count, 8);
	}

	public WeightVector(int count, int max) {
		this.count = count;
		values = new float[Math.max(count, max)];
	}

	public WeightVector set(WeightVector weights) {
		if(weights.count > values.length){
			values = new float[weights.count];
		}
			// throw new GdxRuntimeException("WeightVector out of bound");
		this.count = weights.count;
        System.arraycopy(weights.values, 0, values, 0, weights.values.length);
		return this;
	}

	public void lerp(WeightVector value, float t) {
		if(count != value.count) throw new GdxRuntimeException("WeightVector count mismatch");
		for(int i=0 ; i<count ; i++){
			values[i] = MathUtils.lerp(values[i], value.values[i], t);
		}
	}
	
	@Override
	public String toString() {
		String s = "WeightVector(";
		for(int i=0 ; i<count ; i++){
			if(i > 0) s += ", ";
			s += values[i];
		}
		return s + ")";
	}

	public WeightVector set() {
		this.count = 0;
		return this;
	}

	public WeightVector cpy() {
		return new WeightVector(count, values.length).set(this);
	}

	public float get(int index) {
		return values[index];
	}

	public WeightVector scl(float s) {
		for(int i=0 ; i<count ; i++){
			values[i] *= s;
		}
		return this;
	}

	public WeightVector mulAdd(WeightVector w, float s) {
		for(int i=0 ; i<count ; i++){
			values[i] += w.values[i] * s;
		}
		return this;
	}

}
