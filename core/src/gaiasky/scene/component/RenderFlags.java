/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;

public class RenderFlags implements Component {

    /**
     * Whether to render this entity as a quad.
     */
    public boolean renderQuad = true;

    public void setRenderQuad(String renderQuad) {
        this.renderQuad = Boolean.getBoolean(renderQuad);
    }

    public void setRenderQuad(Boolean renderQuad) {
        this.renderQuad = renderQuad;
    }

    public void setRenderquad(Boolean renderQuad) {
        setRenderQuad(renderQuad);
    }
}
