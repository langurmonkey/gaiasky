/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.graphics.TextureArray;
import gaiasky.scene.record.BillboardDataset;
import gaiasky.util.tree.LoadStatus;

import java.util.concurrent.atomic.AtomicReference;

/**
 * A component that aggregates several sets of billboard datasets.
 */
public class BillboardSet implements Component, IDisposable {

    /** Use procedural generation to create the particles of this set. **/
    public boolean procedural = false;
    /** List of {@link BillboardDataset} objects. **/
    public BillboardDataset[] datasets;
    /** Location of the textures for the billboard particles. **/
    public String[] textureFiles;
    /** The GPU texture array for this set. **/
    public TextureArray textureArray;
    /** Fully qualified name of the data provider class. Loads particle data files. **/
    public String provider;
    /** Current load status. **/
    public AtomicReference<LoadStatus> status = new AtomicReference<>(LoadStatus.NOT_LOADED);

    /** Texture array name. **/
    public String textureArrayName;

    public void setData(Object[] data) {
        int nData = data.length;
        this.datasets = new BillboardDataset[nData];
        for (int i = 0; i < nData; i++) {
            this.datasets[i] = (BillboardDataset) data[i];
        }
    }

    public LoadStatus getStatus() {
        return status.get();
    }

    public void setStatus(LoadStatus status) {
        this.status.set(status);
    }

    public void setProcedural(Boolean procedural) {
        this.procedural = procedural;
    }

    public void setTextures(String[] textures) {
        this.textureFiles = textures;
    }

    public void setTexture(String tex) {
        this.textureFiles = new String[]{tex};
    }

    public boolean contains(BillboardDataset bd) {
        if (datasets == null) {
            return false;
        }
        for (var dataset : datasets) {
            if (bd == dataset)
                return true;
        }
        return false;
    }

    /**
     * Removes the given dataset from the list, if it contains it.
     * @param bd The dataset to remove.
     */
    public void removeDataset(BillboardDataset bd) {
        if (contains(bd)) {
            var newDatasets = new BillboardDataset[datasets.length - 1];
            int j = 0;
            for (var d : datasets) {
                if (d != bd) {
                    newDatasets[j++] = d;
                }
            }
            this.datasets = newDatasets;
        }
    }

    @Override
    public void dispose(Entity e) {
        if (this.textureArray != null) {
            this.textureArray.dispose();
        }
    }
}
