/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.view;

import gaiasky.scene.Mapper;
import gaiasky.scene.component.DatasetDescription;
import gaiasky.scene.component.OrbitElementsSet;
import gaiasky.scene.component.ParticleSet;

public class FilterView extends BaseView {

    private ParticleSet set;
    private OrbitElementsSet elementsSet;
    private DatasetDescription dataset;

    public FilterView() {
    }

    @Override
    protected void entityChanged() {
        super.entityChanged();
        this.dataset = Mapper.datasetDescription.get(entity);
        this.set = Mapper.particleSet.has(entity) ? Mapper.particleSet.get(entity) : Mapper.starSet.get(entity);
        this.elementsSet = Mapper.orbitElementsSet.get(entity);
    }

    public boolean filter(int i) {
        if (set == null && elementsSet == null) {
            return false;
        }
        if (dataset != null && dataset.catalogInfo != null && dataset.catalogInfo.filter != null) {
            if (set != null) {
                return dataset.catalogInfo.filter.evaluate(set.pointData.get(i));
            } else {
                return dataset.catalogInfo.filter.evaluate(elementsSet.data().get(i));
            }
        }
        return true;
    }

}
