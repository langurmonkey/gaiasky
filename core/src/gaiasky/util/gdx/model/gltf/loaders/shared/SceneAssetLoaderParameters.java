/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.loaders.shared;

import com.badlogic.gdx.assets.AssetLoaderParameters;
import gaiasky.util.gdx.model.gltf.scene3d.scene.SceneAsset;

public class SceneAssetLoaderParameters extends AssetLoaderParameters<SceneAsset> {

	/** load scene asset with underlying GLTF {@link SceneAsset#data} structure */
	public boolean withData = false;
}
