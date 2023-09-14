/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.scene3d.scene;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.environment.BaseLight;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.util.gdx.model.IntModel;
import gaiasky.util.gdx.model.IntNode;

public class SceneModel implements Disposable
{
	public String name;
	public IntModel model;
	public ObjectMap<IntNode, Camera> cameras = new ObjectMap<>();
	public ObjectMap<IntNode, BaseLight> lights = new ObjectMap<>();
	
	@Override
	public void dispose() {
		model.dispose();
	}
}
