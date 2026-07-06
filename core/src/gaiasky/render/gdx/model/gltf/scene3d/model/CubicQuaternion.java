/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.scene3d.model;

import com.badlogic.gdx.math.Quaternion;

@SuppressWarnings("serial")
public class CubicQuaternion extends Quaternion
{
	public final Quaternion tangentIn = new Quaternion();
	public final Quaternion tangentOut = new Quaternion();

}
