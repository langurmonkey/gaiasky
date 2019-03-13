package gaia.cu9.ari.gaiaorbit.interfce.components;

import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.render.ComponentType;
import gaia.cu9.ari.gaiaorbit.util.Constants;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.TextUtils;
import gaia.cu9.ari.gaiaorbit.util.math.MathUtilsd;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnLabel;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnTextButton;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnTextIconButton;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class VisibilityComponent extends GuiComponent implements IObserver {
    protected Map<String, Button> buttonMap;
    /**
     * Entities that will go in the visibility check boxes
     */
    private ComponentType[] visibilityEntities;
    private boolean[] visible;
    private CheckBox properMotions;
    private Slider pmNumFactorSlider, pmLenFactorSlider;
    private Label pmNumFactor, pmLenFactor, pmNumFactorLabel, pmLenFactorLabel;
    private VerticalGroup pmNumFactorGroup, pmLenFactorGroup;
    private VerticalGroup pmGroup;
    private boolean sendEvents = true;

    public VisibilityComponent(Skin skin, Stage stage) {
        super(skin, stage);
        EventManager.instance.subscribe(this, Events.TOGGLE_VISIBILITY_CMD, Events.PROPER_MOTIONS_CMD, Events.PM_LEN_FACTOR_CMD, Events.PM_NUM_FACTOR_CMD);
    }

    public void setVisibilityEntitites(ComponentType[] ve, boolean[] v) {
        visibilityEntities = ve;
        visible = v;
    }

    public void initialize() {
        int visTableCols = 5;
        final Table visibilityTable = new Table(skin);
        visibilityTable.setName("visibility table");
        visibilityTable.top().left();
        buttonMap = new HashMap<String, Button>();
        Set<Button> buttons = new HashSet<Button>();
        if (visibilityEntities != null) {
            for (int i = 0; i < visibilityEntities.length; i++) {
                final ComponentType ct = visibilityEntities[i];
                final String name = ct.getName();
                if (name != null) {
                    Button button = null;
                    if (ct.style != null) {
                        Image icon = new Image(skin.getDrawable(ct.style));
                        button = new OwnTextIconButton("", icon, skin, "toggle");
                    } else {
                        button = new OwnTextButton(name, skin, "toggle");
                    }
                    button.setName(name);
                    // Tooltip
                    button.addListener(new TextTooltip(TextUtils.capitalise(name), skin));

                    buttonMap.put(name, button);
                    if (!ct.key.equals(name))
                        buttonMap.put(ct.key, button);

                    button.setChecked(visible[i]);
                    button.addListener(new EventListener() {
                        @Override
                        public boolean handle(Event event) {
                            if (event instanceof ChangeEvent) {
                                EventManager.instance.post(Events.TOGGLE_VISIBILITY_CMD, ct.key, true, ((Button) event.getListenerActor()).isChecked());
                                return true;
                            }
                            return false;
                        }
                    });
                    visibilityTable.add(button).pad(GlobalConf.SCALE_FACTOR).left();
                    if ((i + 1) % visTableCols == 0) {
                        visibilityTable.row().padBottom(2 * GlobalConf.SCALE_FACTOR);
                    }
                    buttons.add(button);
                }
            }
        }

        /** Proper motions **/
        float space3 = 3 * GlobalConf.SCALE_FACTOR;

        // NUM FACTOR
        pmNumFactorLabel = new Label(txt("gui.pmnumfactor"), skin, "default");
        pmNumFactor = new OwnLabel(Integer.toString((int) (MathUtilsd.lint(GlobalConf.scene.PM_NUM_FACTOR, Constants.MIN_PM_NUM_FACTOR, Constants.MAX_PM_NUM_FACTOR, Constants.MIN_SLIDER_1, Constants.MAX_SLIDER))), skin);

        pmNumFactorSlider = new Slider(Constants.MIN_SLIDER_1, Constants.MAX_SLIDER, 1, false, skin);
        pmNumFactorSlider.setName("proper motion vectors number factor");
        pmNumFactorSlider.setValue(MathUtilsd.lint(GlobalConf.scene.PM_NUM_FACTOR, Constants.MIN_PM_NUM_FACTOR, Constants.MAX_PM_NUM_FACTOR, Constants.MIN_SLIDER_1, Constants.MAX_SLIDER));
        pmNumFactorSlider.addListener(event -> {
            if (event instanceof ChangeEvent) {
                if (sendEvents) {
                    EventManager.instance.post(Events.PM_NUM_FACTOR_CMD, MathUtilsd.lint(pmNumFactorSlider.getValue(), Constants.MIN_SLIDER_1, Constants.MAX_SLIDER, Constants.MIN_PM_NUM_FACTOR, Constants.MAX_PM_NUM_FACTOR), true);
                    pmNumFactor.setText(Integer.toString((int) pmNumFactorSlider.getValue()));
                }
                return true;
            }
            return false;
        });

        pmNumFactorGroup = new VerticalGroup();
        pmNumFactorGroup.align(Align.left).columnAlign(Align.left);
        HorizontalGroup pnfg = new HorizontalGroup();
        pnfg.space(space3);
        pnfg.addActor(pmNumFactorSlider);
        pnfg.addActor(pmNumFactor);
        pmNumFactorGroup.addActor(pmNumFactorLabel);
        pmNumFactorGroup.addActor(pnfg);

        // LEN FACTOR
        pmLenFactorLabel = new Label(txt("gui.pmlenfactor"), skin, "default");
        pmLenFactor = new OwnLabel(Integer.toString(Math.round(GlobalConf.scene.PM_LEN_FACTOR)), skin);

        pmLenFactorSlider = new Slider(Constants.MIN_PM_LEN_FACTOR, Constants.MAX_PM_LEN_FACTOR, 0.5f, false, skin);
        pmLenFactorSlider.setName("proper motion vectors number factor");
        pmLenFactorSlider.setValue(GlobalConf.scene.PM_LEN_FACTOR);
        pmLenFactorSlider.addListener(event -> {
            if (event instanceof ChangeEvent) {
                if (sendEvents) {
                    EventManager.instance.post(Events.PM_LEN_FACTOR_CMD, pmLenFactorSlider.getValue(), true);
                    pmLenFactor.setText(Integer.toString(Math.round(pmLenFactorSlider.getValue())));
                }
                return true;
            }
            return false;
        });
        pmLenFactorGroup = new VerticalGroup();
        pmLenFactorGroup.align(Align.left).columnAlign(Align.left);
        HorizontalGroup plfg = new HorizontalGroup();
        plfg.space(space3);
        plfg.addActor(pmLenFactorSlider);
        plfg.addActor(pmLenFactor);
        pmLenFactorGroup.addActor(pmLenFactorLabel);
        pmLenFactorGroup.addActor(plfg);

        // PM CHECKBOX
        pmGroup = new VerticalGroup().align(Align.left).columnAlign(Align.left);
        properMotions = new CheckBox(" " + txt("gui.checkbox.propermotionvectors"), skin);
        properMotions.setName("pm vectors");
        properMotions.addListener(event -> {
            if (event instanceof ChangeEvent) {
                if (sendEvents)
                    EventManager.instance.post(Events.PROPER_MOTIONS_CMD, "Proper motions", properMotions.isChecked());
                if (pmGroup != null) {
                    if (properMotions.isChecked()) {
                        pmGroup.addActor(pmNumFactorGroup);
                        pmGroup.addActor(pmLenFactorGroup);
                    } else {
                        pmGroup.removeActor(pmNumFactorGroup);
                        pmGroup.removeActor(pmLenFactorGroup);
                    }
                }
                return true;
            }
            return false;
        });

        pmGroup.addActor(properMotions);

        // Set button width to max width
        visibilityTable.pack();
        //        for (Button b : buttons) {
        //            b.setSize(25 * GlobalConf.SCALE_FACTOR, 25 * GlobalConf.SCALE_FACTOR);
        //        }

        visibilityTable.row().padBottom(3 * GlobalConf.SCALE_FACTOR);
        visibilityTable.add(pmGroup).padTop(3 * GlobalConf.SCALE_FACTOR).align(Align.left).colspan(visTableCols);

        properMotions.setChecked(GlobalConf.scene.PROPER_MOTION_VECTORS);

        component = visibilityTable;
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
            case TOGGLE_VISIBILITY_CMD:
                boolean interf = (Boolean) data[1];
                if (!interf) {
                    String key = (String) data[0];
                    Button b = buttonMap.get(key);

                    if (b != null) {
                        b.setProgrammaticChangeEvents(false);
                        if (data.length == 3) {
                            b.setChecked((Boolean) data[2]);
                        } else {
                            b.setChecked(!b.isChecked());
                        }
                        b.setProgrammaticChangeEvents(true);
                    }
                }
                break;
            case PROPER_MOTIONS_CMD:
                String key = (String) data[0];
                if (key.equals("element.propermotions")) {
                    sendEvents = false;
                    properMotions.setChecked((Boolean) data[1]);
                    sendEvents = true;
                }
                break;
            case PM_LEN_FACTOR_CMD:
                interf = (Boolean) data[1];
                if (!interf) {
                    sendEvents = false;
                    float value = (Float) data[0];
                    pmLenFactorSlider.setValue(value);
                    pmLenFactorLabel.setText(Integer.toString(Math.round(value)));
                    sendEvents = true;
                }
                break;
            case PM_NUM_FACTOR_CMD:
                interf = (Boolean) data[1];
                if (!interf) {
                    sendEvents = false;
                    float value = (Float) data[0];
                    float val = MathUtilsd.lint(value, Constants.MIN_PM_NUM_FACTOR, Constants.MAX_PM_NUM_FACTOR, Constants.MIN_SLIDER_1, Constants.MAX_SLIDER);
                    pmNumFactorSlider.setValue(val);
                    pmNumFactor.setText(Integer.toString((int) val));
                    sendEvents = true;
                }
                break;
            default:
                break;
        }

    }

    @Override
    public void dispose() {
        EventManager.instance.removeAllSubscriptions(this);
    }
}
