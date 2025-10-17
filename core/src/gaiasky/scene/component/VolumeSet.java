/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.scene.record.BillboardDataset;
import gaiasky.scene.record.VolumeDataset;
import gaiasky.util.tree.LoadStatus;

import java.util.concurrent.atomic.AtomicReference;

/**
 * A component that aggregates several sets of volume datasets.
 */
public class VolumeSet implements Component {

    public VolumeDataset[] datasets;
    public String provider;
    public AtomicReference<LoadStatus> status = new AtomicReference<>(LoadStatus.NOT_LOADED);

    public void setData(Object[] data) {
        int nData = data.length;
        this.datasets = new VolumeDataset[nData];
        for (int i = 0; i < nData; i++) {
            this.datasets[i] = (VolumeDataset) data[i];
        }
    }

    public LoadStatus getStatus() {
        return status.get();
    }

    public void setStatus(LoadStatus status) {
        this.status.set(status);
    }
}
