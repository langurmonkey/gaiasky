/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.g3d.particles.renderers;

import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels;
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels.ColorInitializer;
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels.Rotation2dInitializer;
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels.ScaleInitializer;
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels.TextureRegionInitializer;
import gaiasky.util.gdx.g3d.particles.IntBillboardParticleBatch;
import gaiasky.util.gdx.g3d.particles.IntParticleBatch;
import gaiasky.util.gdx.g3d.particles.IntParticleControllerComponent;

public class IntBillboardRenderer extends IntParticleControllerRenderer<IntBillboardControllerRenderData, IntBillboardParticleBatch> {

	public IntBillboardRenderer() {
		super(new IntBillboardControllerRenderData());
	}

	public IntBillboardRenderer(IntBillboardParticleBatch batch) {
		this();
		setBatch(batch);
	}

	@Override
	public void allocateChannels () {
		renderData.positionChannel = controller.particles.addChannel(ParticleChannels.Position);
		renderData.regionChannel = controller.particles.addChannel(ParticleChannels.TextureRegion, TextureRegionInitializer.get());
		renderData.colorChannel = controller.particles.addChannel(ParticleChannels.Color, ColorInitializer.get());
		renderData.scaleChannel = controller.particles.addChannel(ParticleChannels.Scale, ScaleInitializer.get());
		renderData.rotationChannel = controller.particles.addChannel(ParticleChannels.Rotation2D, Rotation2dInitializer.get());
	}

	@Override
	public IntParticleControllerComponent copy () {
		return new IntBillboardRenderer(batch);
	}

	@Override
	public boolean isCompatible (IntParticleBatch<?> batch) {
		return batch instanceof IntBillboardParticleBatch;
	}

}
