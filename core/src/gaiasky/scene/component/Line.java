/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.system.render.draw.LinePrimitiveRenderer;
import gaiasky.scene.system.render.draw.line.LineEntityRenderSystem;
import gaiasky.util.Consumers.Consumer5;

public class Line implements Component {

    public float lineWidth;

    /** The line rendering code. **/
    public Consumer5<LineEntityRenderSystem, Entity, LinePrimitiveRenderer, ICamera, Float> renderConsumer;

}
