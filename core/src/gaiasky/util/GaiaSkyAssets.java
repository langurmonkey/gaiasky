/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import gaiasky.gui.BookmarksManager;
import gaiasky.render.api.IPostProcessor;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.script.IScriptingInterface;
import gaiasky.util.samp.SAMPClient;
import gaiasky.util.svt.SVTManager;

public class GaiaSkyAssets {
    public IScriptingInterface scriptingInterface;
    public IPostProcessor postProcessor;
    public BookmarksManager bookmarksManager;
    public SAMPClient sampClient;
    public SVTManager svtManager;
}
