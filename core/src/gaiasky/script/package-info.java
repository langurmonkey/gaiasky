/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

/**
 * <strong>Important:</strong> Since Gaia Sky 3.6.9 we have a new API ({@link gaiasky.script.v2.api APIv2}). It is the recommended way to interact with Gaia Sky,
 * whether you are using Python, the REST server, or the internal console.
 * <p>
 * For backwards compatibility purposes, the old APIv1 ({@link gaiasky.script.IScriptingInterface}) will still be kept around. However, if you are
 * starting with Gaia Sky scripting, it is strongly encouraged to use the new APIv2.
 * <p>
 * This package contains the definition and implementation of the Gaia Sky APIv1. This API has been used up until Gaia Sky 3.6.8. Since that version,
 * APIv1 is implemented by calling APIv2 functions. This means that the actual method implementations are in APIv2.
 * <p>
 * Have a look at {@link gaiasky.script.IScriptingInterface IScriptingInterface} for a full listing of all the APIv1 calls. The implementation
 * is in {@link gaiasky.script.EventScriptingInterface}. In order to access the APIv1 from a Python script, do this:
 * <pre>{@code
 * from py4j.clientserver import ClientServer, JavaParameters
 *
 * gateway = ClientServer(java_parameters=JavaParameters(auto_convert=True))
 * gs = gateway.entry_point
 *
 * # Here we can use gs to call APIv1 methods
 * gs.goToObject("Mars", 20.0, 4.5)
 * gs.startSimulationTime()
 *
 * [...]
 *
 * # Remember to shut down the connection before exiting
 * gateway.shutdown()
 * }</pre>
 */
package gaiasky.script;