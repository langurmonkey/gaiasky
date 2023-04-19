/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.model;

import com.badlogic.gdx.utils.Array;

public class IntAnimation {
    /** the unique id of the animation **/
    public String id;
    /** the duration in seconds **/
    public float duration;
    /** the animation curves for individual nodes **/
    public Array<IntNodeAnimation> nodeAnimations = new Array<>();
}
