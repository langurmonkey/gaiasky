/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.loaders.shared.material;

import com.badlogic.gdx.utils.Array;
import gaiasky.util.gdx.model.gltf.data.material.GLTFMaterial;
import gaiasky.util.gdx.shader.Material;

public interface MaterialLoader {

	Material getDefaultMaterial();

	Material get(int index);

	void loadMaterials(Array<GLTFMaterial> materials);

}
