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
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import gaiasky.data.AssetBean;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Body;
import gaiasky.scene.component.GraphNode;
import gaiasky.scene.component.Model;
import gaiasky.scene.system.render.draw.model.ModelEntityRenderSystem;
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;
import gaiasky.scene.view.LabelView;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.gdx.shader.attribute.ColorAttribute;
import gaiasky.util.math.Matrix4D;

public class BackgroundModelInitializer extends AbstractInitSystem {

    public BackgroundModelInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var body = Mapper.body.get(entity);
        var model = Mapper.model.get(entity);
        var renderType = Mapper.render.get(entity);

        // Size
        body.size *= (float) Constants.DISTANCE_SCALE_FACTOR;

        // Force texture loading
        model.renderConsumer = ModelEntityRenderSystem::renderGenericModel;

        model.model.forceInit = true;

        model.model.initialize(null);
        model.model.env.set(new ColorAttribute(ColorAttribute.AmbientLight, body.color[0], body.color[1], body.color[2], 1));

        // Render type
        if (renderType.renderGroup == null) {
            renderType.renderGroup = RenderGroup.SKYBOX;
        }
    }

    @Override
    public void setUpEntity(Entity entity) {
        var body = Mapper.body.get(entity);
        var graph = Mapper.graph.get(entity);
        var model = Mapper.model.get(entity);

        // Regular background model (skybox)
        setUpBackgroundModel(entity, body, graph, model);

        if (Mapper.grid.has(entity)) {
            // UV grid
            setUpUVGrid(entity, body);
        }
    }

    private void setUpBackgroundModel(Entity entity, Body body, GraphNode graph, Model model) {
        var label = Mapper.label.get(entity);

        updateLocalTransform(entity, body, graph);

        // Model
        model.model.doneLoading(AssetBean.manager(), graph.localTransform, body.color);
        // Disable depth writes, enable reads
        model.model.setDepthTest(GL20.GL_LEQUAL, false);
    }

    private void updateLocalTransform(Entity entity, Body body, GraphNode graph) {
        var transform = Mapper.transform.get(entity);
        var model = Mapper.model.get(entity);

        graph.localTransform.idt();
        // Initialize transform.
        graph.localTransform.scl(body.size);

        if (transform.transformName != null) {
            Class<Coordinates> c = Coordinates.class;
            try {
                Method m = ClassReflection.getMethod(c, transform.transformName);
                Matrix4D trf = (Matrix4D) m.invoke(null);
                Matrix4 aux = trf.putIn(new Matrix4());
                graph.localTransform.mul(aux);
            } catch (ReflectionException e) {
                Logger.getLogger(this.getClass()).error("Error getting/invoking method Coordinates." + transform.transformName + "()");
            }
        }

        // Must rotate due to orientation of the sphere model
        if (model.model != null && model.model.type.equalsIgnoreCase("sphere")) {
            graph.localTransform.rotate(0, 1, 0, 90);
        }
    }

    private void setUpUVGrid(Entity entity, Body body) {
        var grid = Mapper.grid.get(entity);
        var transform = Mapper.transform.get(entity);
        var label = Mapper.label.get(entity);

        // Initialize transform
        grid.annotTransform.scl(body.size);
        if (transform.transformName != null) {
            Class<Coordinates> c = Coordinates.class;
            try {
                Method m = ClassReflection.getMethod(c, transform.transformName);
                Matrix4D trf = (Matrix4D) m.invoke(null);
                Matrix4 aux = new Matrix4();
                trf.putIn(aux);
                grid.annotTransform.mul(aux);
            } catch (ReflectionException e) {
                Logger.getLogger(this.getClass()).error("Error getting/invoking method Coordinates." + transform.transformName + "()");
            }
        }

        // Label for VR
        label.label = true;
        label.textScale = 0.2f;
        label.labelMax = (float) (.5e-3 / Constants.DISTANCE_SCALE_FACTOR);
        label.labelFactor = 1;
        label.renderConsumer = LabelEntityRenderSystem::renderGridAnnotations;
        label.renderFunction = LabelView::renderTextGridRec;
    }
}
