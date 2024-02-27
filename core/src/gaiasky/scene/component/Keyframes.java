/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.Array;
import gaiasky.scene.Scene;
import gaiasky.util.camera.rec.Keyframe;

import java.util.List;

public class Keyframes implements Component {

    /**
     * Multiplier to primitive size
     **/
    public final float ss = 1f;
    /**
     * Keyframe objects.
     */
    public List<Keyframe> keyframes;
    /**
     * Selected keyframe.
     **/
    public Keyframe selected = null;
    /**
     * Highlighted keyframe.
     */
    public Keyframe highlighted = null;
    /**
     * The actual path.
     **/
    public Entity path;
    /**
     * The segments joining the knots.
     **/
    public Entity segments;
    /**
     * The knots, or keyframe positions.
     **/
    public Entity knots;
    /**
     * Knots which are also seams.
     **/
    public Entity knotsSeam;
    /**
     * Selected knot.
     **/
    public Entity selectedKnot;
    /**
     * High-lighted knot.
     */
    public Entity highlightedKnot;
    /**
     * Contains pairs of {direction, up} representing the orientation at each knot.
     **/
    public Array<Entity> orientations;
    /**
     * Objects.
     **/
    public Array<Entity> objects;
    /**
     * Invisible focus for camera.
     */
    public Entity focus;
    /** Reference to the scene. **/
    public Scene scene;

    public Keyframes() {
    }

    public void clearOrientations() {
        for (Entity vo : orientations)
            objects.removeValue(vo, true);
        orientations.clear();
    }
}
