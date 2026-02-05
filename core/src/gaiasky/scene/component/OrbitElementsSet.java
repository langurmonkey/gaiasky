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
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.api.IParticleRecord;

import java.util.List;

/**
 * Aggregates children entities (typically orbits) so that they are treated as one,
 * especially in terms of GPU draw calls.
 */
public class OrbitElementsSet implements Component {
    public Array<Entity> alwaysUpdate;
    public boolean initialUpdate = true;

    /**
     * Particle data.
     */
    public List<IParticleRecord> data;

    /**
     * Fully qualified name of data provider class.
     */
    public String provider;

    /**
     * Pointer to the data file.
     */
    public String datafile;

    public void markForUpdate(Render render) {
        EventManager.publish(Event.GPU_DISPOSE_ORBITAL_ELEMENTS, render);
    }

    /**
     * Returns the list of particles.
     */
    public List<IParticleRecord> data() {
        return data;
    }

    public void setDataFile(String dataFile) {
        this.datafile = dataFile;
    }

    public void setData(List<IParticleRecord> pointData) {
        this.data = pointData;
    }
}
