/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.utils.Array;
import gaiasky.util.coord.ComposedTimedOrbitCoordinates;
import gaiasky.util.coord.IBodyCoordinates;
import gaiasky.util.coord.TimedOrbitCoordinates;

public class Coordinates implements Component, ICopy {

    /**
     * Coordinates provider. Provides position coordinates depending on time.
     **/
    public IBodyCoordinates coordinates;

    /**
     * Whether the current time puts the coordinates are out of time range.
     */
    public boolean timeOverflow;

    public void setCoordinatesProvider(IBodyCoordinates coordinates) {
        this.coordinates = coordinates;
    }

    public void setCoordinates(IBodyCoordinates coordinates) {
        setCoordinatesProvider(coordinates);
    }

    @Override
    public Component getCopy(Engine engine) {
        var copy = engine.createComponent(this.getClass());
        copy.coordinates = coordinates.getCopy();
        copy.timeOverflow = timeOverflow;
        return copy;
    }
}
