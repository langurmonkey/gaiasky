package gaiasky.util;

import gaiasky.gui.BookmarksManager;
import gaiasky.render.api.IPostProcessor;
import gaiasky.scene.system.render.SceneRenderer;
import gaiasky.script.IScriptingInterface;
import gaiasky.util.samp.SAMPClient;
import gaiasky.util.svt.SVTManager;

/**
 * A bundle of global Gaia Sky assets loaded together.
 */
public class GaiaSkyAssets {
    public IScriptingInterface scriptingInterface;
    public IPostProcessor postProcessor;
    public BookmarksManager bookmarksManager;
    public SAMPClient sampClient;
    public SVTManager svtManager;
}
