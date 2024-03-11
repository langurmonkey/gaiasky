/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.gui.WelcomeGui;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.DatasetDescription;
import gaiasky.scene.view.FocusView;
import gaiasky.util.CatalogInfo;
import gaiasky.util.CatalogInfo.CatalogInfoSource;
import gaiasky.util.Settings;
import gaiasky.util.datadesc.DatasetDesc;

import java.nio.file.Files;
import java.nio.file.Path;

public class DatasetDescriptionInitializer extends AbstractInitSystem {

    private final FocusView view;

    public DatasetDescriptionInitializer(boolean setUp,
                                         Family family,
                                         int priority) {
        super(setUp, family, priority);
        this.view = new FocusView();
    }

    @Override
    public void initializeEntity(Entity entity) {
    }

    @Override
    public void setUpEntity(Entity entity) {
        var base = Mapper.base.get(entity);
        var datasetDesc = Mapper.datasetDescription.get(entity);
        if (datasetDesc.catalogInfo == null && WelcomeGui.getLocalDatasets().get() != null) {
            // Try to get it from the datasets.
            var local = WelcomeGui.getLocalDatasets().get();
            var dataset = local.findDatasetByName(base.getName());
            if (dataset != null) {
                datasetDesc.catalogInfo = fromDatasetDesc(dataset, entity);
            }
        }
        initializeCatalogInfo(entity, datasetDesc, true, base.getName(), datasetDesc.description);
    }

    private CatalogInfo fromDatasetDesc(DatasetDesc dd, Entity entity) {
        var result = new CatalogInfo(dd.name,
                dd.description,
                view.getDataFile(),
                CatalogInfoSource.INTERNAL,
                1f,
                entity);
        result.nParticles = dd.nObjects;
        result.sizeBytes = dd.sizeBytes;
        return result;
    }

    protected void initializeCatalogInfo(Entity entity,
                                         DatasetDescription dd,
                                         boolean create,
                                         String name,
                                         String description) {
        view.setEntity(entity);
        String dataFile = view.getDataFile();

        if (create && dd.catalogInfo == null) {
            dd.catalogInfo = new CatalogInfo(name, description, dataFile, CatalogInfoSource.INTERNAL, 1f, entity);
        }

        if (dd.catalogInfo != null && dd.addDataset) {
            if (dd.catalogInfo.nParticles <= 0) {
                dd.catalogInfo.nParticles = view.getNumParticles();
            }

            if (dd.catalogInfo.sizeBytes <= 0 && dataFile != null && !dataFile.isBlank()) {
                Path df = Path.of(dataFile);
                if (!Files.isRegularFile(df) || !Files.exists(df)) {
                    df = Path.of(Settings.settings.data.dataFile(dataFile));
                }
                dd.catalogInfo.sizeBytes = Files.exists(df) && Files.isRegularFile(df) ? df.toFile().length() : -1;
            }

            if (dd.catalogInfo.entity == null) {
                dd.catalogInfo.entity = entity;
            }
            // Insert
            EventManager.publish(Event.CATALOG_ADD, entity, dd.catalogInfo, false);
        }
    }

}
