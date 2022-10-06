/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.comp;

import com.badlogic.ashley.core.Entity;
import gaiasky.scene.view.FocusView;

import java.util.Comparator;

/**
 * Compares entities depending on the solid angle from the camera.
 */
public class ViewAngleComparator<T> implements Comparator<T> {

    private final FocusView view1 = new FocusView();
    private final FocusView view2 = new FocusView();

    @Override
    public int compare(T o1, T o2) {
        view1.setEntity((Entity) o1);
        view2.setEntity((Entity) o2);
        return Double.compare(view1.getCandidateSolidAngleApparent(), view2.getCandidateSolidAngleApparent());
    }

}
