/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.impl;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.utils.Array;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.util.CatalogManager;
import gaiasky.util.math.Vector2D;
import gaiasky.util.math.Vector3D;
import gaiasky.util.math.Vector3Q;

import java.util.List;
import java.util.Objects;

/**
 * This is the main entry point to the API v2 of Gaia Sky. Contains different modules that are grouped
 * by functionality, and that give access to the different API calls.
 */
public class APIv2 implements IObserver {

    /** Global asset manager reference. **/
    protected final AssetManager assetManager;
    /** Global catalog manager reference. **/
    protected final CatalogManager catalogManager;

    /** Reference to the event manager, {@link gaiasky.event.EventManager}. **/
    protected final EventManager em;

    /** Parameter checker, to be used by all modules. **/
    final ParameterValidator validator;

    /*
     * MODULES BELOW. THEY MUST BE PUBLIC TO BE VISIBLE.
     */

    /** Base module. **/
    public final BaseModule base;
    /** Camera module. **/
    public final CameraModule camera;
    /** Time module. **/
    public final TimeModule time;
    /** Scene module. **/
    public final SceneModule scene;
    /** Graphics module. **/
    public final GraphicsModule graphics;
    /** Input module. **/
    public final InputModule input;
    /** Output module. **/
    public final OutputModule output;
    /** UI module. **/
    public final UiModule ui;
    /** Camcorder module. **/
    public final CamcorderModule camcorder;
    /** Instances module. **/
    public final InstancesModule instances;

    /** List with all the modules. **/
    final Array<APIModule> modules;

    // Auxiliary vectors
    final Vector3D aux3d1, aux3d2, aux3d3, aux3d4, aux3d5, aux3d6;
    final Vector3Q aux3b1, aux3b2, aux3b3, aux3b4, aux3b5;
    final Vector2D aux2d1;

    /**
     * Create a new instance of {@link APIv2} with the given assets and camera managers.
     *
     * @param assetManager   The global asset manager reference.
     * @param catalogManager The global camera manager reference.
     */
    public APIv2(final AssetManager assetManager, final CatalogManager catalogManager) {
        this.assetManager = assetManager;
        this.catalogManager = catalogManager;
        this.em = EventManager.instance;

        this.validator = new ParameterValidator(this);
        this.modules = new Array<>(12);

        this.base = new BaseModule(em, this, "base");
        this.camera = new CameraModule(em, this, "camera");
        this.time = new TimeModule(em, this, "time");
        this.scene = new SceneModule(em, this, "scene");
        this.graphics = new GraphicsModule(em, this, "graphics");
        this.input = new InputModule(em, this, "input");
        this.output = new OutputModule(em, this, "output");
        this.ui = new UiModule(em, this, "ui");
        this.camcorder = new CamcorderModule(em, this, "camcorder");
        this.instances = new InstancesModule(em, this, "instances");

        // Add all to list.
        this.modules.addAll(base, camera, time, scene, graphics, input, output, ui, camcorder, instances);

        // Auxiliary vectors
        aux3d1 = new Vector3D();
        aux3d2 = new Vector3D();
        aux3d3 = new Vector3D();
        aux3d4 = new Vector3D();
        aux3d5 = new Vector3D();
        aux3d6 = new Vector3D();
        aux3b1 = new Vector3Q();
        aux3b2 = new Vector3Q();
        aux3b3 = new Vector3Q();
        aux3b4 = new Vector3Q();
        aux3b5 = new Vector3Q();
        aux2d1 = new Vector2D();

        // Subscribe to events.
        em.subscribe(this, Event.DISPOSE);
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (Objects.requireNonNull(event) == Event.DISPOSE) {// Unsubscribe.
            em.removeAllSubscriptions(this);

            // Dispose modules.
            for (var module : modules) {
                module.dispose();
            }
        }
    }

    double[] dArray(List<?> l) {
        if (l == null) return null;
        double[] res = new double[l.size()];
        int i = 0;
        for (Object o : l) {
            res[i++] = (Double) o;
        }
        return res;
    }

    int[] iArray(List<?> l) {
        if (l == null) return null;
        int[] res = new int[l.size()];
        int i = 0;
        for (Object o : l) {
            res[i++] = (Integer) o;
        }
        return res;
    }
}
