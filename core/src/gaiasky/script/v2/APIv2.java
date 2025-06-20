/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.utils.Array;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.script.IScriptingInterface;
import gaiasky.util.CatalogManager;

import java.util.Objects;

/**
 * The Gaia Sky API (v2) is the evolution of the first one, embodied in {@link IScriptingInterface}. It organizes the functionality into a
 * number of modules:
 * <ul>
 *     <li>base: base module</li>
 *     <li>camera: methods that modify or query the camera</li>
 *     <li>time: functionality related to time</li>
 *     <li>scene: functions and methods that act on the scene and its objects</li>
 *     <li>ui: user interface</li>
 *     <li>input: input methods</li>
 *     <li>output: functions and methods related to the frame output mode and screenshots</li>
 *     <li>instances: methods to control connected instances in the primary-replica model</li>
 *     <li>camcorder: control the integrated camcorder</li>
 *     <li>data: load datasets and catalogs</li>
 *     <li>geom: geometry and reference system utilities</li>
 * </ul>
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

    /** List with all the modules. **/
    final Array<APIModule> modules;

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

        // Add all to list.
        this.modules.addAll(base, camera, time);

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
}
