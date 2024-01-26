/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.Mapper;
import gaiasky.scene.api.IFocus;
import gaiasky.scene.api.IVisibilitySwitch;
import gaiasky.scene.camera.CameraManager.CameraMode;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.view.FocusView;
import gaiasky.util.*;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector2d;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.util.scene2d.*;

import java.text.DecimalFormat;

public class FocusInfoInterface extends TableGuiInterface implements IObserver {
    static private final int MAX_RULER_NAME_LEN = 9;
    private final FocusView view;
    private final Table focusInfo;
    private final Table moreInfo;
    private final Table rulerInfo;
    private final Table focusNames;
    private final Table content;
    private final Cell<Table> contentCell;
    private final Cell<?> focusInfoCell;
    private final Cell<?> rulerCell;
    private final Vector3d pos;
    private final Vector3b posb;
    protected Skin skin;
    protected OwnLabel focusName, focusType, focusId, focusRA, focusDEC, focusMuAlpha, focusMuDelta, focusRadVel, focusAngle, focusDistCam, focusDistSol, focusAppMagEarth, focusAppMagCamera, focusAbsMag, focusRadius;
    protected Button goTo, landOn, landAt, bookmark;
    protected OwnImageButton objectVisibility, labelVisibility;
    protected OwnLabel pointerName, pointerLonLat, pointerRADEC, viewRADEC;
    protected OwnLabel camName, camVel, camTracking, camDistSol, lonLatLabel, RADECPointerLabel, RADECViewLabel, appMagEarthLabel, appMagCameraLabel, absMagLabel;
    protected OwnLabel rulerName, rulerName0, rulerName1, rulerDist;
    protected OwnLabel focusIdExpand;
    protected HorizontalGroup focusActionsGroup;
    protected IFocus currentFocus;
    DecimalFormat nf;
    float pad1, pad3, pad5, pad10, pad15, bw;
    private ExternalInformationUpdater externalInfoUpdater;
    private boolean maximized;

    public FocusInfoInterface(Skin skin) {
        this(skin, false);
    }

    public FocusInfoInterface(Skin skin, boolean vr) {
        super(skin);
        this.setBackground("bg-pane");
        this.maximized = true;
        this.skin = skin;
        this.content = new Table(skin);
        // Widget width
        float width = 300f;

        nf = new DecimalFormat("##0.##");

        view = new FocusView();

        float buttonSize = 24f;
        float imgSize = 28.8f;
        pad15 = 24f;
        pad10 = 16f;
        pad5 = 8f;
        pad3 = 4.8f;
        pad1 = 1.6f;

        focusInfo = new Table();
        focusInfo.pad(pad5);
        Table cameraInfo = new Table();
        cameraInfo.pad(pad5);
        Table pointerInfo = new Table();
        pointerInfo.pad(pad5);
        moreInfo = new Table();
        rulerInfo = new Table();
        rulerInfo.pad(pad5);

        // FOCUS_MODE
        focusName = new OwnLabel("", skin, "hud-header");
        focusType = new OwnLabel("", skin, "hud-subheader");
        focusId = new OwnLabel("", skin, "hud");
        focusIdExpand = new OwnLabel("(?)", skin, "question");
        focusIdExpand.setVisible(false);
        focusNames = new Table(skin);
        focusRA = new OwnLabel("", skin, "hud");
        focusDEC = new OwnLabel("", skin, "hud");
        focusMuAlpha = new OwnLabel("", skin, "hud");
        focusMuDelta = new OwnLabel("", skin, "hud");
        focusRadVel = new OwnLabel("", skin, "hud");
        focusAppMagEarth = new OwnLabel("", skin, "hud");
        focusAppMagCamera = new OwnLabel("", skin, "hud");
        focusAbsMag = new OwnLabel("", skin, "hud");
        focusAngle = new OwnLabel("", skin, "hud");
        focusDistSol = new OwnLabel("", skin, "hud");
        focusDistCam = new OwnLabel("", skin, "hud");
        focusRadius = new OwnLabel("", skin, "hud");

        // Labels
        appMagEarthLabel = new OwnLabel(I18n.msg("gui.focusinfo.appmag.earth"), skin, "hud");
        appMagCameraLabel = new OwnLabel(I18n.msg("gui.focusinfo.appmag.camera"), skin, "hud");
        absMagLabel = new OwnLabel(I18n.msg("gui.focusinfo.absmag"), skin, "hud");

        // Pointer
        float pointerWidth = 100f;
        pointerName = new OwnLabel(I18n.msg("gui.pointer"), skin, "hud-header");
        pointerRADEC = new OwnLabel("", skin, "hud");
        pointerRADEC.setWidth(pointerWidth);
        pointerLonLat = new OwnLabel("", skin, "hud");
        pointerLonLat.setWidth(pointerWidth);
        viewRADEC = new OwnLabel("", skin, "hud");
        viewRADEC.setWidth(pointerWidth);
        float labelWidth = 80f;
        lonLatLabel = new OwnLabel(I18n.msg("gui.focusinfo.latlon"), skin, "hud");
        lonLatLabel.setWidth(labelWidth);
        RADECPointerLabel = new OwnLabel(I18n.msg("gui.focusinfo.alpha") + "/" + I18n.msg("gui.focusinfo.delta"), skin, "hud");
        RADECPointerLabel.setWidth(labelWidth);
        RADECViewLabel = new OwnLabel(I18n.msg("gui.focusinfo.alpha") + "/" + I18n.msg("gui.focusinfo.delta"), skin, "hud");
        RADECViewLabel.setWidth(labelWidth);
        Button pointerImgBtn1 = new OwnTextIconButton("", skin, "pointer");
        pointerImgBtn1.setSize(imgSize, imgSize);
        pointerImgBtn1.addListener(new OwnTextTooltip(I18n.msg("gui.focusinfo.pointer"), skin));
        Button pointerImgBtn2 = new OwnTextIconButton("", skin, "pointer");
        pointerImgBtn2.setSize(imgSize, imgSize);
        pointerImgBtn2.addListener(new OwnTextTooltip(I18n.msg("gui.focusinfo.pointer"), skin));
        Button viewImgBtn = new OwnTextIconButton("", skin, "eye");
        viewImgBtn.setSize(imgSize, imgSize);
        viewImgBtn.addListener(new OwnTextTooltip(I18n.msg("gui.focusinfo.view"), skin));

        // Camera
        camName = new OwnLabel(I18n.msg("gui.camera"), skin, "hud-header");
        camTracking = new OwnLabel("-", skin, "hud");
        camVel = new OwnLabel("", skin, "hud");
        camDistSol = new OwnLabel("", skin, "hud");

        // Ruler
        rulerName = new OwnLabel(I18n.msg("gui.ruler.title"), skin, "hud-header");
        rulerName0 = new OwnLabel("-", skin, "hud");
        rulerName1 = new OwnLabel("-", skin, "hud");
        HorizontalGroup rulerNameGroup = new HorizontalGroup();
        rulerNameGroup.space(pad5);
        rulerNameGroup.addActor(rulerName0);
        rulerNameGroup.addActor(new OwnLabel("<-->", skin, "hud"));
        rulerNameGroup.addActor(rulerName1);
        rulerDist = new OwnLabel("-", skin, "hud");

        // Bookmark
        bookmark = new OwnImageButton(skin, "bookmark");
        bookmark.addListener(new OwnTextTooltip(I18n.msg("gui.bookmark"), skin));
        bookmark.addListener(event -> {
            if (currentFocus != null && event instanceof ChangeEvent) {
                if (bookmark.isChecked())
                    EventManager.publish(Event.BOOKMARKS_ADD, bookmark, currentFocus.getName(), false);
                else
                    EventManager.publish(Event.BOOKMARKS_REMOVE_ALL, bookmark, currentFocus.getName());
            }
            return false;
        });

        // GoTo, LandOn and LandAt
        goTo = new OwnTextIconButton("", skin, "go-to");
        goTo.setSize(buttonSize, buttonSize);
        goTo.addListener((event) -> {
            if (currentFocus != null && event instanceof ChangeEvent) {
                EventManager.publish(Event.NAVIGATE_TO_OBJECT, goTo, currentFocus);
                return true;
            }
            return false;

        });
        goTo.addListener(new OwnTextTooltip(I18n.msg("gui.focusinfo.goto"), skin));

        landOn = new OwnTextIconButton("", skin, "land-on");
        landOn.setSize(buttonSize, buttonSize);
        landOn.addListener((event) -> {
            if (currentFocus != null && event instanceof ChangeEvent) {
                EventManager.publish(Event.LAND_ON_OBJECT, landOn, currentFocus);
                return true;
            }
            return false;

        });
        landOn.addListener(new OwnTextTooltip(I18n.msg("gui.focusinfo.landon"), skin));

        landAt = new OwnTextIconButton("", skin, "land-at");
        landAt.setSize(buttonSize, buttonSize);
        landAt.addListener((event) -> {
            if (currentFocus != null && event instanceof ChangeEvent) {
                EventManager.publish(Event.SHOW_LAND_AT_LOCATION_ACTION, landAt, currentFocus);
                return true;
            }
            return false;
        });
        landAt.addListener(new OwnTextTooltip(I18n.msg("gui.focusinfo.landat"), skin));

        objectVisibility = new OwnImageButton(skin, "eye-toggle");
        objectVisibility.addListener(event -> {
            if (event instanceof ChangeEvent) {
                // Toggle visibility
                EventManager.publish(Event.PER_OBJECT_VISIBILITY_CMD, objectVisibility, currentFocus, currentFocus.getName(), !objectVisibility.isChecked());
                return true;
            }
            return false;
        });

        labelVisibility = new OwnImageButton(skin, "label-toggle");
        labelVisibility.addListener(event -> {
            if (event instanceof ChangeEvent) {
                // Toggle visibility
                EventManager.publish(Event.FORCE_OBJECT_LABEL_CMD, labelVisibility, currentFocus, currentFocus.getName(), !labelVisibility.isChecked());
                return true;
            }
            return false;
        });

        bw = Math.max(landOn.getWidth(), landAt.getWidth());
        bw += 6.0f;

        goTo.setSize(bw, bw);
        landOn.setSize(bw, bw);
        landAt.setSize(bw, bw);

        focusActionsGroup = new HorizontalGroup();
        focusActionsGroup.space(pad5);
        focusActionsGroup.addActor(objectVisibility);
        focusActionsGroup.addActor(labelVisibility);
        focusActionsGroup.addActor(bookmark);
        focusActionsGroup.addActor(goTo);
        focusActionsGroup.addActor(landOn);
        focusActionsGroup.addActor(landAt);

        float w = 170f;
        focusId.setWidth(w);

        focusRA.setWidth(w);
        focusDEC.setWidth(w);
        focusMuAlpha.setWidth(w);
        focusMuDelta.setWidth(w);
        focusRadVel.setWidth(w);
        focusAngle.setWidth(w);
        focusDistSol.setWidth(w);
        focusDistCam.setWidth(w);
        camTracking.setWidth(w);
        camVel.setWidth(w);

        // FOCUS INFO
        focusInfo.add(focusName).width(width).left().colspan(2).padBottom(pad5).row();
        focusInfo.add(focusActionsGroup).width(width).left().colspan(2).padBottom(pad5).row();
        focusInfo.add(focusType).left().padBottom(pad5).colspan(2).row();
        focusInfo.add(new OwnLabel("ID", skin, "hud")).left();
        focusInfo.add(hg(focusId, focusIdExpand)).left().padLeft(pad15).row();
        focusInfo.add(new OwnLabel(I18n.msg("gui.focusinfo.names"), skin, "hud")).left().padBottom(pad5);
        focusInfo.add(focusNames).left().padBottom(pad5).padLeft(pad15).row();
        if (!vr) {
            focusInfo.add(new OwnLabel(I18n.msg("gui.focusinfo.alpha"), skin, "hud")).left();
            focusInfo.add(focusRA).left().padLeft(pad15).row();
            focusInfo.add(new OwnLabel(I18n.msg("gui.focusinfo.delta"), skin, "hud")).left();
            focusInfo.add(focusDEC).left().padLeft(pad15).row();
            focusInfo.add(new OwnLabel(I18n.msg("gui.focusinfo.mualpha"), skin, "hud")).left();
            focusInfo.add(focusMuAlpha).left().padLeft(pad15).row();
            focusInfo.add(new OwnLabel(I18n.msg("gui.focusinfo.mudelta"), skin, "hud")).left();
            focusInfo.add(focusMuDelta).left().padLeft(pad15).row();
            focusInfo.add(new OwnLabel(I18n.msg("gui.focusinfo.radvel"), skin, "hud")).left();
            focusInfo.add(focusRadVel).left().padLeft(pad15).row();
            focusInfo.add(appMagEarthLabel).left();
            focusInfo.add(focusAppMagEarth).left().padLeft(pad15).row();
            focusInfo.add(appMagCameraLabel).left();
            focusInfo.add(focusAppMagCamera).left().padLeft(pad15).row();
            focusInfo.add(absMagLabel).left();
            focusInfo.add(focusAbsMag).left().padLeft(pad15).row();
        }
        focusInfo.add(new OwnLabel(I18n.msg("gui.focusinfo.angle"), skin, "hud")).left();
        focusInfo.add(focusAngle).left().padLeft(pad15).row();
        focusInfo.add(new OwnLabel(I18n.msg("gui.focusinfo.distance.sol"), skin, "hud")).left();
        focusInfo.add(focusDistSol).left().padLeft(pad15).row();
        focusInfo.add(new OwnLabel(I18n.msg("gui.focusinfo.distance.cam"), skin, "hud")).left();
        focusInfo.add(focusDistCam).left().padLeft(pad15).row();
        focusInfo.add(new OwnLabel(I18n.msg("gui.focusinfo.radius"), skin, "hud")).left();
        focusInfo.add(focusRadius).left().padLeft(pad15).row();
        focusInfo.add(moreInfo).left().colspan(2).padBottom(pad5).padTop(pad10);

        // POINTER INFO
        if (!vr) {
            pointerInfo.add(pointerName).width(width).left().colspan(3).row();
            pointerInfo.add(pointerImgBtn1).left().padRight(pad3);
            pointerInfo.add(RADECPointerLabel).left();
            pointerInfo.add(pointerRADEC).expandX().left().padLeft(pad5).row();
            pointerInfo.add(pointerImgBtn2).left().padRight(pad3);
            pointerInfo.add(lonLatLabel).left();
            pointerInfo.add(pointerLonLat).expandX().left().padLeft(pad5).row();
            pointerInfo.add(viewImgBtn).left().padRight(pad3);
            pointerInfo.add(RADECViewLabel).left();
            pointerInfo.add(viewRADEC).expandX().left().padLeft(pad5);
        }

        // CAMERA INFO
        cameraInfo.add(camName).width(width).left().colspan(2).row();
        cameraInfo.add(new OwnLabel(I18n.msg("gui.camera.track"), skin, "hud")).left();
        cameraInfo.add(camTracking).left().padLeft(pad15).row();
        cameraInfo.add(new OwnLabel(I18n.msg("gui.camera.vel"), skin, "hud")).left();
        cameraInfo.add(camVel).left().padLeft(pad15).row();
        cameraInfo.add(new OwnLabel(I18n.msg("gui.focusinfo.distance.sol"), skin, "hud")).left();
        cameraInfo.add(camDistSol).left().padLeft(pad15);

        // RULER INFO
        rulerInfo.add(rulerName).left().row();
        rulerInfo.add(rulerNameGroup).left().row();
        rulerInfo.add(rulerDist).left();

        // MINIMIZE/MAXIMIZE
        Link toggleSize = new Link(maximized ? "(-)" : "(+)", skin, null);
        var toggleSizeTooltip = new OwnTextTooltip(I18n.msg("gui.minimize.pane"), skin);
        toggleSize.addListener(toggleSizeTooltip);
        toggleSize.setColor(ColorUtils.gYellowC);
        toggleSize.addListener(new ClickListener() {
            public void clicked(InputEvent event, float x, float y) {
                if (maximized) {
                    // Minimize.
                    maximized = false;
                    content.addAction(Actions.sequence(
                            Actions.alpha(1f),
                            Actions.fadeOut(Settings.settings.program.ui.getAnimationSeconds()),
                            Actions.run(() -> {
                                contentCell.setActor(null);
                                toggleSize.setText("(+)");
                                toggleSizeTooltip.setText(I18n.msg("gui.maximize.pane"));
                            })
                    ));
                } else {
                    // Maximize.
                    maximized = true;
                    contentCell.setActor(content);
                    content.addAction(Actions.sequence(
                            Actions.alpha(0f),
                            Actions.fadeIn(Settings.settings.program.ui.getAnimationSeconds()),
                            Actions.run(() -> {
                                toggleSize.setText("(-)");
                                toggleSizeTooltip.setText(I18n.msg("gui.minimize.pane"));
                            })
                    ));
                }
                pack();
            }
        });

        focusInfoCell = content.add(focusInfo).left();
        content.row();
        content.add(pointerInfo).left();
        content.row();
        content.add(cameraInfo).left();
        content.row();
        rulerCell = content.add(rulerInfo).left();
        if (maximized) {
            contentCell = add(content);
        } else {
            contentCell = add();
        }
        row();
        add(toggleSize).right().pad(pad5).row();
        pack();
        rulerCell.clearActor();

        if (!vr) {
            externalInfoUpdater = new ExternalInformationUpdater();
            externalInfoUpdater.setParameters(moreInfo, skin, pad10);
        }

        pos = new Vector3d();
        posb = new Vector3b();
        EventManager.instance.subscribe(this, Event.FOCUS_CHANGED, Event.FOCUS_INFO_UPDATED, Event.CAMERA_MOTION_UPDATE, Event.CAMERA_TRACKING_OBJECT_UPDATE, Event.CAMERA_MODE_CMD, Event.LON_LAT_UPDATED, Event.RA_DEC_UPDATED, Event.RULER_ATTACH_0, Event.RULER_ATTACH_1, Event.RULER_CLEAR, Event.RULER_DIST, Event.PER_OBJECT_VISIBILITY_CMD, Event.FORCE_OBJECT_LABEL_CMD);
    }

    private HorizontalGroup hg(Actor... actors) {
        HorizontalGroup hg = new HorizontalGroup();
        for (Actor a : actors)
            hg.addActor(a);
        return hg;
    }

    private void unsubscribe() {
        EventManager.instance.removeAllSubscriptions(this);
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        final String deg = I18n.msg("gui.unit.deg");
        final var settings = Settings.settings;
        switch (event) {
            case FOCUS_CHANGED -> {
                if (data[0] instanceof String) {
                    var entity = GaiaSky.instance.scene.getEntity((String) data[0]);
                    view.setEntity(entity);
                } else {
                    FocusView v = (FocusView) data[0];
                    view.setEntity(v.getEntity());
                }
                currentFocus = view;
                final int focusFieldMaxLength = 13;

                // ID
                boolean cappedId = false;
                String id = "";
                if (view.getExtra() != null || view.getStarSet() != null) {
                    if (view.getId() > 0) {
                        id = String.valueOf(view.getId());
                    } else if (view.getHip() > 0) {
                        id = "HIP " + view.getHip();
                    }
                }
                if (id.isEmpty()) {
                    id = "-";
                }
                String idString = id;
                if (id.length() > focusFieldMaxLength) {
                    idString = TextUtils.capString(id, focusFieldMaxLength);
                    cappedId = true;
                }

                // Link
                boolean vis = Mapper.atmosphere.has(view.getEntity());
                focusActionsGroup.removeActor(landOn);
                focusActionsGroup.removeActor(landAt);
                if (vis) {
                    focusActionsGroup.addActor(landOn);
                    focusActionsGroup.addActor(landAt);
                }

                // Type
                try {
                    focusType.setText(I18n.msg("element." + ComponentType.values()[view.getCt().getFirstOrdinal()].toString().toLowerCase() + ".singular"));
                } catch (Exception e) {
                    focusType.setText("");
                }

                // Coordinates
                pointerLonLat.setText("-/-");

                // Bookmark
                bookmark.setProgrammaticChangeEvents(false);
                bookmark.setChecked(GaiaSky.instance.getBookmarksManager().containsName(currentFocus.getName()));
                bookmark.setProgrammaticChangeEvents(true);

                // Visible
                objectVisibility.setCheckedNoFire(!((IVisibilitySwitch) currentFocus).isVisible(true));
                objectVisibility.addListener(new OwnTextTooltip(I18n.msg("action.visibility", currentFocus.getName()), skin));

                // Force label
                labelVisibility.setCheckedNoFire(!currentFocus.isForceLabel(currentFocus.getName().toLowerCase().trim()));
                labelVisibility.addListener(new OwnTextTooltip(I18n.msg("action.forcelabel", currentFocus.getName()), skin));

                // Id, names
                focusId.setText(idString);
                focusId.clearListeners();
                if (cappedId) {
                    focusId.addListener(new OwnTextTooltip(id, skin));
                    focusIdExpand.addListener(new OwnTextTooltip(id, skin));
                    focusIdExpand.setVisible(true);
                } else {
                    focusIdExpand.clearListeners();
                    focusIdExpand.setVisible(false);
                }
                String objectName = TextUtils.capString(view.getLocalizedName(), focusFieldMaxLength);
                focusName.setText(objectName);
                focusName.clearListeners();
                focusName.addListener(new OwnTextTooltip(view.getLocalizedName(), skin));
                focusNames.clearChildren();
                String[] names = view.getNames();
                if (names != null && names.length > 0) {
                    int chars = 0;
                    HorizontalGroup currGroup = new HorizontalGroup();
                    for (int i = 0; i < names.length; i++) {
                        String name = names[i];
                        String nameCapped = TextUtils.capString(name, focusFieldMaxLength);
                        OwnLabel nl = new OwnLabel(nameCapped, skin, "object-name");
                        if (nameCapped.length() != name.length())
                            nl.addListener(new OwnTextTooltip(name, skin));
                        currGroup.addActor(nl);
                        chars += nameCapped.length() + 1;
                        if (i < names.length - 1) {
                            currGroup.addActor(new OwnLabel(", ", skin));
                            chars++;
                        }
                        if (i < names.length - 1 && chars > 14) {
                            focusNames.add(currGroup).left().row();
                            currGroup = new HorizontalGroup();
                            chars = 0;
                        }
                    }
                    if (chars > 0)
                        focusNames.add(currGroup).left();
                } else {
                    focusNames.add(new OwnLabel("-", skin));
                }
                Vector2d posSph = view.getPosSph();
                if (posSph != null && posSph.len() > 0f) {
                    focusRA.setText(nf.format(posSph.x) + deg);
                    focusDEC.setText(nf.format(posSph.y) + deg);
                } else {
                    Coordinates.cartesianToSpherical(view.getAbsolutePosition(posb), pos);

                    focusRA.setText(nf.format(MathUtilsDouble.radDeg * pos.x % 360) + deg);
                    focusDEC.setText(nf.format(MathUtilsDouble.radDeg * pos.y % 360) + deg);
                }
                if (view.hasProperMotion()) {
                    focusMuAlpha.setText(nf.format(view.getMuAlpha()) + " " + I18n.msg("gui.unit.masyr"));
                    focusMuDelta.setText(nf.format(view.getMuDelta()) + " " + I18n.msg("gui.unit.masyr"));
                    double rv = view.getRadialVelocity();
                    if (Double.isFinite(rv)) {
                        focusRadVel.setText(nf.format(view.getRadialVelocity()) + " " + I18n.msg("gui.unit.kms"));
                    } else {
                        focusRadVel.setText(I18n.msg("gui.focusinfo.na"));
                    }
                } else {
                    focusMuAlpha.setText("-");
                    focusMuDelta.setText("-");
                    focusRadVel.setText("-");
                }
                if (view.isCluster()) {
                    // Some star clusters have the number of stars
                    // Magnitudes make not sense
                    var cluster = Mapper.cluster.get(view.getEntity());
                    if (cluster.numStars > 0) {
                        appMagEarthLabel.setText("# " + I18n.msg("element.stars"));
                        focusAppMagEarth.setText(Integer.toString(cluster.numStars));
                    } else {
                        appMagEarthLabel.setText("");
                        focusAppMagEarth.setText("");
                    }
                    focusAppMagCamera.setText("");
                    appMagCameraLabel.setText("");
                    focusAbsMag.setText("");
                    absMagLabel.setText("");

                } else if (view.isCelestial()) {
                    // Planets, satellites, etc.
                    // Apparent magnitude depends on absolute magnitude
                    // We need to compute the apparent magnitude from earth and camera

                    // Apparent magnitude (earth)
                    appMagEarthLabel.setText(I18n.msg("gui.focusinfo.appmag.earth"));
                    float appMag = view.getAppmag();
                    focusAppMagEarth.setText(nf.format(appMag));

                    // Apparent magnitude (camera)
                    appMagCameraLabel.setText(I18n.msg("gui.focusinfo.appmag.camera"));

                    // Absolute magnitude
                    absMagLabel.setText(I18n.msg("gui.focusinfo.absmag"));
                    focusAbsMag.setText(nf.format(view.getAbsmag()));

                    appMagEarthLabel.addListener(new OwnTextTooltip(I18n.msg("gui.focusinfo.appmag.earth.tooltip"), skin));
                    focusAppMagEarth.addListener(new OwnTextTooltip(I18n.msg("gui.focusinfo.appmag.earth.tooltip"), skin));
                    appMagCameraLabel.addListener(new OwnTextTooltip(I18n.msg("gui.focusinfo.appmag.camera.tooltip"), skin));
                    focusAppMagCamera.addListener(new OwnTextTooltip(I18n.msg("gui.focusinfo.appmag.camera.tooltip"), skin));
                    absMagLabel.addListener(new OwnTextTooltip(I18n.msg("gui.focusinfo.absmag.tooltip"), skin));
                    focusAbsMag.addListener(new OwnTextTooltip(I18n.msg("gui.focusinfo.absmag.tooltip"), skin));
                } else {
                    // Stars, apparent magnitude form Earth is fixed, from camera not so much.

                    // Apparent magnitude (earth)
                    appMagEarthLabel.setText(I18n.msg("gui.focusinfo.appmag.earth"));
                    float appMag = view.getAppmag();
                    focusAppMagEarth.setText(nf.format(appMag));

                    // Apparent magnitude (cam)
                    appMagCameraLabel.setText(I18n.msg("gui.focusinfo.appmag.camera"));

                    // Absolute magnitude
                    absMagLabel.setText(I18n.msg("gui.focusinfo.absmag"));
                    focusAbsMag.setText(nf.format(view.getAbsmag()));

                    // Tooltips
                    appMagEarthLabel.addListener(new OwnTextTooltip(I18n.msg("gui.focusinfo.appmag.earth.tooltip"), skin));
                    focusAppMagEarth.addListener(new OwnTextTooltip(I18n.msg("gui.focusinfo.appmag.earth.tooltip"), skin));
                    appMagCameraLabel.addListener(new OwnTextTooltip(I18n.msg("gui.focusinfo.appmag.camera.tooltip"), skin));
                    focusAppMagCamera.addListener(new OwnTextTooltip(I18n.msg("gui.focusinfo.appmag.camera.tooltip"), skin));
                    absMagLabel.addListener(new OwnTextTooltip(I18n.msg("gui.focusinfo.absmag.tooltip"), skin));
                    focusAbsMag.addListener(new OwnTextTooltip(I18n.msg("gui.focusinfo.absmag.tooltip"), skin));
                }
                if (ComponentType.values()[view.getCt().getFirstOrdinal()] == ComponentType.Stars) {
                    focusRadius.setText("-");
                } else {
                    focusRadius.setText(GlobalResources.formatNumber(view.getRadius() * Constants.U_TO_KM) + " " + I18n.msg("gui.unit.km"));
                }

                // Update more info table
                moreInfo.clear();
                if (externalInfoUpdater != null)
                    externalInfoUpdater.update(view);
            }
            case FOCUS_INFO_UPDATED -> {
                focusAngle.setText(GlobalResources.formatNumber(Math.toDegrees((double) data[1]) % 360) + deg);

                // Dist to cam
                Pair<Double, String> distCam = GlobalResources.doubleToDistanceString((double) data[0], settings.program.ui.distanceUnits);
                focusDistCam.setText(GlobalResources.formatNumber(Math.max(0d, distCam.getFirst())) + " " + distCam.getSecond());

                // Dist to sol
                if (data.length > 4) {
                    Pair<Double, String> distSol = GlobalResources.doubleToDistanceString((double) data[4], settings.program.ui.distanceUnits);
                    focusDistSol.setText(GlobalResources.formatNumber(Math.max(0d, distSol.getFirst())) + " " + distSol.getSecond());
                }

                // Apparent magnitude from camera
                focusAppMagCamera.setText(nf.format((double) data[5]));

                // Apparent magnitude from Earth (for planets, etc.)
                if (data.length > 6 && Double.isFinite((double) data[6])) {
                    // Apparent magnitude from Earth
                    focusAppMagEarth.setText(nf.format((double) data[6]));
                }
                focusRA.setText(nf.format((double) data[2] % 360) + deg);
                focusDEC.setText(nf.format((double) data[3] % 360) + deg);
            }
            case CAMERA_MOTION_UPDATE -> {
                final Vector3b campos = (Vector3b) data[0];
                camVel.setText(GlobalResources.formatNumber((double) data[1]) + " " + I18n.msg("gui.unit.kmh"));
                Pair<Double, String> distSol = GlobalResources.doubleToDistanceString(campos.lenDouble(), settings.program.ui.distanceUnits);
                camDistSol.setText(GlobalResources.formatNumber(Math.max(0d, distSol.getFirst())) + " " + distSol.getSecond());
            }
            case CAMERA_TRACKING_OBJECT_UPDATE -> {
                final IFocus trackingObject = (IFocus) data[0];
                final String trackingName = (String) data[1];
                if (trackingObject == null && trackingName == null) {
                    camTracking.setText("-");
                } else {
                    camTracking.setText(trackingName);
                }
            }
            case CAMERA_MODE_CMD -> {
                // Update camera mode selection
                final CameraMode mode = (CameraMode) data[0];
                if (mode.equals(CameraMode.FOCUS_MODE)) {
                    displayInfo(focusInfoCell, focusInfo);
                } else {
                    hideInfo(focusInfoCell);
                }
            }
            case LON_LAT_UPDATED -> {
                Double lon = (Double) data[0];
                Double lat = (Double) data[1];
                pointerLonLat.setText(nf.format(lat) + deg + "/" + nf.format(lon) + deg);
            }
            case RA_DEC_UPDATED -> {
                Double pmRa = (Double) data[0];
                Double pmDec = (Double) data[1];
                Double vRa = (Double) data[2];
                Double vDec = (Double) data[3];
                pointerRADEC.setText(nf.format(pmRa) + deg + "/" + nf.format(pmDec) + deg);
                viewRADEC.setText(nf.format(vRa) + deg + "/" + nf.format(vDec) + deg);
            }
            case RULER_ATTACH_0 -> {
                String n0 = (String) data[0];
                rulerName0.setText(TextUtils.capString(n0, MAX_RULER_NAME_LEN));
                displayInfo(rulerCell, rulerInfo);
            }
            case RULER_ATTACH_1 -> {
                String n1 = (String) data[0];
                rulerName1.setText(TextUtils.capString(n1, MAX_RULER_NAME_LEN));
                displayInfo(rulerCell, rulerInfo);
            }
            case RULER_CLEAR -> {
                rulerName0.setText("-");
                rulerName1.setText("-");
                rulerDist.setText(I18n.msg("gui.sc.distance") + ": -");
                hideInfo(rulerCell);
            }
            case RULER_DIST -> {
                String rd = (String) data[1];
                rulerDist.setText(I18n.msg("gui.sc.distance") + ": " + rd);
            }
            case PER_OBJECT_VISIBILITY_CMD -> {
                if (source != objectVisibility) {
                    if (data[0] instanceof IVisibilitySwitch vs) {
                        String name = (String) data[1];
                        if (vs == currentFocus && currentFocus.hasName(name)) {
                            boolean visible = (boolean) data[2];
                            objectVisibility.setCheckedNoFire(!visible);
                        }
                    }

                    if (data[0] instanceof Entity entity) {
                        String name = (String) data[1];
                        if (currentFocus == view && view.getEntity() == entity && currentFocus.hasName(name)) {
                            boolean visible = (boolean) data[2];
                            objectVisibility.setCheckedNoFire(!visible);
                        }
                    }
                }
            }
            case FORCE_OBJECT_LABEL_CMD -> {
                if (source != labelVisibility) {
                    if (data[0] instanceof Entity entity) {
                        String name = (String) data[1];
                        if (currentFocus == view && view.getEntity() == entity && currentFocus.hasName(name)) {
                            boolean forceLabel = (boolean) data[2];
                            labelVisibility.setCheckedNoFire(forceLabel);
                        }
                    }
                }
            }
            default -> {
            }
        }

    }

    public void programmaticUpdate() {
        ICamera camera = GaiaSky.instance.getICamera();
        notify(Event.CAMERA_MODE_CMD, this, camera.getMode());
        if (camera.getMode().isFocus()) {
            notify(Event.FOCUS_CHANGED, this, camera.getFocus());
        }
    }

    @SuppressWarnings({"rawtypes"})
    private void displayInfo(Cell cell, Actor info) {
        cell.setActor(info);
        pack();
    }

    @SuppressWarnings({"rawtypes"})
    private void hideInfo(Cell cell) {
        cell.clearActor();
        pack();
    }

    public void dispose() {
        unsubscribe();
    }

    @Override
    public void update() {

    }

}
