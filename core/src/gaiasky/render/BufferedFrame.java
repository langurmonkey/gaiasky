/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.render;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Pool.Poolable;

public class BufferedFrame implements Poolable {

    public Pixmap pixmap;
    public String folder, filename;

    public BufferedFrame() {
        super();
    }

    public BufferedFrame(Pixmap pixmap, String folder, String file) {
        super();
        this.pixmap = pixmap;
        this.folder = folder;
        this.filename = file;
    }

    @Override
    public void reset() {
        pixmap = null;
        folder = null;
        filename = null;
    }

}
