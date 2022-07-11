package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.RenderingContext;
import gaiasky.render.system.FontRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.entity.TitleRadio;
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;
import gaiasky.scene.view.LabelView;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.gdx.g2d.ExtSpriteBatch;
import gaiasky.util.gdx.shader.ExtShaderProgram;

public class TitleInitializer extends AbstractInitSystem {
    public TitleInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var body = Mapper.body.get(entity);
        var label = Mapper.label.get(entity);

        label.renderConsumer = LabelEntityRenderSystem::renderTitle;

        EventManager.instance.subscribe(new TitleRadio(entity), Event.UI_THEME_RELOAD_INFO);

        LabelStyle headerStyle = GaiaSky.instance.getGlobalResources().getSkin().get("header", LabelStyle.class);
        body.labelColor[0] = headerStyle.fontColor.r;
        body.labelColor[1] = headerStyle.fontColor.g;
        body.labelColor[2] = headerStyle.fontColor.b;
    }

    @Override
    public void setUpEntity(Entity entity) {

    }
}
