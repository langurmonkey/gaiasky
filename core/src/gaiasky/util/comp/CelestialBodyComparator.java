/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.comp;

import gaiasky.scene.api.IFocus;

import java.util.Comparator;

public class CelestialBodyComparator implements Comparator<IFocus> {

    @Override
    public int compare(IFocus a, IFocus b) {
        return b.getName().compareTo(a.getName());
    }

}
