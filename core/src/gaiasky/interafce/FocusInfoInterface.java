/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import gaiasky.GaiaSky;
import gaiasky.desktop.util.ExternalInformationUpdater;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scenegraph.*;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.util.*;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.format.INumberFormat;
import gaiasky.util.format.NumberFormatFactory;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Vector2d;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.util.scene2d.OwnImageButton;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextIconButton;
import gaiasky.util.scene2d.OwnTextTooltip;

/**
 * Part of the user interface which holds the information on the current focus
 * object and on the camera.
 */
public class FocusInfoInterface extends TableGuiInterface implements IObserver {
    static private final int MAX_RULER_NAME_LEN = 9;

    protected Skin skin;
    protected OwnLabel focusName, focusType, focusId, focusRA, focusDEC, focusMuAlpha, focusMuDelta, focusRadVel, focusAngle, focusDistCam, focusDistSol, focusAppMagEarth, focusAppMagCamera, focusAbsMag, focusRadius;
    protected Button goTo, landOn, landAt, bookmark;
    protected OwnImageButton objectVisibility, labelVisibility;
    protected OwnLabel pointerName, pointerLonLat, pointerRADEC, viewRADEC;
    protected OwnLabel camName, camVel, camPos, camTracking, camDistSol, lonLatLabel, RADECPointerLabel, RADECViewLabel, appMagEarthLabel, appMagCameraLabel, absMagLabel;
    protected OwnLabel rulerName, rulerName0, rulerName1, rulerDist;
    protected OwnLabel focusIdExpand;

    protected HorizontalGroup focusNameGroup;

    protected IFocus currentFocus;
    private ExternalInformationUpdater externalInfoUpdater;

    private final Table focusInfo;
    private final Table moreInfo;
    private final Table rulerInfo;
    private final Table focusNames;
    private final Cell<?> focusInfoCell;
    private final Cell<?> rulerCell;
    private Vector3d pos;
    private Vector3b posb;

    INumberFormat nf, sf;

    float pad1, pad3, pad5, pad10, pad15, bw;

    public FocusInfoInterface(Skin skin) {
        this(skin, false);
    }

    public FocusInfoInterface(Skin skin, boolean vr) {
        super(skin);
        this.setBackground("table-bg");
        this.skin = skin;
        // Widget width
        float width = 360f;

        nf = NumberFormatFactory.getFormatter("##0.##");
        sf = NumberFormatFactory.getFormatter("0.###E0");

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
        appMagEarthLabel = new OwnLabel(I18n.txt("gui.focusinfo.appmag.earth"), skin, "hud");
        appMagCameraLabel = new OwnLabel(I18n.txt("gui.focusinfo.appmag.camera"), skin, "hud");
        absMagLabel = new OwnLabel(I18n.txt("gui.focusinfo.absmag"), skin, "hud");

        // Pointer
        float pointerWidth = 100f;
        pointerName = new OwnLabel(I18n.txt("gui.pointer"), skin, "hud-header");
        pointerRADEC = new OwnLabel("", skin, "hud");
        pointerRADEC.setWidth(pointerWidth);
        pointerLonLat = new OwnLabel("", skin, "hud");
        pointerLonLat.setWidth(pointerWidth);
        viewRADEC = new OwnLabel("", skin, "hud");
        viewRADEC.setWidth(pointerWidth);
        float labelWidth = 80f;
        lonLatLabel = new OwnLabel(I18n.txt("gui.focusinfo.latlon"), skin, "hud");
        lonLatLabel.setWidth(labelWidth);
        RADECPointerLabel = new OwnLabel(I18n.txt("gui.focusinfo.alpha") + "/" + I18n.txt("gui.focusinfo.delta"), skin, "hud");
        RADECPointerLabel.setWidth(labelWidth);
        RADECViewLabel = new OwnLabel(I18n.txt("gui.focusinfo.alpha") + "/" + I18n.txt("gui.focusinfo.delta"), skin, "hud");
        RADECViewLabel.setWidth(labelWidth);
        Button pointerImgBtn1 = new OwnTextIconButton("", skin, "pointer");
        pointerImgBtn1.setSize(imgSize, imgSize);
        pointerImgBtn1.addListener(new OwnTextTooltip(I18n.txt("gui.focusinfo.pointer"), skin));
        Button pointerImgBtn2 = new OwnTextIconButton("", skin, "pointer");
        pointerImgBtn2.setSize(imgSize, imgSize);
        pointerImgBtn2.addListener(new OwnTextTooltip(I18n.txt("gui.focusinfo.pointer"), skin));
        Button viewImgBtn = new OwnTextIconButton("", skin, "eye");
        viewImgBtn.setSize(imgSize, imgSize);
        viewImgBtn.addListener(new OwnTextTooltip(I18n.txt("gui.focusinfo.view"), skin));

        // Camera
        camName = new OwnLabel(I18n.txt("gui.camera"), skin, "hud-header");
        camTracking = new OwnLabel("-", skin, "hud");
        camVel = new OwnLabel("", skin, "hud");
        camDistSol = new OwnLabel("", skin, "hud");
        camPos = new OwnLabel("", skin, "hud");

        // Ruler
        rulerName = new OwnLabel(I18n.txt("gui.ruler.title"), skin, "hud-header");
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
        bookmark.addListener(new OwnTextTooltip(I18n.txt("gui.bookmark"), skin));
        bookmark.addListener(event -> {
            if (currentFocus != null && event instanceof ChangeEvent) {
                if (bookmark.isChecked())
                    EventManager.publish(Event.BOOKMARKS_ADD, bookmark, bookmark, currentFocus.getName(), false);
                else
                    EventManager.publish(Event.BOOKMARKS_REMOVE_ALL, bookmark, bookmark, currentFocus.getName());
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
        goTo.addListener(new OwnTextTooltip(I18n.txt("gui.focusinfo.goto"), skin));

        landOn = new OwnTextIconButton("", skin, "land-on");
        landOn.setSize(buttonSize, buttonSize);
        landOn.addListener((event) -> {
            if (currentFocus != null && event instanceof ChangeEvent) {
                EventManager.publish(Event.LAND_ON_OBJECT, landOn, currentFocus);
                return true;
            }
            return false;

        });
        landOn.addListener(new OwnTextTooltip(I18n.txt("gui.focusinfo.landon"), skin));

        landAt = new OwnTextIconButton("", skin, "land-at");
        landAt.setSize(buttonSize, buttonSize);
        landAt.addListener((event) -> {
            if (currentFocus != null && event instanceof ChangeEvent) {
                EventManager.publish(Event.SHOW_LAND_AT_LOCATION_ACTION, landAt, currentFocus);
                return true;
            }
            return false;
        });
        landAt.addListener(new OwnTextTooltip(I18n.txt("gui.focusinfo.landat"), skin));

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
        bw += 3.2f;

        goTo.setWidth(bw);
        landOn.setWidth(bw);
        landAt.setWidth(bw);

        focusNameGroup = new HorizontalGroup();
        focusNameGroup.space(pad5);
        focusNameGroup.addActor(focusName);
        focusNameGroup.addActor(objectVisibility);
        focusNameGroup.addActor(labelVisibility);
        focusNameGroup.addActor(bookmark);
        focusNameGroup.addActor(goTo);
        focusNameGroup.addActor(landOn);
        focusNameGroup.addActor(landAt);

        float w = 208f;
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
        focusInfo.add(focusNameGroup).width(width).left().colspan(2).padBottom(pad5);
        focusInfo.row();
        focusInfo.add(focusType).left().padBottom(pad5).colspan(2);
        focusInfo.row();
        focusInfo.add(new OwnLabel("ID", skin, "hud")).left();
        focusInfo.add(hg(focusId, focusIdExpand)).left().padLeft(pad15);
        focusInfo.row();
        focusInfo.add(new OwnLabel(I18n.txt("gui.focusinfo.names"), skin, "hud")).left().padBottom(pad5);
        focusInfo.add(focusNames).left().padBottom(pad5).padLeft(pad15);
        focusInfo.row();
        if (!vr) {
            focusInfo.add(new OwnLabel(I18n.txt("gui.focusinfo.alpha"), skin, "hud")).left();
            focusInfo.add(focusRA).left().padLeft(pad15);
            focusInfo.row();
            focusInfo.add(new OwnLabel(I18n.txt("gui.focusinfo.delta"), skin, "hud")).left();
            focusInfo.add(focusDEC).left().padLeft(pad15);
            focusInfo.row();
            focusInfo.add(new OwnLabel(I18n.txt("gui.focusinfo.mualpha"), skin, "hud")).left();
            focusInfo.add(focusMuAlpha).left().padLeft(pad15);
            focusInfo.row();
            focusInfo.add(new OwnLabel(I18n.txt("gui.focusinfo.mudelta"), skin, "hud")).left();
            focusInfo.add(focusMuDelta).left().padLeft(pad15);
            focusInfo.row();
            focusInfo.add(new OwnLabel(I18n.txt("gui.focusinfo.radvel"), skin, "hud")).left();
            focusInfo.add(focusRadVel).left().padLeft(pad15);
            focusInfo.row();
            focusInfo.add(appMagEarthLabel).left();
            focusInfo.add(focusAppMagEarth).left().padLeft(pad15);
            focusInfo.row();
            focusInfo.row();
            focusInfo.add(appMagCameraLabel).left();
            focusInfo.add(focusAppMagCamera).left().padLeft(pad15);
            focusInfo.row();
            focusInfo.add(absMagLabel).left();
            focusInfo.add(focusAbsMag).left().padLeft(pad15);
            focusInfo.row();
        }
        focusInfo.add(new OwnLabel(I18n.txt("gui.focusinfo.angle"), skin, "hud")).left();
        focusInfo.add(focusAngle).left().padLeft(pad15);
        focusInfo.row();
        focusInfo.add(new OwnLabel(I18n.txt("gui.focusinfo.distance.sol"), skin, "hud")).left();
        focusInfo.add(focusDistSol).left().padLeft(pad15);
        focusInfo.row();
        focusInfo.add(new OwnLabel(I18n.txt("gui.focusinfo.distance.cam"), skin, "hud")).left();
        focusInfo.add(focusDistCam).left().padLeft(pad15);
        focusInfo.row();
        focusInfo.add(new OwnLabel(I18n.txt("gui.focusinfo.radius"), skin, "hud")).left();
        focusInfo.add(focusRadius).left().padLeft(pad15);
        focusInfo.row();
        focusInfo.add(moreInfo).left().colspan(2).padBottom(pad5).padTop(pad10);

        // POINTER INFO
        if (!vr) {
            pointerInfo.add(pointerName).width(width).left().colspan(3);
            pointerInfo.row();
            pointerInfo.add(pointerImgBtn1).left().padRight(pad3);
            pointerInfo.add(RADECPointerLabel).left();
            pointerInfo.add(pointerRADEC).left().padLeft(pad15);
            pointerInfo.row();
            pointerInfo.add(pointerImgBtn2).left().padRight(pad3);
            pointerInfo.add(lonLatLabel).left();
            pointerInfo.add(pointerLonLat).left().padLeft(pad15);
            pointerInfo.row();
            pointerInfo.add(viewImgBtn).left().padRight(pad3);
            pointerInfo.add(RADECViewLabel).left();
            pointerInfo.add(viewRADEC).left().padLeft(pad15);
        }

        // CAMERA INFO
        cameraInfo.add(camName).width(width).left().colspan(2);
        cameraInfo.row();
        cameraInfo.add(new OwnLabel(I18n.txt("gui.camera.track"), skin, "hud")).left();
        cameraInfo.add(camTracking).left().padLeft(pad15);
        cameraInfo.row();
        cameraInfo.add(new OwnLabel(I18n.txt("gui.camera.vel"), skin, "hud")).left();
        cameraInfo.add(camVel).left().padLeft(pad15);
        cameraInfo.row();
        cameraInfo.add(new OwnLabel(I18n.txt("gui.focusinfo.distance.sol"), skin, "hud")).left();
        cameraInfo.add(camDistSol).left().padLeft(pad15);
        cameraInfo.row();
        cameraInfo.add(camPos).left().colspan(2);

        // RULER INFO
        rulerInfo.add(rulerName).left();
        rulerInfo.row();
        rulerInfo.add(rulerNameGroup).left();
        rulerInfo.row();
        rulerInfo.add(rulerDist).left();

        focusInfoCell = add(focusInfo).align(Align.left);
        row();
        add(pointerInfo).align(Align.left);
        row();
        add(cameraInfo).align(Align.left);
        row();
        rulerCell = add(rulerInfo).align(Align.left);
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
        final String deg = I18n.txt("gui.unit.deg");
        final Settings s = Settings.settings;
        switch (event) {
        case FOCUS_CHANGED -> {
            IFocus focus;
            if (data[0] instanceof String) {
                focus = (IFocus) GaiaSky.instance.sceneGraph.getNode((String) data[0]);
            } else {
                focus = (IFocus) data[0];
            }
            currentFocus = focus;
            final int focusFieldMaxLength = 20;

            // ID
            boolean cappedId = false;
            String id = "";
            if (focus instanceof IStarFocus) {
                IStarFocus sf = (IStarFocus) focus;
                if (sf.getId() > 0) {
                    id = String.valueOf(sf.getId());
                } else if (sf.getHip() > 0) {
                    id = "HIP " + sf.getHip();
                }
            }
            if (id.length() == 0) {
                id = "-";
            }
            String idString = id;
            if (id.length() > focusFieldMaxLength) {
                idString = TextUtils.capString(id, focusFieldMaxLength);
                cappedId = true;
            }

            // Link
            boolean vis = focus instanceof Planet;
            focusNameGroup.removeActor(landOn);
            focusNameGroup.removeActor(landAt);
            if (vis) {
                focusNameGroup.addActor(landOn);
                focusNameGroup.addActor(landAt);
            }

            // Type
            try {
                focusType.setText(I18n.txt("element." + ComponentType.values()[focus.getCt().getFirstOrdinal()].toString().toLowerCase() + ".singular"));
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
            objectVisibility.addListener(new OwnTextTooltip(I18n.txt("action.visibility", currentFocus.getName()), skin));

            // Force label
            labelVisibility.setCheckedNoFire(!((SceneGraphNode) currentFocus).isForceLabel(currentFocus.getName().toLowerCase().trim()));
            labelVisibility.addListener(new OwnTextTooltip(I18n.txt("action.forcelabel", currentFocus.getName()), skin));

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
            String objectName = TextUtils.capString(focus.getName(), focusFieldMaxLength);
            focusName.setText(objectName);
            focusName.clearListeners();
            focusName.addListener(new OwnTextTooltip(focus.getName(), skin));
            focusNames.clearChildren();
            String[] names = focus.getNames();
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
            Vector2d posSph = focus.getPosSph();
            if (posSph != null && posSph.len() > 0f) {
                focusRA.setText(nf.format(posSph.x) + deg);
                focusDEC.setText(nf.format(posSph.y) + deg);
            } else {
                Coordinates.cartesianToSpherical(focus.getAbsolutePosition(posb), pos);

                focusRA.setText(nf.format(MathUtilsd.radDeg * pos.x % 360) + deg);
                focusDEC.setText(nf.format(MathUtilsd.radDeg * pos.y % 360) + deg);
            }
            if (focus instanceof IProperMotion) {
                IProperMotion part = (IProperMotion) focus;
                focusMuAlpha.setText(nf.format(part.getMuAlpha()) + " " + I18n.txt("gui.unit.masyr"));
                focusMuDelta.setText(nf.format(part.getMuDelta()) + " " + I18n.txt("gui.unit.masyr"));
                focusRadVel.setText(nf.format(part.getRadialVelocity()) + " " + I18n.txt("gui.unit.kms"));
            } else {
                focusMuAlpha.setText("-");
                focusMuDelta.setText("-");
                focusRadVel.setText("-");
            }
            if (focus instanceof StarCluster) {
                // Some star clusters have the number of stars
                // Magnitudes make not sense
                StarCluster sc = (StarCluster) focus;
                if (sc.getNStars() > 0) {
                    appMagEarthLabel.setText("# " + I18n.txt("element.stars"));
                    focusAppMagEarth.setText(Integer.toString(sc.getNStars()));
                } else {
                    appMagEarthLabel.setText("");
                    focusAppMagEarth.setText("");
                }
                focusAppMagCamera.setText("");
                appMagCameraLabel.setText("");
                focusAbsMag.setText("");
                absMagLabel.setText("");

            } else if (focus instanceof CelestialBody) {
                // Planets, satellites, etc.
                // Apparent magnitude depends on absolute magnitude
                // We need to compute the apparent magnitude from earth and camera

                // Apparent magnitude (earth)
                appMagEarthLabel.setText(I18n.txt("gui.focusinfo.appmag.earth"));
                appMagCameraLabel.setText(I18n.txt("gui.focusinfo.appmag.camera"));

                // Absolute magnitude
                absMagLabel.setText(I18n.txt("gui.focusinfo.absmag"));
                focusAbsMag.setText(nf.format(focus.getAbsmag()));

                appMagEarthLabel.addListener(new OwnTextTooltip(I18n.txt("gui.focusinfo.appmag.earth.tooltip"), skin));
                focusAppMagEarth.addListener(new OwnTextTooltip(I18n.txt("gui.focusinfo.appmag.earth.tooltip"), skin));
                appMagCameraLabel.addListener(new OwnTextTooltip(I18n.txt("gui.focusinfo.appmag.camera.tooltip"), skin));
                focusAppMagCamera.addListener(new OwnTextTooltip(I18n.txt("gui.focusinfo.appmag.camera.tooltip"), skin));
                absMagLabel.addListener(new OwnTextTooltip(I18n.txt("gui.focusinfo.absmag.tooltip"), skin));
                focusAbsMag.addListener(new OwnTextTooltip(I18n.txt("gui.focusinfo.absmag.tooltip"), skin));
            } else {
                // Stars, apparent magnitude form Earth is fixed, from camera not so much.

                // Apparent magnitude (earth)
                appMagEarthLabel.setText(I18n.txt("gui.focusinfo.appmag.earth"));
                float appMag = focus.getAppmag();
                focusAppMagEarth.setText(nf.format(appMag));

                // Apparent magnitude (cam)
                appMagCameraLabel.setText(I18n.txt("gui.focusinfo.appmag.camera"));

                // Absolute magnitude
                absMagLabel.setText(I18n.txt("gui.focusinfo.absmag"));
                focusAbsMag.setText(nf.format(focus.getAbsmag()));

                // Tooltips
                appMagEarthLabel.addListener(new OwnTextTooltip(I18n.txt("gui.focusinfo.appmag.earth.tooltip"), skin));
                focusAppMagEarth.addListener(new OwnTextTooltip(I18n.txt("gui.focusinfo.appmag.earth.tooltip"), skin));
                appMagCameraLabel.addListener(new OwnTextTooltip(I18n.txt("gui.focusinfo.appmag.camera.tooltip"), skin));
                focusAppMagCamera.addListener(new OwnTextTooltip(I18n.txt("gui.focusinfo.appmag.camera.tooltip"), skin));
                absMagLabel.addListener(new OwnTextTooltip(I18n.txt("gui.focusinfo.absmag.tooltip"), skin));
                focusAbsMag.addListener(new OwnTextTooltip(I18n.txt("gui.focusinfo.absmag.tooltip"), skin));
            }
            if (ComponentType.values()[focus.getCt().getFirstOrdinal()] == ComponentType.Stars) {
                focusRadius.setText("-");
            } else {
                focusRadius.setText(sf.format(focus.getRadius() * Constants.U_TO_KM) + " " + I18n.txt("gui.unit.km"));
            }

            // Update more info table
            moreInfo.clear();
            if (externalInfoUpdater != null)
                externalInfoUpdater.update(focus);
        }
        case FOCUS_INFO_UPDATED -> {
            focusAngle.setText(sf.format(Math.toDegrees((double) data[1]) % 360) + deg);

            // Dist to cam
            Pair<Double, String> distCam = GlobalResources.doubleToDistanceString((double) data[0], s.program.ui.distanceUnits);
            focusDistCam.setText(sf.format(Math.max(0d, distCam.getFirst())) + " " + distCam.getSecond());

            // Dist to sol
            if (data.length > 4) {
                Pair<Double, String> distSol = GlobalResources.doubleToDistanceString((double) data[4], s.program.ui.distanceUnits);
                focusDistSol.setText(sf.format(Math.max(0d, distSol.getFirst())) + " " + distSol.getSecond());
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
            Pair<Double, String> x = GlobalResources.doubleToDistanceString(campos.x, s.program.ui.distanceUnits);
            Pair<Double, String> y = GlobalResources.doubleToDistanceString(campos.y, s.program.ui.distanceUnits);
            Pair<Double, String> z = GlobalResources.doubleToDistanceString(campos.z, s.program.ui.distanceUnits);
            String camPosStr = "[" + sf.format(x.getFirst()) + " " + x.getSecond() + ", " + sf.format(y.getFirst()) + " " + y.getSecond() + ", " + sf.format(z.getFirst()) + " " + z.getSecond() + "]";
            camVel.setText(sf.format((double) data[1]) + " " + I18n.txt("gui.unit.kmh"));
            camPos.setText(TextUtils.capString(camPosStr, 40));
            camPos.addListener(new OwnTextTooltip(camPosStr, skin));
            Pair<Double, String> distSol = GlobalResources.doubleToDistanceString(campos.lend(), s.program.ui.distanceUnits);
            camDistSol.setText(sf.format(Math.max(0d, distSol.getFirst())) + " " + distSol.getSecond());
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
            Double pra = (Double) data[0];
            Double pdec = (Double) data[1];
            Double vra = (Double) data[2];
            Double vdec = (Double) data[3];
            pointerRADEC.setText(nf.format(pra) + deg + "/" + nf.format(pdec) + deg);
            viewRADEC.setText(nf.format(vra) + deg + "/" + nf.format(vdec) + deg);
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
            rulerDist.setText(I18n.txt("gui.sc.distance") + ": -");
            hideInfo(rulerCell);
        }
        case RULER_DIST -> {
            String rd = (String) data[1];
            rulerDist.setText(I18n.txt("gui.sc.distance") + ": " + rd);
        }
        case PER_OBJECT_VISIBILITY_CMD -> {
            if (source != objectVisibility) {
                IVisibilitySwitch vs = (IVisibilitySwitch) data[0];
                String name = (String) data[1];
                if (vs == currentFocus && currentFocus.hasName(name)) {
                    boolean visible = (boolean) data[2];
                    objectVisibility.setCheckedNoFire(!visible);
                }
            }
        }
        case FORCE_OBJECT_LABEL_CMD -> {
            if (source != labelVisibility) {
                SceneGraphNode sgn = (SceneGraphNode) data[0];
                String name = (String) data[1];
                if (sgn == currentFocus && currentFocus.hasName(name)) {
                    boolean forceLabel = (boolean) data[2];
                    labelVisibility.setCheckedNoFire(forceLabel);
                }
            }
        }
        default -> {
        }
        }

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void displayInfo(Cell cell, Actor info) {
        cell.setActor(info);
        pack();
    }

    @SuppressWarnings({ "rawtypes" })
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
