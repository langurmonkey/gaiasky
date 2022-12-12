/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.api;

import gaiasky.render.ComponentTypes;

/**
 * This interface must be implemented by all objects whose visibility
 * can be manipulated using the per-object visibility controls.
 */
public interface IVisibilitySwitch {
    String getName();

    void setName(String name);

    String getDescription();

    void setDescription(String description);

    boolean isVisible();

    void setVisible(boolean visible);

    boolean isVisible(String name);

    void setVisible(boolean visible, String name);

    boolean isVisible(boolean attributeValue);

    boolean hasCt(ComponentTypes.ComponentType ct);
}
