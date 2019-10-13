/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce;

public class MusicActorsManager {

    private static IMusicActors musicActors;

    public static void initialize(IMusicActors ma) {
        musicActors = ma;
    }

    public static IMusicActors getMusicActors() {
        return musicActors;
    }
}
