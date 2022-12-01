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
