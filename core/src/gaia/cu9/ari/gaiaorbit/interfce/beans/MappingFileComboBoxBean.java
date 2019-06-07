/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce.beans;

import com.badlogic.gdx.files.FileHandle;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.Logger;

import java.io.File;
import java.io.IOException;

public class MappingFileComboBoxBean extends FileComboBoxBean {
    public MappingFileComboBoxBean(FileHandle file) {
        super(file);
        File assetsFolder = new File(GlobalConf.ASSETS_LOC + File.separator);
        try {
            String suffix = file.file().getCanonicalPath().contains(assetsFolder.getCanonicalPath()) ? " [internal]" : " [user]";
            this.name += suffix;
        }catch(IOException e){
            Logger.getLogger(MappingFileComboBoxBean.class.getSimpleName()).error(e);
        }
    }
}
