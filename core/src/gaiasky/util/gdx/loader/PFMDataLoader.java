/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.loader;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;

public class PFMDataLoader extends AsynchronousAssetLoader<PFMData, PFMDataLoader.PFMDataParameter> {
    private static final Log logger = Logger.getLogger(PFMDataLoader.class);
    PFMDataInfo info = new PFMDataInfo();

    public PFMDataLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, PFMDataParameter parameter) {
        return null;
    }

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, PFMDataParameter parameter) {
        info.filename = fileName;
        logger.info("Loading PFM: " + file.path());
        info.data = PFMReader.readPFMData(file, false, parameter.invert);
    }

    @Override
    public PFMData loadSync(AssetManager manager, String fileName, FileHandle file, PFMDataParameter parameter) {
        if (info == null)
            return null;
        return info.data;
    }

    static public class PFMDataInfo {
        String filename;
        PFMData data;
    }

    static public class PFMDataParameter extends AssetLoaderParameters<PFMData> {
        public PFMData data = null;
        public boolean invert = false;
    }

}
