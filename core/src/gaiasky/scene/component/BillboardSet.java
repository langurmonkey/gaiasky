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

    public void setTextures(String[] textures) {
        this.textureFiles = textures;
    }

    public void setTexture(String tex) {
        this.textureFiles = new String[]{tex};
    }

    @Override
    public void dispose(Entity e) {
       if (this.textureArray != null) {
           this.textureArray.dispose();
       }
    }
}
