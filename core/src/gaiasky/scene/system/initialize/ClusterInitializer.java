/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Matrix4;
import gaiasky.render.BlendMode;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.Mapper;
import gaiasky.scene.entity.FocusActive;
import gaiasky.scene.entity.FocusHit;
import gaiasky.scene.record.ModelComponent;
import gaiasky.scene.system.render.draw.billboard.BillboardEntityRenderSystem;
import gaiasky.scene.system.render.draw.model.ModelEntityRenderSystem;
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;
import gaiasky.scene.view.LabelView;
import gaiasky.util.Bits;
import gaiasky.util.Constants;
import gaiasky.util.ModelCache;
import gaiasky.util.Settings;
import gaiasky.util.gdx.IntMeshPartBuilder;
import gaiasky.util.gdx.IntModelBuilder;
import gaiasky.util.gdx.model.IntModelInstance;
import gaiasky.util.gdx.shader.Environment;
import gaiasky.util.gdx.shader.Material;
import gaiasky.util.gdx.shader.attribute.BlendingAttribute;
import gaiasky.util.gdx.shader.attribute.ColorAttribute;
import gaiasky.util.gdx.shader.attribute.FloatAttribute;

public class ClusterInitializer extends AbstractInitSystem {

    public ClusterInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var base = Mapper.base.get(entity);
        var body = Mapper.body.get(entity);
        var cluster = Mapper.cluster.get(entity);
        var sa = Mapper.sa.get(entity);
        var label = Mapper.label.get(entity);
        var focus = Mapper.focus.get(entity);

        // Focus active.
        focus.activeFunction = FocusActive::isFocusActiveCtOpacity;

        // Focus hits.
        focus.hitCoordinatesConsumer = FocusHit::addHitCoordinateCluster;
        focus.hitRayConsumer = FocusHit::addHitRayCluster;

        // Solid angle.
        sa.thresholdQuad = Math.toRadians(2.0);
        sa.thresholdPoint = Math.toRadians(1.5);
        sa.thresholdLabel = sa.thresholdPoint;

        base.ct = new ComponentTypes(ComponentType.Clusters.ordinal());
        // Compute size from distance and radius, convert to units
        body.size = (float) (Math.tan(Math.toRadians(cluster.radiusDeg)) * cluster.dist);

        label.label = true;
        label.textScale = 0.2f;
        label.labelMax = (float) (.5e-3 / Constants.DISTANCE_SCALE_FACTOR);
        label.labelFactor = 1;
        label.renderConsumer = LabelEntityRenderSystem::renderCluster;
        label.renderFunction = LabelView::renderTextEssential;

    }

    @Override
    public void setUpEntity(Entity entity) {
        initModel(entity);
    }

    private void initModel(Entity entity) {
        var body = Mapper.body.get(entity);
        var model = Mapper.model.get(entity);
        var cluster = Mapper.cluster.get(entity);
        var bb = Mapper.billboard.get(entity);

        if (cluster.clusterTex == null) {
            cluster.clusterTex = new Texture(Settings.settings.data.dataFileHandle(Constants.DATA_LOCATION_TOKEN + "tex/base/cluster-tex.png"), true);
            cluster.clusterTex.setFilter(TextureFilter.MipMapLinearNearest, TextureFilter.Linear);
        }
        if (cluster.model == null) {
            Material mat = new Material(new BlendingAttribute(GL20.GL_ONE, GL20.GL_ONE), new ColorAttribute(ColorAttribute.Diffuse, body.color[0], body.color[1], body.color[2], body.color[3]));
            IntModelBuilder modelBuilder = ModelCache.cache.mb;
            modelBuilder.begin();
            // create part
            IntMeshPartBuilder bPartBuilder = modelBuilder.part("sph", GL20.GL_LINES, Bits.indices(Usage.Position), mat);
            bPartBuilder.icosphere(1, 3, false, true);

            cluster.model = (modelBuilder.end());
            cluster.modelTransform = new Matrix4();
        }

        // Billboard.
        bb.renderConsumer = BillboardEntityRenderSystem::renderBillboardCluster;

        // Model.
        model.renderConsumer = ModelEntityRenderSystem::renderStarClusterModel;

        model.model = new ModelComponent(false);
        model.model.setBlendMode(BlendMode.ADDITIVE);
        model.model.initialize(null);
        DirectionalLight dLight = new DirectionalLight();
        dLight.set(1, 1, 1, 1, 1, 1);
        model.model.env = new Environment();
        model.model.env.add(dLight);
        model.model.env.set(new ColorAttribute(ColorAttribute.AmbientLight, 1.0f, 1.0f, 1.0f, 1f));
        model.model.env.set(new FloatAttribute(FloatAttribute.Shininess, 0.2f));
        model.model.instance = new IntModelInstance(cluster.model, cluster.modelTransform);

        // Relativistic effects
        if (Settings.settings.runtime.relativisticAberration)
            model.model.rec.setUpRelativisticEffectsMaterial(model.model.instance.materials);
        // Gravitational waves
        if (Settings.settings.runtime.gravitationalWaves)
            model.model.rec.setUpGravitationalWavesMaterial(model.model.instance.materials);

    }
}
