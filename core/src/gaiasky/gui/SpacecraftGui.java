/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import gaiasky.event.EventManager;
import gaiasky.scene.camera.CameraManager.CameraMode;
import gaiasky.scene.component.MotorEngine;
import gaiasky.scene.record.MachineDefinition;
import gaiasky.scene.view.SpacecraftView;
import gaiasky.util.*;
import gaiasky.util.gdx.IntModelBatch;
import gaiasky.util.gdx.IntModelBuilder;
import gaiasky.util.gdx.g3d.decals.CameraGroupStrategy;
import gaiasky.util.gdx.model.IntModel;
import gaiasky.util.gdx.model.IntModelInstance;
import gaiasky.util.gdx.shader.Environment;
import gaiasky.util.gdx.shader.Material;
import gaiasky.util.gdx.shader.attribute.ColorAttribute;
import gaiasky.util.gdx.shader.attribute.DepthTestAttribute;
import gaiasky.util.gdx.shader.attribute.TextureAttribute;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Vector3d;
import gaiasky.util.scene2d.*;

import java.text.DecimalFormat;

public class SpacecraftGui extends AbstractGui {

    // Number format
    private final DecimalFormat nf;
    private final DecimalFormat sf;
    // Auxiliary vectors
    private final Vector3 aux3f1;
    private final Vector3 aux3f2;
    private Container<HorizontalGroup> buttonContainer;
    private Container<Label> thrustContainer;
    private HorizontalGroup buttonRow, engineGroup;
    private VerticalGroup thrustGroup;
    private Table main, motionGroup, nearestGroup, controlsGroup;
    private OwnImageButton stabilise, stop, exit, enginePlus, engineMinus;
    private Slider enginePower;
    private Slider thrustv, thrusty, thrustp, thrustr;
    private Slider thrustvm, thrustym, thrustpm, thrustrm;
    private OwnLabel mainvel, yawvel, pitchvel, rollvel, closestname, closestdist, thrustfactor;
    private CheckBox velToDir;
    private SelectBox<MachineDefinition> machineSelector;
    // The spacecraft object
    private Entity sc;
    private final SpacecraftView view;
    // Camera to render the attitude indicator system
    private PerspectiveCamera aiCam;
    // Attitude indicator
    private IntModelBatch mb;
    private DecalBatch db;
    private SpriteBatch sb;
    private IntModel aiModel;
    private IntModelInstance aiModelInstance;
    private Texture aiTexture, aiPointerTexture, aiVelTex, aiAntivelTex;
    private Decal aiVelDec, aiAntivelDec;
    private Environment env;
    private Matrix4 aiTransform;
    private Viewport aiViewport;
    private DirectionalLight dlight;
    // Reference to spacecraft camera rotation quaternion
    private Quaternion qf;
    // Reference to spacecraft camera velocity vector
    private Vector3d vel;
    private float indicatorw, indicatorh, indicatorx, indicatory;
    private boolean thrustEvents = true;

    public SpacecraftGui(final Skin skin, final Graphics graphics, final Float unitsPerPixel) {
        super(graphics, unitsPerPixel);
        this.skin = skin;
        aux3f1 = new Vector3();
        aux3f2 = new Vector3();
        view = new SpacecraftView();

        nf = new DecimalFormat("##0.##");
        sf = new DecimalFormat("#0.###E0");
    }

    public void initialize(AssetManager assetManager, SpriteBatch sb) {
        this.sb = sb;
        // User interface
        ScreenViewport vp = new ScreenViewport();
        vp.setUnitsPerPixel(unitsPerPixel);
        stage = new Stage(vp, sb);

        indicatorw = 480f;
        indicatorh = 480f;
        indicatorx = -32f;
        indicatory = -20f;

        // init gui camera
        aiCam = new PerspectiveCamera(30, indicatorw * Settings.settings.program.ui.scale, indicatorh * Settings.settings.program.ui.scale);
        aiCam.near = (float) (1e5 * Constants.KM_TO_U);
        aiCam.far = (float) (1e8 * Constants.KM_TO_U);
        aiCam.up.set(0, 1, 0);
        aiCam.direction.set(0, 0, 1);
        aiCam.position.set(0, 0, 0);

        // Init AI
        dlight = new DirectionalLight();
        dlight.color.set(1f, 1f, 1f, 1f);
        dlight.setDirection(-1f, .05f, .5f);
        env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 1f, 1f, 1f, 1f), new ColorAttribute(ColorAttribute.Specular, .5f, .5f, .5f, 1f));
        env.add(dlight);
        db = new DecalBatch(4, new CameraGroupStrategy(aiCam));
        mb = new IntModelBatch();

        assetManager.load(Settings.settings.data.dataFile(Constants.DATA_LOCATION_TOKEN + "tex/base/attitudeindicator.png"), Texture.class);
        assetManager.load("img/ai-pointer.png", Texture.class);
        assetManager.load("img/ai-vel.png", Texture.class);
        assetManager.load("img/ai-antivel.png", Texture.class);

        EventManager.instance.subscribe(this, gaiasky.event.Event.SPACECRAFT_LOADED);
    }

    /**
     * Constructs the interface
     */
    public void doneLoading(AssetManager assetManager) {
        aiTexture = assetManager.get(Settings.settings.data.dataFile(Constants.DATA_LOCATION_TOKEN + "tex/base/attitudeindicator.png"), Texture.class);
        aiTexture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        aiPointerTexture = assetManager.get("img/ai-pointer.png", Texture.class);
        aiPointerTexture.setFilter(TextureFilter.Linear, TextureFilter.Linear);

        aiVelTex = assetManager.get("img/ai-vel.png", Texture.class);
        aiVelTex.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        aiAntivelTex = assetManager.get("img/ai-antivel.png", Texture.class);
        aiAntivelTex.setFilter(TextureFilter.Linear, TextureFilter.Linear);

        aiVelDec = Decal.newDecal(new TextureRegion(aiVelTex));
        aiAntivelDec = Decal.newDecal(new TextureRegion(aiAntivelTex));

        Material mat = new Material(new TextureAttribute(TextureAttribute.Diffuse, aiTexture), new ColorAttribute(ColorAttribute.Specular, 0.3f, 0.3f, 0.3f, 1f), new DepthTestAttribute(GL20.GL_LESS, aiCam.near, aiCam.far, true));
        aiModel = new IntModelBuilder().createSphere(1.6f, 30, 30, false, mat, Bits.indexes(Usage.Position, Usage.Normal, Usage.Tangent, Usage.BiNormal, Usage.TextureCoordinates));
        aiTransform = new Matrix4();
        aiModelInstance = new IntModelInstance(aiModel, aiTransform);
        aiViewport = new ExtendViewport(indicatorw * Settings.settings.program.ui.scale, indicatorh * Settings.settings.program.ui.scale, aiCam);

        buildGui();

        EventManager.instance.subscribe(this, gaiasky.event.Event.SPACECRAFT_STABILISE_CMD, gaiasky.event.Event.SPACECRAFT_STOP_CMD, gaiasky.event.Event.SPACECRAFT_INFO, gaiasky.event.Event.SPACECRAFT_NEAREST_INFO, gaiasky.event.Event.SPACECRAFT_THRUST_INFO);
        EventManager.instance.unsubscribe(this, gaiasky.event.Event.SPACECRAFT_LOADED);

    }

    private void buildGui() {

        // BUTTONS
        buttonContainer = new Container<>();
        buttonRow = new HorizontalGroup();
        buttonRow.space(4.8f);
        buttonRow.setFillParent(true);
        buttonRow.align(Align.bottomLeft);

        stabilise = new OwnImageButton(skin, "sc-stabilise");
        stabilise.setProgrammaticChangeEvents(false);
        stabilise.setName("stabilise");
        if (sc != null)
            stabilise.setChecked(view.isStabilising());
        stabilise.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(gaiasky.event.Event.SPACECRAFT_STABILISE_CMD, stabilise, stabilise.isChecked());
                return true;
            }
            return false;
        });
        stabilise.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.sc.stabilise"), skin));

        stop = new OwnImageButton(skin, "sc-stop");
        stop.setProgrammaticChangeEvents(false);
        stop.setName("stop spacecraft");
        if (sc != null)
            stop.setChecked(view.isStopping());
        stop.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(gaiasky.event.Event.SPACECRAFT_STOP_CMD, stop, stop.isChecked());
                return true;
            }
            return false;
        });
        stop.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.sc.stop"), skin));

        exit = new OwnImageButton(skin, "sc-exit");
        exit.setProgrammaticChangeEvents(false);
        exit.setName("exit spacecraft");
        exit.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(gaiasky.event.Event.CAMERA_MODE_CMD, exit, CameraMode.FOCUS_MODE);
                return true;
            }
            return false;
        });
        exit.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.sc.exit"), skin));

        buttonRow.addActor(stabilise);
        buttonRow.addActor(stop);
        buttonRow.addActor(exit);

        buttonContainer.setActor(buttonRow);

        buttonContainer.pack();

        // ENGINE GROUP
        engineGroup = new HorizontalGroup();
        engineGroup.space(0.8f);
        engineGroup.align(Align.bottomLeft);

        // Engine controls
        float enginePowerH = 380f;
        Table engineControls = new Table(skin);
        engineControls.pad(0f);

        enginePlus = new OwnImageButton(skin, "sc-engine-power-up");
        enginePlus.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.sc.powerup"), skin));
        enginePlus.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(gaiasky.event.Event.SPACECRAFT_THRUST_INCREASE_CMD, enginePlus);
                return true;
            }
            return false;
        });
        engineMinus = new OwnImageButton(skin, "sc-engine-power-down");
        enginePlus.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.sc.powerdown"), skin));
        engineMinus.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(gaiasky.event.Event.SPACECRAFT_THRUST_DECREASE_CMD, engineMinus);
                return true;
            }
            return false;
        });

        Group engineLabelRotated = new Group();
        Label engineLabel = new OwnLabel(I18n.msg("gui.sc.enginepower"), skin);
        engineLabelRotated.addActor(engineLabel);
        float engineLabelH = enginePowerH - enginePlus.getHeight() - engineMinus.getHeight() - 2;
        engineLabelRotated.addAction(Actions.rotateBy(-90));
        engineLabelRotated.addAction(Actions.moveBy(-5, (((engineLabelH - enginePlus.getHeight()) - engineLabel.getWidth()) / 2f + engineLabel.getWidth())));
        engineLabelRotated.setHeight(engineLabelH);

        engineControls.add(enginePlus);
        engineControls.row();
        engineControls.add(engineLabelRotated);
        engineControls.row();
        engineControls.add(engineMinus);

        // Power slider - The value of the slider is the index of the thrustFactor array 
        enginePower = new OwnSlider(0, MotorEngine.thrustFactor.length - 1, 1, true, skin, "sc-engine");
        enginePower.setName("engine power slider");
        enginePower.setValue(0);
        enginePower.setHeight(enginePowerH);
        enginePower.addListener(event -> {
            if (thrustEvents)
                if (event instanceof ChangeEvent) {
                    EventManager.publish(gaiasky.event.Event.SPACECRAFT_THRUST_SET_CMD, enginePower, Math.round(enginePower.getValue()));
                    return true;
                }
            return false;
        });

        engineGroup.addActor(enginePower);
        engineGroup.addActor(engineControls);

        engineGroup.pack();

        // CONTROLS
        controlsGroup = new Table(skin);
        controlsGroup.align(Align.topLeft);

        // Spaceship selector
        machineSelector = new OwnSelectBox<>(skin);
        machineSelector.setItems(view.getMachines());
        machineSelector.setSelected(view.getMachines()[view.getCurrentMachine()]);
        machineSelector.addListener(event -> {
            if (event instanceof ChangeEvent) {
                int machineIndex = machineSelector.getSelectedIndex();
                EventManager.publish(gaiasky.event.Event.SPACECRAFT_MACHINE_SELECTION_CMD, machineSelector, machineIndex);
                return true;
            }
            return false;
        });

        // Whether to keep the velocity pointing in the direction vector
        velToDir = new OwnCheckBox(I18n.msg("gui.sc.veltodir"), skin, 16f);
        velToDir.setName("sc veltodir");
        velToDir.setChecked(Settings.settings.spacecraft.velocityDirection);
        velToDir.addListener(event -> {
            if (event instanceof ChangeEvent) {
                Settings.settings.spacecraft.velocityDirection = velToDir.isChecked();
            }
            return false;
        });

        controlsGroup.add(new OwnLabel(I18n.msg("gui.sc.spaceship"), skin, "sc-header")).left().padRight(16f).padBottom(8f);
        controlsGroup.add(machineSelector).left().padBottom(8f).row();
        controlsGroup.add(velToDir).left().colspan(2);
        controlsGroup.pack();

        // INFORMATION
        float groupspacing = 10f;
        thrustfactor = new OwnLabel("", skin);
        thrustContainer = new Container<>(thrustfactor);

        float labelWidth = 110f;
        float valueWidth = 150f;

        mainvel = new OwnLabel("", skin);
        mainvel.setWidth(valueWidth);
        HorizontalGroup mvg = new HorizontalGroup();
        mvg.space(groupspacing);
        Label speed = new OwnLabel(I18n.msg("gui.sc.velocity") + ":", skin, "sc-header");
        speed.setWidth(labelWidth);
        mvg.addActor(speed);
        mvg.addActor(mainvel);
        yawvel = new OwnLabel("", skin);
        yawvel.setWidth(valueWidth);
        HorizontalGroup yvg = new HorizontalGroup();
        yvg.space(groupspacing);
        Label yaw = new OwnLabel(I18n.msg("gui.sc.yaw") + ":", skin, "sc-header");
        yaw.setWidth(labelWidth);
        yvg.addActor(yaw);
        yvg.addActor(yawvel);
        pitchvel = new OwnLabel("", skin);
        pitchvel.setWidth(valueWidth);
        HorizontalGroup pvg = new HorizontalGroup();
        pvg.space(groupspacing);
        Label pitch = new OwnLabel(I18n.msg("gui.sc.pitch") + ":", skin, "sc-header");
        pitch.setWidth(labelWidth);
        pvg.addActor(pitch);
        pvg.addActor(pitchvel);
        rollvel = new OwnLabel("", skin);
        rollvel.setWidth(valueWidth);
        HorizontalGroup rvg = new HorizontalGroup();
        rvg.space(groupspacing);
        Label roll = new OwnLabel(I18n.msg("gui.sc.roll") + ":", skin, "sc-header");
        roll.setWidth(labelWidth);
        rvg.addActor(roll);
        rvg.addActor(rollvel);

        motionGroup = new Table(skin);
        motionGroup.align(Align.topLeft);

        motionGroup.add(mvg).left().row();
        motionGroup.add(yvg).left().row();
        motionGroup.add(pvg).left().row();
        motionGroup.add(rvg).left();

        motionGroup.pack();

        // NEAREST
        nearestGroup = new Table(skin);
        nearestGroup.align(Align.topLeft);

        closestname = new OwnLabel("", skin);
        closestdist = new OwnLabel("", skin);
        HorizontalGroup cng = new HorizontalGroup();
        cng.align(Align.left);
        cng.space(groupspacing);
        cng.addActor(new OwnLabel(I18n.msg("gui.sc.nearest") + ":", skin, "sc-header"));
        cng.addActor(closestname);

        HorizontalGroup cdg = new HorizontalGroup();
        cdg.align(Align.left);
        cdg.space(groupspacing);
        cdg.addActor(new OwnLabel(I18n.msg("gui.sc.distance") + ":", skin, "sc-header"));
        cdg.addActor(closestdist);

        nearestGroup.add(cng).left().row();
        nearestGroup.add(cdg).left();

        nearestGroup.pack();

        // THRUST INDICATORS for VEL, YAW, PITCH, ROLL
        float thrustHeight = 96f;
        float thrustWidth = 16f;
        thrustGroup = new VerticalGroup();
        thrustGroup.space(1.6f);

        HorizontalGroup thrustPlus = new HorizontalGroup().space(1f);
        HorizontalGroup thrustMinus = new HorizontalGroup().space(1f);

        thrustv = new OwnSlider(0, 1, 0.05f, true, skin, "sc-thrust");
        thrustv.setHeight(thrustHeight);
        thrustv.setWidth(thrustWidth);
        thrustv.setDisabled(true);
        thrustvm = new OwnSlider(0, 1, 0.05f, true, skin, "sc-thrust-minus");
        thrustvm.setHeight(thrustHeight);
        thrustvm.setWidth(thrustWidth);
        thrustvm.setDisabled(true);

        thrusty = new OwnSlider(0, 1, 0.05f, true, skin, "sc-thrust");
        thrusty.setHeight(thrustHeight);
        thrusty.setWidth(thrustWidth);
        thrusty.setDisabled(true);
        thrustym = new OwnSlider(0, 1, 0.05f, true, skin, "sc-thrust-minus");
        thrustym.setHeight(thrustHeight);
        thrustym.setWidth(thrustWidth);
        thrustym.setDisabled(true);

        thrustp = new OwnSlider(0, 1, 0.05f, true, skin, "sc-thrust");
        thrustp.setHeight(thrustHeight);
        thrustp.setWidth(thrustWidth);
        thrustp.setDisabled(true);
        thrustpm = new OwnSlider(0, 1, 0.05f, true, skin, "sc-thrust-minus");
        thrustpm.setHeight(thrustHeight);
        thrustpm.setWidth(thrustWidth);
        thrustpm.setDisabled(true);

        thrustr = new OwnSlider(0, 1, 0.05f, true, skin, "sc-thrust");
        thrustr.setHeight(thrustHeight);
        thrustr.setWidth(thrustWidth);
        thrustr.setDisabled(true);
        thrustrm = new OwnSlider(0, 1, 0.05f, true, skin, "sc-thrust-minus");
        thrustrm.setHeight(thrustHeight);
        thrustrm.setWidth(thrustWidth);
        thrustrm.setDisabled(true);

        thrustPlus.addActor(thrustv);
        thrustMinus.addActor(thrustvm);

        thrustPlus.addActor(thrusty);
        thrustMinus.addActor(thrustym);

        thrustPlus.addActor(thrustp);
        thrustMinus.addActor(thrustpm);

        thrustPlus.addActor(thrustr);
        thrustMinus.addActor(thrustrm);

        thrustGroup.addActor(thrustPlus);
        thrustGroup.addActor(thrustMinus);

        thrustGroup.pack();

        Table motionThrustGroup = new Table(skin);
        motionThrustGroup.add(motionGroup).left().padBottom(50f).row();
        motionThrustGroup.add(thrustGroup).right();

        Table buttonNearestGroup = new Table(skin);
        buttonNearestGroup.add(buttonContainer).left().padRight(15f);
        buttonNearestGroup.add(nearestGroup).left();

        // Main table
        main = new Table(skin);
        main.setBackground("table-bg");
        main.setWidth(410f);
        main.setHeight(560f);
        main.setFillParent(false);
        main.bottom().left();
        main.pad(0, 20, 20, 0);

        float pad = 10f;
        main.add(controlsGroup).left().colspan(2).padBottom(pad).row();
        main.add(thrustContainer).left().colspan(2).row();
        main.add(engineGroup).left().padBottom(pad).padRight(pad * 8f);
        main.add(motionThrustGroup).left().top().padBottom(pad).row();
        main.add(buttonNearestGroup).left().colspan(2);

        rebuildGui();
    }

    protected void rebuildGui() {
        if (stage != null) {
            stage.clear();
            stage.addActor(main);

            // CAPTURE SCROLL FOCUS
            stage.addListener(new EventListener() {

                @Override
                public boolean handle(com.badlogic.gdx.scenes.scene2d.Event event) {
                    if (event instanceof InputEvent) {
                        InputEvent ie = (InputEvent) event;

                        if (ie.getType() == Type.mouseMoved) {
                            Actor scrollPanelAncestor = getScrollPanelAncestor(ie.getTarget());
                            stage.setScrollFocus(scrollPanelAncestor);
                        } else if (ie.getType() == Type.touchDown) {
                            if (ie.getTarget() instanceof TextField)
                                stage.setKeyboardFocus(ie.getTarget());
                        }
                    }
                    return false;
                }

                private Actor getScrollPanelAncestor(Actor actor) {
                    if (actor == null) {
                        return null;
                    } else if (actor instanceof ScrollPane) {
                        return actor;
                    } else {
                        return getScrollPanelAncestor(actor.getParent());
                    }
                }

            });
        }
    }

    @Override
    public void render(int rw, int rh) {
        // Draw UI
        stage.draw();

        // Draw attitude indicator
        aiViewport.setCamera(aiCam);
        aiViewport.setWorldSize(indicatorw * Settings.settings.program.ui.scale, indicatorh * Settings.settings.program.ui.scale);
        aiViewport.setScreenBounds((int) (indicatorx * Settings.settings.program.ui.scale), (int) (indicatory * Settings.settings.program.ui.scale), (int) (indicatorw * Settings.settings.program.ui.scale), (int) (indicatorh * Settings.settings.program.ui.scale));
        aiViewport.apply();

        // Model
        mb.begin(aiCam);
        aiTransform.idt();

        aiTransform.translate(0, 0, 6.4f);
        aiTransform.rotate(qf);
        aiTransform.rotate(0, 1, 0, 90);

        mb.render(aiModelInstance, env);
        mb.end();

        // VELOCITY INDICATORS IN NAVBALL
        if (!vel.isZero()) {
            // velocity
            aux3f1.set(vel.valuesf()).nor().scl(0.864f);
            aux3f1.mul(qf);
            aux3f1.add(0, 0, 6.3f);
            aiVelDec.setPosition(aux3f1);
            aiVelDec.setScale(0.0078f);
            aiVelDec.lookAt(aiCam.position, aiCam.up);

            // anti-velocity
            aux3f2.set(vel.valuesf()).nor().scl(-0.864f);
            aux3f2.mul(qf);
            aux3f2.add(0, 0, 6.3f);
            aiAntivelDec.setPosition(aux3f2);
            aiAntivelDec.setScale(0.0048f);
            aiAntivelDec.lookAt(aiCam.position, aiCam.up);

            // Depth
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
            Gdx.gl.glDepthFunc(GL20.GL_GREATER);
            Gdx.gl.glDepthMask(true);
            db.add(aiVelDec);
            db.add(aiAntivelDec);
            db.flush();
        }

        aiViewport.setWorldSize(rw, rh);
        aiViewport.setScreenBounds(0, 0, rw, rh);
        aiViewport.apply();

        // ai pointer
        sb.begin();
        sb.draw(aiPointerTexture, (indicatorx + indicatorw / 2 - 16), (indicatory + indicatorh / 2 - 16));
        sb.end();

    }

    @Override
    public void resizeImmediate(final int width, final int height) {
        stage.getViewport().update(width, height, true);
        rebuildGui();
    }

    @Override
    public void notify(final gaiasky.event.Event event, Object source, final Object... data) {
        switch (event) {
            case SPACECRAFT_LOADED -> {
                this.sc = (Entity) data[0];
                this.view.setEntity(this.sc);
                this.qf = view.getRotationQuaternion();
                this.vel = view.vel();
            }
            case SPACECRAFT_STABILISE_CMD -> {
                Boolean state = (Boolean) data[0];
                stabilise.setChecked(state);
            }
            case SPACECRAFT_STOP_CMD -> {
                Boolean state = (Boolean) data[0];
                stop.setChecked(state);
            }
            case SPACECRAFT_INFO -> {
                double y = -(Double) data[0];
                double p = -(Double) data[1];
                double r = (Double) data[2];
                double v = (Double) data[3];
                double thf = (Double) data[4];
                double epow = (Double) data[5];
                double ypow = (Double) data[6];
                double ppow = (Double) data[7];
                double rpow = (Double) data[8];
                yawvel.setText(nf.format(y) + "°");
                pitchvel.setText(nf.format(p) + "°");
                rollvel.setText(nf.format(r) + "°");
                Pair<Double, String> velstr = GlobalResources.doubleToVelocityString(v, Settings.settings.program.ui.distanceUnits);
                mainvel.setText(sf.format(velstr.getFirst()) + " " + velstr.getSecond());
                thrustfactor.setText("x" + (thf > 1000 ? sf.format(thf) : nf.format(thf)));
                setPowerValuesSlider(thrustv, thrustvm, epow);
                setPowerValuesSlider(thrusty, thrustym, ypow);
                setPowerValuesSlider(thrustp, thrustpm, ppow);
                setPowerValuesSlider(thrustr, thrustrm, rpow);
            }
            case SPACECRAFT_NEAREST_INFO -> {
                if (data[0] != null) {
                    closestname.setText((String) data[0]);
                    Pair<Double, String> closestDistance = GlobalResources.doubleToDistanceString((Double) data[1], Settings.settings.program.ui.distanceUnits);
                    closestdist.setText(sf.format(closestDistance.getFirst()) + " " + closestDistance.getSecond());
                } else {
                    closestname.setText("");
                    closestdist.setText("");
                }
            }
            case SPACECRAFT_THRUST_INFO -> {
                thrustEvents = false;
                enginePower.setValue((Integer) data[0]);
                thrustEvents = true;
            }
            default -> {
            }
        }

    }

    private void setPowerValuesSlider(Slider plus, Slider minus, double value) {
        plus.setValue((float) value);
        minus.setValue(1f + (float) value);
    }
}
