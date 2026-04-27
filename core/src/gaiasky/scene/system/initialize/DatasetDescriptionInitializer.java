/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.gui.main.WelcomeGui;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.DatasetDescription;
import gaiasky.scene.view.FocusView;
import gaiasky.util.DatasetCard;
import gaiasky.util.DatasetCard.DatasetSourceType;
import gaiasky.util.datadesc.DatasetDesc;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Initializes dataset description entities and handles catalog information management.
 */
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
        var graph = Mapper.graph.get(entity);
        // Do not create nested datasets (those whose parents are also datasets)
        var nested = graph.parent != null && Mapper.datasetDescription.has(graph.parent);
        if (datasetDesc.datasetCard == null && WelcomeGui.getLocalDatasets().get() != null) {
            // Try to get it from the datasets.
            var local = WelcomeGui.getLocalDatasets().get();
            var dataset = local.findDatasetByName(base.getName());
            if (dataset != null) {
                datasetDesc.datasetCard = fromDatasetDesc(dataset, entity);
            }
        }
        initializeCatalogInfo(entity, datasetDesc, !nested, base.getName(), datasetDesc.description);
    }

    private DatasetCard fromDatasetDesc(DatasetDesc dd, Entity entity) {
        var result = new DatasetCard(dd.key,
                                     dd.name,
                                     dd.description,
                                     view.getDataFile(),
                                     DatasetSourceType.INTERNAL,
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

        if (create && dd.datasetCard == null) {
            dd.datasetCard = new DatasetCard(null, name, description, dataFile, DatasetSourceType.INTERNAL, 1f, entity);
        }

        if (dd.datasetCard != null && dd.datasetCard.entity == null) {
            dd.datasetCard.setEntity(entity);
        }

        if (dd.datasetCard != null && dd.addDataset) {
            if (dd.datasetCard.nParticles <= 0) {
                dd.datasetCard.nParticles = view.getNumParticles();
            }

            if (dd.datasetCard.sizeBytes <= 0 && dataFile != null && !dataFile.isBlank()) {
                Path df = Path.of(dataFile);
                if (!Files.isRegularFile(df) || !Files.exists(df)) {
                    df = Path.of(GaiaSky.settings().data.dataFile(dataFile));
                }
                dd.datasetCard.sizeBytes = Files.exists(df) && Files.isRegularFile(df) ? df.toFile().length() : -1;
            }

            // Insert
            EventManager.publish(Event.CATALOG_ADD, entity, dd.datasetCard, false);
        }
    }

}
