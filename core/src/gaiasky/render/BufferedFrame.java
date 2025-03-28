/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Pool.Poolable;

/**
 * This class is used to store a frame that has been buffered and will be rendered later.
 */
public class BufferedFrame implements Poolable {

    /**
     * The pixmap of the frame.
     */
    public Pixmap pixmap;
    /**
     * The folder where the frame should be saved to.
     */
    public String folder;
    /**
     * The filename for the frame.
     */
    public String filename;

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
