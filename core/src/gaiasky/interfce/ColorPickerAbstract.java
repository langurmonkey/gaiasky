/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interfce;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.util.CatalogInfo;

public abstract class ColorPickerAbstract extends Image {
    protected Stage stage;
    protected Skin skin;
    protected Runnable newColorRunnable, newColormapRunnable;
    protected float[] color;
    protected String name;
    protected CatalogInfo catalogInfo;

    protected ColorPickerAbstract(String name, CatalogInfo ci, Stage stage, Skin skin) {
        super(skin.getDrawable("white"));
        this.name = name;
        this.skin = skin;
        this.stage = stage;
        this.catalogInfo = ci;
    }

    protected abstract void initialize();
}
