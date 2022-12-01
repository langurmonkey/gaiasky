package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.graphics.GL20;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.GridRecursive;
import gaiasky.scene.entity.GridRecursiveRadio;
import gaiasky.scene.record.ModelComponent;
import gaiasky.scene.system.render.draw.line.LineEntityRenderSystem;
import gaiasky.scene.system.render.draw.model.ModelEntityRenderSystem;
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;
import gaiasky.scene.view.LabelView;
import gaiasky.util.Constants;
import gaiasky.util.Pair;
import gaiasky.util.Settings;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.gdx.shader.attribute.ColorAttribute;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Vector2d;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GridRecInitializer extends AbstractInitSystem {
    public GridRecInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var body = Mapper.body.get(entity);
        var transform = Mapper.transform.get(entity);
        var gr = Mapper.gridRec.get(entity);
        var model = Mapper.model.get(entity);
        var label = Mapper.label.get(entity);
        var line = Mapper.line.get(entity);

        // Labels.
        label.textScale = 1;
        label.labelFactor = 2e-3f;
        label.labelMax = 1f;
        label.label = true;
        label.labelPosition = new Vector3b();
        label.renderConsumer = LabelEntityRenderSystem::renderRecursiveGrid;
        label.renderFunction = LabelView::renderTextGridRec;

        // Lines.
        line.lineWidth = 0.5f;
        line.renderConsumer = LineEntityRenderSystem::renderGridRec;

        transform.floatVersion = true;
        transform.setTransformName(Settings.settings.scene.visibility.get(ComponentType.Galactic.toString()) ? "galacticToEquatorial" : (Settings.settings.scene.visibility.get(ComponentType.Ecliptic.toString()) ? "eclipticToEquatorial" : null));
        gr.scalingFading = new Pair<>(0d, 0d);

        body.color = Settings.settings.scene.visibility.get(ComponentType.Galactic.toString()) ? gr.ccGal : (Settings.settings.scene.visibility.get(ComponentType.Ecliptic.toString()) ? gr.ccEcl : gr.ccEq);
        body.labelColor = body.color;


        gr.p01 = new Vector3d();
        gr.p02 = new Vector3d();
        gr.d01 = -1;
        gr.d02 = -1;
        gr.a = new Vector3d();
        gr.b = new Vector3d();
        gr.c = new Vector3d();
        gr.d = new Vector3d();

        // Init billboard model
        model.renderConsumer = ModelEntityRenderSystem::renderRecursiveGridModel;

        model.model = new ModelComponent();
        model.model.setType("twofacedbillboard");
        Map<String, Object> p = new HashMap<>();
        p.put("diameter", 1d);
        model.model.setParams(p);
        model.model.forceInit = true;
        model.model.initialize(null);
        model.model.env.set(new ColorAttribute(ColorAttribute.AmbientLight, body.color[0], body.color[1], body.color[2], body.color[3]));
        // Depth reads, no depth writes
        model.model.setDepthTest(GL20.GL_LEQUAL, false);

        // Initialize annotations vectorR
        initAnnotations(gr);
    }

    private void initAnnotations(GridRecursive gr) {
        gr.annotations = new ArrayList<>();
        annotation(gr, 1d * Constants.M_TO_U, "1 " + I18n.msg("gui.unit.m"));
        annotation(gr, 50d * Constants.M_TO_U, "50 " + I18n.msg("gui.unit.m"));
        annotation(gr, 100d * Constants.M_TO_U, "100 " + I18n.msg("gui.unit.m"));
        annotation(gr, 200d * Constants.M_TO_U, "200 " + I18n.msg("gui.unit.m"));
        annotation(gr, 500d * Constants.M_TO_U, "500 " + I18n.msg("gui.unit.m"));

        annotation(gr, 1d * Constants.KM_TO_U, "1 " + I18n.msg("gui.unit.km"));
        annotation(gr, 10d * Constants.KM_TO_U, "10 " + I18n.msg("gui.unit.km"));
        annotation(gr, 100d * Constants.KM_TO_U, "100 " + I18n.msg("gui.unit.km"));
        annotation(gr, 250d * Constants.KM_TO_U, "250 " + I18n.msg("gui.unit.km"));
        annotation(gr, 500d * Constants.KM_TO_U, "500 " + I18n.msg("gui.unit.km"));
        annotation(gr, 1000d * Constants.KM_TO_U, "1000 " + I18n.msg("gui.unit.km"));
        annotation(gr, 2500d * Constants.KM_TO_U, "2500 " + I18n.msg("gui.unit.km"));
        annotation(gr, 5000d * Constants.KM_TO_U, "5000 " + I18n.msg("gui.unit.km"));
        annotation(gr, 10000d * Constants.KM_TO_U, "10000 " + I18n.msg("gui.unit.km"));
        annotation(gr, 75000d * Constants.KM_TO_U, "75000 " + I18n.msg("gui.unit.km"));
        annotation(gr, 370000d * Constants.KM_TO_U, "370000 " + I18n.msg("gui.unit.km"));
        annotation(gr, 1500000d * Constants.KM_TO_U, "1.5M " + I18n.msg("gui.unit.km"));
        annotation(gr, 5000000d * Constants.KM_TO_U, "5M " + I18n.msg("gui.unit.km"));
        annotation(gr, 10000000d * Constants.KM_TO_U, "10M " + I18n.msg("gui.unit.km"));

        annotation(gr, 0.1d * Constants.AU_TO_U, "0.1 " + I18n.msg("gui.unit.au"));
        annotation(gr, 0.5d * Constants.AU_TO_U, "0.5 " + I18n.msg("gui.unit.au"));
        annotation(gr, 1d * Constants.AU_TO_U, "1 " + I18n.msg("gui.unit.au"));
        annotation(gr, 2d * Constants.AU_TO_U, "2 " + I18n.msg("gui.unit.au"));
        annotation(gr, 5d * Constants.AU_TO_U, "5 " + I18n.msg("gui.unit.au"));
        annotation(gr, 10d * Constants.AU_TO_U, "10 " + I18n.msg("gui.unit.au"));
        annotation(gr, 50d * Constants.AU_TO_U, "50 " + I18n.msg("gui.unit.au"));
        annotation(gr, 100d * Constants.AU_TO_U, "100 " + I18n.msg("gui.unit.au"));
        annotation(gr, 500d * Constants.AU_TO_U, "500 " + I18n.msg("gui.unit.au"));
        annotation(gr, 1000d * Constants.AU_TO_U, "1000 " + I18n.msg("gui.unit.au"));
        annotation(gr, 5000d * Constants.AU_TO_U, "5000 " + I18n.msg("gui.unit.au"));
        annotation(gr, 10000d * Constants.AU_TO_U, "10000 " + I18n.msg("gui.unit.au"));
        annotation(gr, 50000d * Constants.AU_TO_U, "50000 " + I18n.msg("gui.unit.au"));

        annotation(gr, 1d * Constants.LY_TO_U, "1 " + I18n.msg("gui.unit.ly"));
        annotation(gr, 2d * Constants.LY_TO_U, "2 " + I18n.msg("gui.unit.ly"));

        annotation(gr, 1d * Constants.PC_TO_U, "1 " + I18n.msg("gui.unit.pc"));
        annotation(gr, 2.5d * Constants.PC_TO_U, "2.5 " + I18n.msg("gui.unit.pc"));
        annotation(gr, 5d * Constants.PC_TO_U, "5 " + I18n.msg("gui.unit.pc"));
        annotation(gr, 10d * Constants.PC_TO_U, "10 " + I18n.msg("gui.unit.pc"));
        annotation(gr, 25d * Constants.PC_TO_U, "25 " + I18n.msg("gui.unit.pc"));
        annotation(gr, 50d * Constants.PC_TO_U, "50 " + I18n.msg("gui.unit.pc"));
        annotation(gr, 100d * Constants.PC_TO_U, "100 " + I18n.msg("gui.unit.pc"));
        annotation(gr, 250d * Constants.PC_TO_U, "250 " + I18n.msg("gui.unit.pc"));
        annotation(gr, 500d * Constants.PC_TO_U, "500 " + I18n.msg("gui.unit.pc"));

        annotation(gr, 1000d * Constants.PC_TO_U, "1 " + I18n.msg("gui.unit.kpc"));
        annotation(gr, 2500d * Constants.PC_TO_U, "2.5 " + I18n.msg("gui.unit.kpc"));
        annotation(gr, 5000d * Constants.PC_TO_U, "5 " + I18n.msg("gui.unit.kpc"));
        annotation(gr, 10000d * Constants.PC_TO_U, "10 " + I18n.msg("gui.unit.kpc"));
        annotation(gr, 25000d * Constants.PC_TO_U, "25 " + I18n.msg("gui.unit.kpc"));
        annotation(gr, 50000d * Constants.PC_TO_U, "50 " + I18n.msg("gui.unit.kpc"));
        annotation(gr, 100000d * Constants.PC_TO_U, "100 " + I18n.msg("gui.unit.kpc"));
        annotation(gr, 250000d * Constants.PC_TO_U, "250 " + I18n.msg("gui.unit.kpc"));
        annotation(gr, 500000d * Constants.PC_TO_U, "500 " + I18n.msg("gui.unit.kpc"));

        annotation(gr, 1000000d * Constants.PC_TO_U, "1 " + I18n.msg("gui.unit.mpc"));
        annotation(gr, 2500000d * Constants.PC_TO_U, "2.5 " + I18n.msg("gui.unit.mpc"));
        annotation(gr, 5000000d * Constants.PC_TO_U, "5 " + I18n.msg("gui.unit.mpc"));
        annotation(gr, 10000000d * Constants.PC_TO_U, "10 " + I18n.msg("gui.unit.mpc"));
        annotation(gr, 25000000d * Constants.PC_TO_U, "25 " + I18n.msg("gui.unit.mpc"));
        annotation(gr, 50000000d * Constants.PC_TO_U, "50 " + I18n.msg("gui.unit.mpc"));
        annotation(gr, 100000000d * Constants.PC_TO_U, "100 " + I18n.msg("gui.unit.mpc"));
        annotation(gr, 500000000d * Constants.PC_TO_U, "500 " + I18n.msg("gui.unit.mpc"));

        annotation(gr, 1000000000d * Constants.PC_TO_U, "1 " + I18n.msg("gui.unit.gpc"));
        annotation(gr, 2500000000d * Constants.PC_TO_U, "2.5 " + I18n.msg("gui.unit.gpc"));
        annotation(gr, 5000000000d * Constants.PC_TO_U, "5 " + I18n.msg("gui.unit.gpc"));
        annotation(gr, 10000000000d * Constants.PC_TO_U, "10 " + I18n.msg("gui.unit.gpc"));
        annotation(gr, 50000000000d * Constants.PC_TO_U, "50 " + I18n.msg("gui.unit.gpc"));
        annotation(gr, 100000000000d * Constants.PC_TO_U, "100 " + I18n.msg("gui.unit.gpc"));
    }

    private void annotation(GridRecursive gr, double dist, String text) {
        gr.annotations.add(new Pair<>(dist, text));
    }

    @Override
    public void setUpEntity(Entity entity) {
        var body = Mapper.body.get(entity);
        var graph = Mapper.graph.get(entity);
        var model = Mapper.model.get(entity);

        // Model
        model.model.doneLoading(GaiaSky.instance.assetManager, graph.localTransform, body.color);
        model.model.setColorAttribute(ColorAttribute.Emissive, ColorUtils.getRgbaComplimentary(body.color));

        // Listen
        EventManager.instance.subscribe(new GridRecursiveRadio(entity), Event.TOGGLE_VISIBILITY_CMD);

        // Fade out in VR
        if (Settings.settings.runtime.openVr) {
            var fade = Mapper.fade.get(entity);
            fade.setFadeOut(new double[] { 5e6, 50e6 });
            fade.fadeOutMap = new Vector2d(1, 0);
        }

    }
}
