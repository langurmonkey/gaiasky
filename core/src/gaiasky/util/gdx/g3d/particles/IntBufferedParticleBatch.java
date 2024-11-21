/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.g3d.particles;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.utils.Array;
import gaiasky.util.gdx.g3d.particles.renderers.IntParticleControllerRenderData;

public abstract class IntBufferedParticleBatch<T extends IntParticleControllerRenderData> implements IntParticleBatch<T> {
	protected Array<T> renderData;
	protected int bufferedParticlesCount, currentCapacity = 0;
	protected IntParticleSorter sorter;
	protected Camera camera;

	protected IntBufferedParticleBatch(Class<T> type) {
		this.sorter = new IntParticleSorter.Distance();
		renderData = new Array<T>(false, 10, type);
	}

	public void begin () {
		renderData.clear();
		bufferedParticlesCount = 0;
	}

	@Override
	public void draw (T data) {
		if (data.controller.particles.size > 0) {
			renderData.add(data);
			bufferedParticlesCount += data.controller.particles.size;
		}
	}

	/** */
	public void end () {
		if (bufferedParticlesCount > 0) {
			ensureCapacity(bufferedParticlesCount);
			flush(sorter.sort(renderData));
		}
	}

	/** Ensure the batch can contain the passed in amount of particles */
	public void ensureCapacity (int capacity) {
		if (currentCapacity >= capacity) return;
		sorter.ensureCapacity(capacity);
		allocParticlesData(capacity);
		currentCapacity = capacity;
	}

	public void resetCapacity () {
		currentCapacity = bufferedParticlesCount = 0;
	}

	protected abstract void allocParticlesData (int capacity);

	public void setCamera (Camera camera) {
		this.camera = camera;
		sorter.setCamera(camera);
	}

	public IntParticleSorter getSorter () {
		return sorter;
	}

	public void setSorter (IntParticleSorter sorter) {
		this.sorter = sorter;
		sorter.setCamera(camera);
		sorter.ensureCapacity(currentCapacity);
	}

	/** Sends the data to the gpu. This method must use the calculated offsets to build the particles meshes. The offsets represent
	 * the position at which a particle should be placed into the vertex array.
	 * @param offsets the calculated offsets */
	protected abstract void flush (int[] offsets);

	public int getBufferedCount () {
		return bufferedParticlesCount;
	}
}
