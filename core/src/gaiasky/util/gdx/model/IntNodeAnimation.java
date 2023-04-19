/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.model;

import com.badlogic.gdx.graphics.g3d.model.NodeKeyframe;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

public class IntNodeAnimation {
    /** the Node affected by this animation **/
    public IntNode node;
    /** the translation keyframes if any (might be null), sorted by time ascending **/
    public Array<NodeKeyframe<Vector3>> translation = null;
    /** the rotation keyframes if any (might be null), sorted by time ascending **/
    public Array<NodeKeyframe<Quaternion>> rotation = null;
    /** the scaling keyframes if any (might be null), sorted by time ascending **/
    public Array<NodeKeyframe<Vector3>> scaling = null;
}
