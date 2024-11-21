/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.g3d.particles.emitters;

import com.badlogic.gdx.graphics.g3d.particles.ParticleControllerComponent;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import gaiasky.util.gdx.g3d.particles.IntParticleControllerComponent;

public abstract class IntEmitter extends IntParticleControllerComponent implements Json.Serializable {
	/** The min/max quantity of particles */
	public int minParticleCount, maxParticleCount = 4;

	/** Current state of the emission, should be currentTime/ duration Must be updated on each update */
	public float percent;

	public IntEmitter(IntEmitter regularEmitter) {
		set(regularEmitter);
	}

	public IntEmitter() {
	}

	@Override
	public void init () {
		controller.particles.size = 0;
	}

	@Override
	public void end () {
		controller.particles.size = 0;
	}

	public boolean isComplete () {
		return percent >= 1.0f;
	}

	public int getMinParticleCount () {
		return minParticleCount;
	}

	public void setMinParticleCount (int minParticleCount) {
		this.minParticleCount = minParticleCount;
	}

	public int getMaxParticleCount () {
		return maxParticleCount;
	}

	public void setMaxParticleCount (int maxParticleCount) {
		this.maxParticleCount = maxParticleCount;
	}

	public void setParticleCount (int aMin, int aMax) {
		setMinParticleCount(aMin);
		setMaxParticleCount(aMax);
	}

	public void set (IntEmitter emitter) {
		minParticleCount = emitter.minParticleCount;
		maxParticleCount = emitter.maxParticleCount;
	}

	@Override
	public void write (Json json) {
		json.writeValue("minParticleCount", minParticleCount);
		json.writeValue("maxParticleCount", maxParticleCount);
	}

	@Override
	public void read (Json json, JsonValue jsonData) {
		minParticleCount = json.readValue("minParticleCount", int.class, jsonData);
		maxParticleCount = json.readValue("maxParticleCount", int.class, jsonData);
	}

}
