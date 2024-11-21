/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.g3d.particles;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g3d.particles.ParticleController;
import com.badlogic.gdx.graphics.g3d.particles.ResourceData;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public abstract class IntParticleControllerComponent implements Disposable, Json.Serializable, ResourceData.Configurable {
	protected static final Vector3 TMP_V1 = new Vector3(), TMP_V2 = new Vector3(), TMP_V3 = new Vector3(), TMP_V4 = new Vector3(),
		TMP_V5 = new Vector3(), TMP_V6 = new Vector3();
	protected static final Quaternion TMP_Q = new Quaternion(), TMP_Q2 = new Quaternion();
	protected static final Matrix3 TMP_M3 = new Matrix3();
	protected static final Matrix4 TMP_M4 = new Matrix4();
	protected IntParticleController controller;

	/** Called to initialize new emitted particles. */
	public void activateParticles (int startIndex, int count) {
	};

	/** Called to notify which particles have been killed. */
	public void killParticles (int startIndex, int count) {
	};

	/** Called to execute the component behavior. */
	public void update () {
	};

	/** Called once during intialization */
	public void init () {
	};

	/** Called at the start of the simulation. */
	public void start () {
	};

	/** Called at the end of the simulation. */
	public void end () {
	};

	public void dispose () {
	}

	public abstract IntParticleControllerComponent copy ();

	/** Called during initialization to allocate additional particles channels */
	public void allocateChannels () {
	}

	public void set (IntParticleController particleController) {
		controller = particleController;
	}

	@Override
	public void save (AssetManager manager, ResourceData data) {
	}

	@Override
	public void load (AssetManager manager, ResourceData data) {
	}

	@Override
	public void write (Json json) {
	}

	@Override
	public void read (Json json, JsonValue jsonData) {
	}

}
