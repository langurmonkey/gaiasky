/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.api;

import gaiasky.render.ComponentTypes;

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
