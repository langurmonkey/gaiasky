/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.loader;

public class PFMData {
    public float[] data;
    public int width, height;
    public PFMData(float[] data, int width, int height) {
        this.data = data;
        this.width = width;
        this.height = height;
    }
}
