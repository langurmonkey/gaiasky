package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import gaiasky.scene.system.render.draw.billboard.BillboardEntityRenderSystem;
import gaiasky.scene.view.BillboardView;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Consumers.Consumer6;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;

public class Billboard implements Component {

    public Consumer6<BillboardEntityRenderSystem, BillboardView, Float, ExtShaderProgram, IntMesh, ICamera> renderConsumer;

}
