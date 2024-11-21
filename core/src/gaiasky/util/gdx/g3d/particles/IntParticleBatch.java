/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.g3d.particles;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g3d.particles.ResourceData;
import gaiasky.util.gdx.IntRenderableProvider;
import gaiasky.util.gdx.g3d.particles.renderers.IntParticleControllerRenderData;

public interface IntParticleBatch<T extends IntParticleControllerRenderData> extends IntRenderableProvider, ResourceData.Configurable {

	/** Must be called once before any drawing operation */
	public void begin ();

	public void draw (T controller);

	/** Must be called after all the drawing operations */
	public void end ();

	public void save (AssetManager manager, ResourceData assetDependencyData);

	public void load (AssetManager manager, ResourceData assetDependencyData);
}
