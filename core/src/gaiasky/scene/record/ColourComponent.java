/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

/**
 * Colour component containing information about a colour
 */
public class ColourComponent {
    public float[] rgba;

    public ColourComponent() {
        rgba = new float[4];
    }

    public ColourComponent(float r, float g, float b, float a) {
        rgba = new float[] { r, g, b, a };
    }

    public ColourComponent(float r, float g, float b) {
        rgba = new float[] { r, g, b, 1f };
    }

    public ColourComponent(String color) {
        String[] rgbs = color.split("\\s+");
        if (rgbs.length == 3) {
            rgba = new float[] { Float.parseFloat(rgbs[0]), Float.parseFloat(rgbs[1]), Float.parseFloat(rgbs[2]), 1f };
        } else if (rgbs.length == 4) {
            rgba = new float[] { Float.parseFloat(rgbs[0]), Float.parseFloat(rgbs[1]), Float.parseFloat(rgbs[2]), Float.parseFloat(rgbs[3]) };
        }
    }
}
