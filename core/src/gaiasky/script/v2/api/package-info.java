/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

/**
 * This package contains the API definition of the new <strong>Gaia Sky APIv2</strong>.
 * <p>
 * APIv2 is the preferred interface to access Gaia Sky scripting. It has been re-designed from the ground up, but it
 * contains the same functionality as the old {@link gaiasky.script.IScriptingInterface}. Here are some of its properties:
 * <ul>
 *     <li>It is modular. All methods are organized into modules. This creates a nice distinction between similar calls affecting different systems.
 *     The modules are:
 *     <ul>
 *         <li>{@link gaiasky.script.v2.impl.BaseModule} &mdash; Functions to manipulate and query global attributes.</li>
 *         <li>{@link gaiasky.script.v2.impl.CameraModule} &mdash; Functions to manipulate and query the camera. Contains an inner module.
 *            <ul>
 *              <li>{@link gaiasky.script.v2.impl.InteractiveCameraModule} &mdash; Functions to manipulate the camera in interactive mode.</li>
 *            </ul>
 *         </li>
 *         <li>{@link gaiasky.script.v2.impl.TimeModule} &mdash; Functions to manipulate and query simulation time.</li>
 *         <li>{@link gaiasky.script.v2.impl.SceneModule} &mdash; Functions to add and remove objects from the internal scene.</li>
 *         <li>{@link gaiasky.script.v2.impl.DataModule} &mdash; Functions to load datasets.</li>
 *         <li>{@link gaiasky.script.v2.impl.GraphicsModule} &mdash; Functions to manipulate and query the graphics system.</li>
 *         <li>{@link gaiasky.script.v2.impl.CamcorderModule} &mdash; Functions to manipulate and query the camcorder system.</li>
 *         <li>{@link gaiasky.script.v2.impl.UiModule} &mdash; Functions to manipulate and query the user interface.</li>
 *         <li>{@link gaiasky.script.v2.impl.InputModule} &mdash; Functions to manipulate and query input events.</li>
 *         <li>{@link gaiasky.script.v2.impl.OutputModule} &mdash; Functions to manipulate and query output systems like frame output or screenshots.</li>
 *         <li>{@link gaiasky.script.v2.impl.RefsysModule} &mdash; Functions to convert between reference systems.</li>
 *         <li>{@link gaiasky.script.v2.impl.GeomModule} &mdash; Functions to perform geometry operations.</li>
 *         <li>{@link gaiasky.script.v2.impl.InstancesModule} &mdash; Functions to work with multiple connected instances in the primary-replica model.</li>
 *     </ul>
 *     </li>
 * </ul>
 * The new APIv2 has some advantages over the old one:
 * <ul>
 *     <li>It is well organized.</li>
 *     <li>It is modular.</li>
 *     <li>Uses consistent function naming.</li>
 *     <li>Uses consistent parameter naming.</li>
 * </ul>
 *
 * From a script, you can access the new APIv2 like this:
 *
 * <pre>{@code
 * from py4j.clientserver import ClientServer, JavaParameters
 *
 * gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True, auto_field=True))
 * apiv2 = gateway.entry_point.apiv2
 * # Base module
 * base = apiv2.base
 * # Camera module
 * camera = apiv2.camera
 * # Interactive camera module
 * icam = camera.interactive
 * }
 * </pre>
 * Then, you can access API calls directly from each of the module objects.
 */
package gaiasky.script.v2.api;