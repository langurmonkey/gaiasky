/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.g3d.particles.renderers;

import gaiasky.util.gdx.g3d.particles.IntParticleBatch;
import gaiasky.util.gdx.g3d.particles.IntParticleController;
import gaiasky.util.gdx.g3d.particles.IntParticleControllerComponent;

public abstract class IntParticleControllerRenderer<D extends IntParticleControllerRenderData, T extends IntParticleBatch<D>>
	extends IntParticleControllerComponent {
	protected T batch;
	protected D renderData;

	protected IntParticleControllerRenderer() {
	}

	protected IntParticleControllerRenderer(D renderData) {
		this.renderData = renderData;
	}

	@Override
	public void update () {
		batch.draw(renderData);
	}

	@SuppressWarnings("unchecked")
	public boolean setBatch (IntParticleBatch<?> batch) {
		if (isCompatible(batch)) {
			this.batch = (T)batch;
			return true;
		}
		return false;
	}

	public abstract boolean isCompatible (IntParticleBatch<?> batch);

	@Override
	public void set (IntParticleController particleController) {
		super.set(particleController);
		if (renderData != null) renderData.controller = controller;
	}
}
