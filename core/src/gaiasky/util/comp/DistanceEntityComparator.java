/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.comp;

import gaiasky.scene.Mapper;
import gaiasky.scene.component.Render;

import java.util.Comparator;

public class DistanceEntityComparator<T> implements Comparator<T> {
    @Override
    public int compare(T o1, T o2) {
        return -Double.compare(Mapper.body.get(((Render) o1).entity).distToCamera, Mapper.body.get(((Render) o2).entity).distToCamera);
    }
}
