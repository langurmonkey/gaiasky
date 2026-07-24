/*
 * Copyright (c) 2023-2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.util;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.scenes.scene2d.ui.TooltipManager;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import gaiasky.GaiaSky;
import gaiasky.data.util.GaiaSkyLoader.GaiaSkyLoaderParameters;
import gaiasky.gui.bookmarks.BookmarksManager;
import gaiasky.render.MainPostProcessor;
import gaiasky.render.gdx.model.IntModel;
import gaiasky.render.gdx.shader.Material;
import gaiasky.scene.record.MaterialComponent;
import gaiasky.scene.record.ModelComponent;
import gaiasky.script.ConsoleManager;
import gaiasky.script.EventScriptingInterface;
import gaiasky.script.HiddenHelperUser;
import gaiasky.util.*;
import gaiasky.util.gravwaves.RelativisticEffectsManager;
import gaiasky.util.samp.SAMPClient;
import gaiasky.util.svt.SVTManager;

import java.util.Map;

public class GaiaSkyLoader extends AsynchronousAssetLoader<GaiaSkyAssets, GaiaSkyLoaderParameters> {

    private GaiaSkyAssets assets;

    public GaiaSkyLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public void loadAsync(AssetManager manager,
                          String fileName,
                          FileHandle file,
                          GaiaSkyLoaderParameters parameter) {
        assets = new GaiaSkyAssets();

        // First stage async.

        // Tooltip to 1s
        TooltipManager.getInstance().initialTime = 1f;

        // Initialize hidden helper user
        HiddenHelperUser.initialize();

        // Initialize gravitational waves helper
        RelativisticEffectsManager.initialize(parameter.gaiaSky.time);

        // Location log
        LocationLogManager.initialize();

        // Init timer thread
        Timer.instance();

        // Catalog manager.
        assets.catalogManager = new CatalogManager();

        // Scripting interface.
        assets.scriptingInterface = new EventScriptingInterface(parameter.gaiaSky.assetManager, assets.catalogManager);

        // Bookmarks manager.
        assets.bookmarksManager = new BookmarksManager();

        // SAMP client.
        assets.sampClient = new SAMPClient(assets.catalogManager);
        assets.sampClient.initialize(parameter.gaiaSky.getGlobalResources()
                                             .getSkin());

        // SVT.
        assets.svtManager = new SVTManager();

        // Post processor.
        assets.postProcessor = new MainPostProcessor(null);
        assets.postProcessor.initialize(manager);

        // Console manager.
        assets.consoleManager = new ConsoleManager(assets.scriptingInterface);
    }

    @Override
    public GaiaSkyAssets loadSync(AssetManager manager,
                                  String fileName,
                                  FileHandle file,
                                  GaiaSkyLoaderParameters parameter) {
        // First stage sync.
        assets.svtManager.doneLoading(manager);
        assets.postProcessor.doneLoading(manager);

        // Preload the biome LUTs if NASA exoplanet archive is enabled.
        if (GaiaSky.settings().data.isEnabled("nasa-exoplanet-archive")) {
            MaterialComponent.getLUTManager();
            String modelType = ModelComponent.getDefaultModelType();
            var modelParams = ModelComponent.getDefaultModelParameters(modelType);
            Bits attributes = Bits.indices(VertexAttributes.Usage.Position, VertexAttributes.Usage.Normal, VertexAttributes.Usage.Tangent, VertexAttributes.Usage.BiNormal, VertexAttributes.Usage.TextureCoordinates);
            if (modelParams.containsKey("attributes")) {
                attributes = Bits.indices(((Long) modelParams.get("attributes")).intValue());
            }
            var ignored = ModelCache.cache.getModel(modelType, modelParams, attributes, GL20.GL_TRIANGLES);
        }
        return assets;
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName,
                                                  FileHandle file,
                                                  GaiaSkyLoaderParameters parameter) {
        return null;
    }

    static public class GaiaSkyLoaderParameters extends AssetLoaderParameters<GaiaSkyAssets> {
        public GaiaSky gaiaSky;

        public GaiaSkyLoaderParameters(GaiaSky gaiaSky) {
            this.gaiaSky = gaiaSky;
        }
    }
}
