/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.components;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.KeyBindings;
import gaiasky.gui.beans.ComboBoxBean;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.util.Constants;
import gaiasky.util.Settings;
import gaiasky.util.TextUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.*;

import java.util.HashMap;
import java.util.Map;

public class VisibilityComponent extends GuiComponent implements IObserver {
    protected Map<String, Button> visibilityButtonMap;
    /**
     * Entities that will go in the visibility check boxes
     */
    private ComponentType[] visibilityEntities;
    private boolean[] visible;
    private CheckBox pmArrowheads;
    private OwnSliderPlus pmNumFactorSlider, pmLenFactorSlider;
    private SelectBox<ComboBoxBean> pmColorMode;
    private Button pmToggleButton;
    private Label pmLabel;
    private VerticalGroup pmColorModeGroup;
    private VerticalGroup pmGroup;
    private boolean sendEvents = true;

    public VisibilityComponent(Skin skin, Stage stage) {
        super(skin, stage);
        EventManager.instance.subscribe(this, Event.TOGGLE_VISIBILITY_CMD, Event.PM_LEN_FACTOR_CMD, Event.PM_NUM_FACTOR_CMD, Event.PM_COLOR_MODE_CMD, Event.PM_ARROWHEADS_CMD);
    }

    public void setVisibilityEntitites(ComponentType[] ve, boolean[] v) {
        visibilityEntities = ve;
        visible = v;
    }

    @Override
    public void initialize(float componentWidth) {
        float space4 = 6.4f;
        float space2 = 3.2f;
        float buttonPadHor = 6f;
        float buttonPadVert = 6f;
        int visTableCols = 5;
        final Table visibilityTable = new Table(skin);

        visibilityTable.setName("visibility table");
        visibilityTable.top().center();
        visibilityButtonMap = new HashMap<>();
        if (visibilityEntities != null) {
            for (int i = 0; i < visibilityEntities.length; i++) {
                final ComponentType ct = visibilityEntities[i];
                final String name = ct.getName();
                if (name != null) {
                    Button button;
                    if (ct.style != null) {
                        Image icon = new Image(skin.getDrawable(ct.style));
                        button = new OwnTextIconButton("", icon, skin, "toggle");
                    } else {
                        button = new OwnTextButton(name, skin, "toggle");
                    }
                    // Name is the key
                    button.setName(ct.key);
                    // Tooltip (with or without hotkey)
                    String hk = KeyBindings.instance.getStringKeys("action.toggle/" + ct.key);
                    if (hk != null) {
                        button.addListener(new OwnTextHotkeyTooltip(TextUtils.capitalise(ct.getName()), hk, skin));
                    } else {
                        button.addListener(new OwnTextTooltip(TextUtils.capitalise(ct.getName()), skin));
                    }
                    // In VR, protect 'Others' component type by disabling it. Otherwise, VR controllers, which are of type 'Others',
                    // may disappear.
                    button.setDisabled(ct.key.equals("element.others") && GaiaSky.instance.isVR());

                    visibilityButtonMap.put(name, button);
                    if (!ct.key.equals(name))
                        visibilityButtonMap.put(ct.key, button);

                    button.setChecked(visible[i]);
                    button.addListener(event -> {
                        if (event instanceof ChangeEvent) {
                            EventManager.publish(Event.TOGGLE_VISIBILITY_CMD, button, ct.key, button.isChecked());
                            return true;
                        }
                        return false;
                    });
                    Cell<Button> c = visibilityTable.add(button).padBottom(buttonPadVert);

                    if ((i + 1) % visTableCols == 0) {
                        visibilityTable.row();
                    } else {
                        c.padRight(buttonPadHor);
                    }
                }
            }
        }

        /* Proper motions */

        // LABEL
        pmLabel = new OwnLabel(I18n.msg("gui.velocityvectors"), skin, "header");

        // ARROWHEADS
        pmArrowheads = new OwnCheckBox(I18n.msg("gui.pm.arrowheads"), skin, space2);
        pmArrowheads.setName("pm arrow caps");
        pmArrowheads.setChecked(Settings.settings.scene.properMotion.arrowHeads);
        pmArrowheads.addListener(event -> {
            if (event instanceof ChangeEvent) {
                if (sendEvents)
                    EventManager.publish(Event.PM_ARROWHEADS_CMD, pmArrowheads, pmArrowheads.isChecked());
                return true;
            }
            return false;
        });

        // NUM FACTOR
        pmNumFactorSlider = new OwnSliderPlus(I18n.msg("gui.pmnumfactor"), Constants.MIN_SLIDER_1, Constants.MAX_SLIDER, Constants.SLIDER_STEP_SMALL, Constants.MIN_PM_NUM_FACTOR, Constants.MAX_PM_NUM_FACTOR, skin);
        pmNumFactorSlider.setName("proper motion vectors number factor");
        pmNumFactorSlider.setWidth(componentWidth);
        pmNumFactorSlider.setMappedValue(Settings.settings.scene.properMotion.number);
        pmNumFactorSlider.addListener(event -> {
            if (event instanceof ChangeEvent) {
                if (sendEvents) {
                    EventManager.publish(Event.PM_NUM_FACTOR_CMD, pmNumFactorSlider, pmNumFactorSlider.getMappedValue());
                }
                return true;
            }
            return false;
        });

        // LEN FACTOR
        pmLenFactorSlider = new OwnSliderPlus(I18n.msg("gui.pmlenfactor"), Constants.MIN_PM_LEN_FACTOR, Constants.MAX_PM_LEN_FACTOR, Constants.SLIDER_STEP_SMALL, skin);
        pmLenFactorSlider.setName("proper motion vectors number factor");
        pmLenFactorSlider.setWidth(componentWidth);
        pmLenFactorSlider.setValue(Settings.settings.scene.properMotion.length);
        pmLenFactorSlider.addListener(event -> {
            if (event instanceof ChangeEvent) {
                if (sendEvents) {
                    EventManager.publish(Event.PM_LEN_FACTOR_CMD, pmLenFactorSlider, pmLenFactorSlider.getValue());
                }
                return true;
            }
            return false;
        });

        // PM COLOR MODE
        Label pmColorModeLabel = new Label(I18n.msg("gui.pm.colormode"), skin, "default");

        ComboBoxBean[] cms = new ComboBoxBean[]{
                new ComboBoxBean(I18n.msg("gui.pm.colormode.dir"), 0),
                new ComboBoxBean(I18n.msg("gui.pm.colormode.speed"), 1),
                new ComboBoxBean(I18n.msg("gui.pm.colormode.hasrv"), 2),
                new ComboBoxBean(I18n.msg("gui.pm.colormode.redshift"), 3),
                new ComboBoxBean(I18n.msg("gui.pm.colormode.redshift.cam"), 4),
                new ComboBoxBean(I18n.msg("gui.pm.colormode.single"), 5)
        };
        pmColorMode = new OwnSelectBox<>(skin);
        pmColorMode.setItems(cms);
        pmColorMode.setWidth(componentWidth);
        pmColorMode.setSelectedIndex(Settings.settings.scene.properMotion.colorMode);
        pmColorMode.setName("proper motion color mode");
        pmColorMode.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                if (sendEvents)
                    EventManager.publish(Event.PM_COLOR_MODE_CMD, pmColorMode, pmColorMode.getSelectedIndex());
                return true;
            }
            return false;
        });
        pmColorModeGroup = new VerticalGroup();
        pmColorModeGroup.space(space2);
        pmColorModeGroup.align(Align.left).columnAlign(Align.left);
        pmColorModeGroup.addActor(pmColorModeLabel);
        pmColorModeGroup.addActor(pmColorMode);

        // PM BUTTON
        pmGroup = new VerticalGroup().align(Align.left).columnAlign(Align.left);
        pmGroup.space(space4);

        pmToggleButton = visibilityButtonMap.get(ComponentType.VelocityVectors.key);
        // Overwrite listeners
        pmToggleButton.addListener(event -> {
            if (event instanceof ChangeEvent) {
                if (pmGroup != null) {
                    velocityVectorsEnabled(pmToggleButton.isChecked());
                }
                return true;
            }
            return false;
        });
        velocityVectorsEnabled(GaiaSky.instance.sceneRenderer.isOn(ComponentType.VelocityVectors));

        // INDIVIDUAL VISIBILITY
        OwnTextIconButton individualVisibility = new OwnTextIconButton(I18n.msg("gui.visibility.individual"), skin, "eye");
        individualVisibility.align(Align.center);
        individualVisibility.setWidth(componentWidth);
        individualVisibility.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.SHOW_PER_OBJECT_VISIBILITY_ACTION, individualVisibility);
                return true;
            }
            return false;
        });

        // Set button width to max width
        Table content = new Table(skin);
        content.add(visibilityTable).center().row();
        content.add(individualVisibility).center().padTop(pad8).row();
        content.add(pmGroup).left().padTop(pad8);

        component = content;
    }

    private void velocityVectorsEnabled(boolean state) {
        if (state) {
            pmGroup.addActor(pmLabel);
            pmGroup.addActor(pmNumFactorSlider);
            pmGroup.addActor(pmLenFactorSlider);
            pmGroup.addActor(pmColorModeGroup);
            pmGroup.addActor(pmArrowheads);
        } else {
            pmGroup.removeActor(pmLabel);
            pmGroup.removeActor(pmNumFactorSlider);
            pmGroup.removeActor(pmLenFactorSlider);
            pmGroup.removeActor(pmColorModeGroup);
            pmGroup.removeActor(pmArrowheads);
        }
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
            case TOGGLE_VISIBILITY_CMD -> {
                String key = (String) data[0];
                Button b = visibilityButtonMap.get(key);
                if (b != null && source != b) {
                    b.setProgrammaticChangeEvents(false);
                    if (data.length == 2) {
                        b.setChecked((Boolean) data[1]);
                    } else {
                        b.setChecked(!b.isChecked());
                    }
                    b.setProgrammaticChangeEvents(true);
                }
            }
            case PM_LEN_FACTOR_CMD -> {
                if (source != pmLenFactorSlider) {
                    sendEvents = false;
                    float value = (Float) data[0];
                    pmLenFactorSlider.setValue(value);
                    sendEvents = true;
                }
            }
            case PM_NUM_FACTOR_CMD -> {
                if (source != pmNumFactorSlider) {
                    sendEvents = false;
                    float value = (Float) data[0];
                    pmNumFactorSlider.setMappedValue(value);
                    sendEvents = true;
                }
            }
            case PM_COLOR_MODE_CMD -> {
                if (source != pmColorMode) {
                    sendEvents = false;
                    pmColorMode.setSelectedIndex((Integer) data[0]);
                    sendEvents = true;
                }
            }
            case PM_ARROWHEADS_CMD -> {
                if (source != pmArrowheads) {
                    sendEvents = false;
                    pmArrowheads.setChecked((boolean) data[0]);
                    sendEvents = true;
                }
            }
            default -> {
            }
        }

    }

    @Override
    public void dispose() {
        EventManager.instance.removeAllSubscriptions(this);
    }
}
