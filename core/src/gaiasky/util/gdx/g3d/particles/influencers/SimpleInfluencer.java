/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.g3d.particles.influencers;

import com.badlogic.gdx.graphics.g3d.particles.ParallelArray.ChannelDescriptor;
import com.badlogic.gdx.graphics.g3d.particles.ParallelArray.FloatChannel;
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels;
import com.badlogic.gdx.graphics.g3d.particles.values.ScaledNumericValue;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public abstract class SimpleInfluencer extends Influencer {

	public ScaledNumericValue value;
	FloatChannel valueChannel, interpolationChannel, lifeChannel;
	ChannelDescriptor valueChannelDescriptor;

	public SimpleInfluencer() {
		value = new ScaledNumericValue();
		value.setHigh(1);
	}

	public SimpleInfluencer(SimpleInfluencer billboardScaleinfluencer) {
		this();
		set(billboardScaleinfluencer);
	}

	private void set (SimpleInfluencer scaleInfluencer) {
		value.load(scaleInfluencer.value);
		valueChannelDescriptor = scaleInfluencer.valueChannelDescriptor;
	}

	@Override
	public void allocateChannels () {
		valueChannel = controller.particles.addChannel(valueChannelDescriptor);
		ParticleChannels.Interpolation.id = controller.particleChannels.newId();
		interpolationChannel = controller.particles.addChannel(ParticleChannels.Interpolation);
		lifeChannel = controller.particles.addChannel(ParticleChannels.Life);
	}

	@Override
	public void activateParticles (int startIndex, int count) {
		if (!value.isRelative()) {
			for (int i = startIndex * valueChannel.strideSize, a = startIndex * interpolationChannel.strideSize,
				c = i + count * valueChannel.strideSize; i < c; i += valueChannel.strideSize, a += interpolationChannel.strideSize) {
				float start = value.newLowValue();
				float diff = value.newHighValue() - start;
				interpolationChannel.data[a + ParticleChannels.InterpolationStartOffset] = start;
				interpolationChannel.data[a + ParticleChannels.InterpolationDiffOffset] = diff;
				valueChannel.data[i] = start + diff * value.getScale(0);
			}
		} else {
			for (int i = startIndex * valueChannel.strideSize, a = startIndex * interpolationChannel.strideSize,
				c = i + count * valueChannel.strideSize; i < c; i += valueChannel.strideSize, a += interpolationChannel.strideSize) {
				float start = value.newLowValue();
				float diff = value.newHighValue();
				interpolationChannel.data[a + ParticleChannels.InterpolationStartOffset] = start;
				interpolationChannel.data[a + ParticleChannels.InterpolationDiffOffset] = diff;
				valueChannel.data[i] = start + diff * value.getScale(0);
			}
		}
	}

	@Override
	public void update () {
		for (int i = 0, a = 0, l = ParticleChannels.LifePercentOffset, c = i + controller.particles.size
			* valueChannel.strideSize; i < c; i += valueChannel.strideSize, a += interpolationChannel.strideSize, l += lifeChannel.strideSize) {

			valueChannel.data[i] = interpolationChannel.data[a + ParticleChannels.InterpolationStartOffset]
				+ interpolationChannel.data[a + ParticleChannels.InterpolationDiffOffset] * value.getScale(lifeChannel.data[l]);
		}
	}

	@Override
	public void write (Json json) {
		json.writeValue("value", value);
	}

	@Override
	public void read (Json json, JsonValue jsonData) {
		value = json.readValue("value", ScaledNumericValue.class, jsonData);
	}

}
