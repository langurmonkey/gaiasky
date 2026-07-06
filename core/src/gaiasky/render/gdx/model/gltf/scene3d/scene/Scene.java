/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.model.gltf.scene3d.scene;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.environment.BaseLight;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.graphics.g3d.environment.SpotLight;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Entry;
import com.badlogic.gdx.utils.Pool;
import gaiasky.render.gdx.IntRenderable;
import gaiasky.render.gdx.IntRenderableProvider;
import gaiasky.render.gdx.model.IntAnimationController;
import gaiasky.render.gdx.model.IntModel;
import gaiasky.render.gdx.model.IntModelInstance;
import gaiasky.render.gdx.model.IntNode;
import gaiasky.render.gdx.model.gltf.scene3d.animation.AnimationControllerHack;
import gaiasky.render.gdx.model.gltf.scene3d.animation.AnimationsPlayer;
import gaiasky.render.gdx.model.gltf.scene3d.lights.DirectionalLightEx;
import gaiasky.render.gdx.model.gltf.scene3d.lights.PointLightEx;
import gaiasky.render.gdx.model.gltf.scene3d.lights.SpotLightEx;
import gaiasky.render.gdx.model.gltf.scene3d.model.ModelInstanceHack;

public class Scene implements IntRenderableProvider, Updatable {
    public IntModelInstance modelInstance;
    public IntAnimationController animationController;

    public final ObjectMap<IntNode, BaseLight> lights = new ObjectMap<>();
    public final ObjectMap<IntNode, Camera> cameras = new ObjectMap<>();
    public final AnimationsPlayer animations;

    private static final Matrix4 transform = new Matrix4();

    public Scene(SceneModel sceneModel) {
        this(new ModelInstanceHack(sceneModel.model), sceneModel);
    }

    public Scene(SceneModel sceneModel, String... rootNodeIds) {
        this(new ModelInstanceHack(sceneModel.model, rootNodeIds), sceneModel);
    }

    private Scene(IntModelInstance modelInstance, SceneModel sceneModel) {
        this(modelInstance);
        for (Entry<IntNode, Camera> entry : sceneModel.cameras) {
            IntNode node = modelInstance.getNode(entry.key.id, true);
            if (node != null) {
                cameras.put(node, createCamera(entry.value));
            }
        }
        for (Entry<IntNode, BaseLight> entry : sceneModel.lights) {
            IntNode node = modelInstance.getNode(entry.key.id, true);
            if (node != null) {
                lights.put(node, createLight(entry.value));
            }
        }
        syncCameras();
        syncLights();
    }

    public Camera createCamera(Camera from) {
        Camera copy;
        if (from instanceof PerspectiveCamera) {
            PerspectiveCamera camera = new PerspectiveCamera();
            camera.fieldOfView = ((PerspectiveCamera) from).fieldOfView;
            copy = camera;
        } else if (from instanceof OrthographicCamera) {
            OrthographicCamera camera = new OrthographicCamera();
            camera.zoom = ((OrthographicCamera) from).zoom;
            copy = camera;
        } else {
            throw new GdxRuntimeException("unknown camera type " + from.getClass().getName());
        }
        copy.position.set(from.position);
        copy.direction.set(from.direction);
        copy.up.set(from.up);
        copy.near = from.near;
        copy.far = from.far;
        copy.viewportWidth = from.viewportWidth;
        copy.viewportHeight = from.viewportHeight;
        return copy;
    }

    protected BaseLight createLight(BaseLight from) {
        if (from instanceof DirectionalLight) {
            return new DirectionalLightEx().set((DirectionalLight) from);
        }
        if (from instanceof PointLight) {
            return new PointLightEx().set((PointLight) from);
        }
        if (from instanceof SpotLight) {
            return new SpotLightEx().set((SpotLight) from);
        }
        throw new GdxRuntimeException("unknown light type " + from.getClass().getName());
    }

    /**
     * Default constructor create animated scene if model contains animations.
     */
    public Scene(IntModel model) {
        this(new ModelInstanceHack(model));
    }

    /**
     * Default constructor create animated scene if model instance contains animations.
     */
    public Scene(IntModelInstance modelInstance) {
        this(modelInstance, modelInstance.animations.size > 0);
    }

    /**
     * Create a scene
     */
    public Scene(IntModelInstance modelInstance, boolean animated) {
        super();
        this.modelInstance = modelInstance;
        if (animated) {
            this.animationController = new AnimationControllerHack(modelInstance);
        }
        animations = new AnimationsPlayer(this);
    }

    public Scene(IntModel model, boolean animated) {
        this(new ModelInstanceHack(model), animated);
    }

    @Override
    public void update(Camera camera, float delta) {
        animations.update(delta);
        syncCameras();
        syncLights();
    }

    private void syncCameras() {
        for (Entry<IntNode, Camera> e : cameras) {
            IntNode node = e.key;
            Camera camera = e.value;
            transform.set(modelInstance.transform).mul(node.globalTransform);
            camera.position.setZero().mul(transform);
            camera.direction.set(0, 0, -1).rot(transform);
            camera.up.set(Vector3.Y).rot(transform);
            camera.update();
        }
    }

    private void syncLights() {
        for (Entry<IntNode, BaseLight> e : lights) {
            IntNode node = e.key;
            BaseLight light = e.value;
            transform.set(modelInstance.transform).mul(node.globalTransform);
            if (light instanceof DirectionalLight) {
                ((DirectionalLight) light).direction.set(0, 0, -1).rot(transform);
            } else if (light instanceof PointLight) {
                ((PointLight) light).position.setZero().mul(transform);
            } else if (light instanceof SpotLight) {
                ((SpotLight) light).position.setZero().mul(transform);
                ((SpotLight) light).direction.set(0, 0, -1).rot(transform);
            }
        }
    }

    public Camera getCamera(String name) {
        for (Entry<IntNode, Camera> e : cameras) {
            if (name.equals(e.key.id)) {
                return e.value;
            }
        }
        return null;
    }

    public BaseLight getLight(String name) {
        for (Entry<IntNode, BaseLight> e : lights) {
            if (name.equals(e.key.id)) {
                return e.value;
            }
        }
        return null;
    }

    public int getDirectionalLightCount() {
        int count = 0;
        for (Entry<IntNode, BaseLight> entry : lights) {
            if (entry.value instanceof DirectionalLight) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void getRenderables(Array<IntRenderable> renderables, Pool<IntRenderable> pool) {
        modelInstance.getRenderables(renderables, pool);
    }
}
