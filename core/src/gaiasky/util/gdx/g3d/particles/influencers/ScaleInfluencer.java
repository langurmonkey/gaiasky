/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.g3d.particles.influencers;

import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels;
import gaiasky.util.gdx.g3d.particles.IntParticleControllerComponent;

public class ScaleInfluencer extends SimpleInfluencer {

	public ScaleInfluencer() {
		super();
		valueChannelDescriptor = ParticleChannels.Scale;
	}

	@Override
	public void activateParticles (int startIndex, int count) {
		if (value.isRelative()) {
			for (int i = startIndex * valueChannel.strideSize, a = startIndex * interpolationChannel.strideSize,
				c = i + count * valueChannel.strideSize; i < c; i += valueChannel.strideSize, a += interpolationChannel.strideSize) {
				float start = value.newLowValue() * controller.scale.x;
				float diff = value.newHighValue() * controller.scale.x;
				interpolationChannel.data[a + ParticleChannels.InterpolationStartOffset] = start;
				interpolationChannel.data[a + ParticleChannels.InterpolationDiffOffset] = diff;
				valueChannel.data[i] = start + diff * value.getScale(0);
			}
		} else {
			for (int i = startIndex * valueChannel.strideSize, a = startIndex * interpolationChannel.strideSize,
				c = i + count * valueChannel.strideSize; i < c; i += valueChannel.strideSize, a += interpolationChannel.strideSize) {
				float start = value.newLowValue() * controller.scale.x;
				float diff = value.newHighValue() * controller.scale.x - start;
				interpolationChannel.data[a + ParticleChannels.InterpolationStartOffset] = start;
				interpolationChannel.data[a + ParticleChannels.InterpolationDiffOffset] = diff;
				valueChannel.data[i] = start + diff * value.getScale(0);
			}
		}
	}

	public ScaleInfluencer(ScaleInfluencer scaleInfluencer) {
		super(scaleInfluencer);
	}

	@Override
	public IntParticleControllerComponent copy () {
		return new ScaleInfluencer(this);
	}

}
