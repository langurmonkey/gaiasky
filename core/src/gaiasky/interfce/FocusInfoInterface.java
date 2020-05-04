/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interfce;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.SnapshotArray;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
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
import gaiasky.util.math.Vector3d;
import gaiasky.util.scene2d.*;

/**
 * Part of the user interface which holds the information on the current focus
 * object and on the camera.
 *
 * @author tsagrista
 */
public class FocusInfoInterface extends TableGuiInterface implements IObserver {
    static private INetworkChecker daemon;
    static private int MAX_RULER_NAME_LEN = 9;

    protected Skin skin;
    protected OwnLabel focusName, focusType, focusId, focusRA, focusDEC, focusMuAlpha, focusMuDelta, focusRadVel, focusAngle, focusDistCam, focusDistSol, focusAppMag, focusAbsMag, focusRadius;
    protected Button goTo, landOn, landAt, bookmark;
    protected OwnLabel pointerName, pointerLonLat, pointerRADEC, viewRADEC;
    protected OwnLabel camName, camVel, camPos, lonLatLabel, RADECPointerLabel, RADECViewLabel, appmagLabel, absmagLabel;
    protected OwnLabel rulerName, rulerName0, rulerName1, rulerDist;
    protected OwnLabel focusIdExpand;

    protected HorizontalGroup focusNameGroup;

    protected IFocus currentFocus;

    private Table focusInfo, pointerInfo, cameraInfo, moreInfo, rulerInfo, focusNames;
    private Cell<?> focusInfoCell, rulerCell;
    Vector3d pos;

    INumberFormat nf, sf;

    float pad1, pad3, pad5, pad10, pad15, bw;

    public FocusInfoInterface(Skin skin) {
        this(skin, false);
    }

    public FocusInfoInterface(Skin skin, boolean vr) {
        super(skin);
        this.setBackground("table-bg");
        this.skin = skin;

        nf = NumberFormatFactory.getFormatter("##0.##");
        sf = NumberFormatFactory.getFormatter("#0.##E0");

        float buttonSize = 15f * GlobalConf.UI_SCALE_FACTOR;
        float imgSize = 18f * GlobalConf.UI_SCALE_FACTOR;
        pad15 = 15f * GlobalConf.UI_SCALE_FACTOR;
        pad10 = 10f * GlobalConf.UI_SCALE_FACTOR;
        pad5 = 5f * GlobalConf.UI_SCALE_FACTOR;
        pad3 = 3f * GlobalConf.UI_SCALE_FACTOR;
        pad1 = 1f * GlobalConf.UI_SCALE_FACTOR;

        focusInfo = new Table();
        focusInfo.pad(pad5);
        cameraInfo = new Table();
        cameraInfo.pad(pad5);
        pointerInfo = new Table();
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
        focusAppMag = new OwnLabel("", skin, "hud");
        focusAbsMag = new OwnLabel("", skin, "hud");
        focusAngle = new OwnLabel("", skin, "hud");
        focusDistSol = new OwnLabel("", skin, "hud");
        focusDistCam = new OwnLabel("", skin, "hud");
        focusRadius = new OwnLabel("", skin, "hud");

        // Labels
        appmagLabel = new OwnLabel(I18n.txt("gui.focusinfo.appmag"), skin, "hud");
        absmagLabel = new OwnLabel(I18n.txt("gui.focusinfo.absmag"), skin, "hud");

        // Pointer
        pointerName = new OwnLabel(I18n.bundle.get("gui.pointer"), skin, "hud-header");
        pointerRADEC = new OwnLabel("", skin, "hud");
        pointerLonLat = new OwnLabel("", skin, "hud");
        viewRADEC = new OwnLabel("", skin, "hud");
        lonLatLabel = new OwnLabel("Lat/Lon", skin, "hud");
        RADECPointerLabel = new OwnLabel(I18n.txt("gui.focusinfo.alpha") + "/" + I18n.txt("gui.focusinfo.delta"), skin, "hud");
        RADECViewLabel = new OwnLabel(I18n.txt("gui.focusinfo.alpha") + "/" + I18n.txt("gui.focusinfo.delta"), skin, "hud");
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
        camName = new OwnLabel(I18n.bundle.get("gui.camera"), skin, "hud-header");
        camVel = new OwnLabel("", skin, "hud");
        camPos = new OwnLabel("", skin, "hud");

        // Ruler
        rulerName = new OwnLabel(I18n.bundle.get("gui.ruler.title"), skin, "hud-header");
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
                    EventManager.instance.post(Events.BOOKMARKS_ADD, currentFocus.getName(), false);
                else
                    EventManager.instance.post(Events.BOOKMARKS_REMOVE_ALL, currentFocus.getName());
            }
            return false;
        });

        // GoTo, LandOn and LandAt
        goTo = new OwnTextIconButton("", skin, "go-to");
        goTo.setSize(buttonSize, buttonSize);
        goTo.addListener((event) -> {
            if (currentFocus != null && event instanceof ChangeEvent) {
                EventManager.instance.post(Events.NAVIGATE_TO_OBJECT, currentFocus);
                return true;
            }
            return false;

        });
        goTo.addListener(new OwnTextTooltip(I18n.txt("gui.focusinfo.goto"), skin));

        landOn = new OwnTextIconButton("", skin, "land-on");
        landOn.setSize(buttonSize, buttonSize);
        landOn.addListener((event) -> {
            if (currentFocus != null && event instanceof ChangeEvent) {
                EventManager.instance.post(Events.LAND_ON_OBJECT, currentFocus);
                return true;
            }
            return false;

        });
        landOn.addListener(new OwnTextTooltip(I18n.txt("gui.focusinfo.landon"), skin));

        landAt = new OwnTextIconButton("", skin, "land-at");
        landAt.setSize(buttonSize, buttonSize);
        landAt.addListener((event) -> {
            if (currentFocus != null && event instanceof ChangeEvent) {
                EventManager.instance.post(Events.SHOW_LAND_AT_LOCATION_ACTION, currentFocus);
                return true;
            }
            return false;
        });
        landAt.addListener(new OwnTextTooltip(I18n.txt("gui.focusinfo.landat"), skin));

        bw = Math.max(landOn.getWidth(), landAt.getWidth());
        bw += 2 * GlobalConf.UI_SCALE_FACTOR;

        goTo.setWidth(bw);
        landOn.setWidth(bw);
        landAt.setWidth(bw);

        focusNameGroup = new HorizontalGroup();
        focusNameGroup.space(pad5);
        focusNameGroup.addActor(focusName);
        focusNameGroup.addActor(bookmark);
        focusNameGroup.addActor(goTo);
        focusNameGroup.addActor(landOn);
        focusNameGroup.addActor(landAt);

        float w = 130 * GlobalConf.UI_SCALE_FACTOR;
        focusId.setWidth(w);

        focusRA.setWidth(w);
        focusDEC.setWidth(w);
        focusMuAlpha.setWidth(w);
        focusMuDelta.setWidth(w);
        focusRadVel.setWidth(w);
        focusAngle.setWidth(w);
        focusDistSol.setWidth(w);
        focusDistCam.setWidth(w);
        camVel.setWidth(w);

        /** FOCUS INFO **/
        focusInfo.add(focusNameGroup).left().colspan(2).padBottom(pad5);
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
            focusInfo.add(appmagLabel).left();
            focusInfo.add(focusAppMag).left().padLeft(pad15);
            focusInfo.row();
            focusInfo.add(absmagLabel).left();
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

        /** POINTER INFO **/
        if (!vr) {
            pointerInfo.add(pointerName).left().colspan(3);
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

        /** CAMERA INFO **/
        cameraInfo.add(camName).left().colspan(2);
        cameraInfo.row();
        cameraInfo.add(new OwnLabel(I18n.txt("gui.camera.vel"), skin, "hud")).left();
        cameraInfo.add(camVel).left().padLeft(pad15);
        cameraInfo.row();
        cameraInfo.add(camPos).left().colspan(2);

        /** RULER INFO **/
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

        if (daemon == null && !vr) {
            daemon = NetworkCheckerManager.getNewtorkChecker();
            daemon.setParameters(moreInfo, skin, pad10);
            daemon.start();
        }

        pos = new Vector3d();
        EventManager.instance.subscribe(this, Events.FOCUS_CHANGED, Events.FOCUS_INFO_UPDATED, Events.CAMERA_MOTION_UPDATED, Events.CAMERA_MODE_CMD, Events.LON_LAT_UPDATED, Events.RA_DEC_UPDATED, Events.RULER_ATTACH_0, Events.RULER_ATTACH_1, Events.RULER_CLEAR, Events.RULER_DIST);
    }

    private HorizontalGroup hg(Actor... actors) {
        HorizontalGroup hg = new HorizontalGroup();
        for (Actor a : actors)
            hg.addActor(a);
        return hg;
    }

    private void scaleFonts(SnapshotArray<Actor> a, float scl) {
        for (Actor actor : a) {
            if (actor instanceof Group) {
                Group g = (Group) actor;
                scaleFonts(g.getChildren(), scl);
            } else if (actor instanceof Label) {
                Label l = (Label) actor;
                l.setScale(scl);
            }
        }
    }

    private void unsubscribe() {
        EventManager.instance.removeAllSubscriptions(this);
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
            case FOCUS_CHANGED:
                IFocus focus;
                if (data[0] instanceof String) {
                    focus = (IFocus) GaiaSky.instance.sg.getNode((String) data[0]);
                } else {
                    focus = (IFocus) data[0];
                }
                currentFocus = focus;

                final int focusFieldMaxLength = !GlobalConf.isHiDPI() ? 17 : 20;

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

                // Coords
                pointerLonLat.setText("-/-");

                // Bookmark
                bookmark.setProgrammaticChangeEvents(false);
                bookmark.setChecked(BookmarksManager.instance().containsName(currentFocus.getName()));
                bookmark.setProgrammaticChangeEvents(true);

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
                    focusRA.setText(nf.format(posSph.x) + "°");
                    focusDEC.setText(nf.format(posSph.y) + "°");
                } else {
                    Coordinates.cartesianToSpherical(focus.getAbsolutePosition(pos), pos);

                    focusRA.setText(nf.format(MathUtilsd.radDeg * pos.x % 360) + "°");
                    focusDEC.setText(nf.format(MathUtilsd.radDeg * pos.y % 360) + "°");
                }

                if (focus instanceof IProperMotion) {
                    IProperMotion part = (IProperMotion) focus;
                    focusMuAlpha.setText(nf.format(part.getMuAlpha()) + " mas/yr");
                    focusMuDelta.setText(nf.format(part.getMuDelta()) + " mas/yr");
                    focusRadVel.setText(nf.format(part.getRadialVelocity()) + " km/s");
                } else {
                    focusMuAlpha.setText("-");
                    focusMuDelta.setText("-");
                    focusRadVel.setText("-");
                }

                if (!(focus instanceof StarCluster)) {
                    appmagLabel.setText(I18n.txt("gui.focusinfo.appmag"));
                    Float appmag = focus.getAppmag();
                    focusAppMag.setText(nf.format(appmag));
                    absmagLabel.setText(I18n.txt("gui.focusinfo.absmag"));
                    Float absmag = focus.getAbsmag();
                    focusAbsMag.setText(nf.format(absmag));
                } else {
                    appmagLabel.setText("# " + I18n.txt("element.stars"));
                    StarCluster sc = (StarCluster) focus;
                    focusAppMag.setText(Integer.toString(sc.getNStars()));
                    absmagLabel.setText("");
                    focusAbsMag.setText("");
                }

                if (ComponentType.values()[focus.getCt().getFirstOrdinal()] == ComponentType.Stars) {
                    focusRadius.setText("-");
                } else {
                    focusRadius.setText(sf.format(focus.getRadius() * Constants.U_TO_KM) + " km");
                }

                // Update more info table
                if (!daemon.executing()) {
                    moreInfo.clear();
                    daemon.setFocus(focus);
                    daemon.doNotify();
                }

                break;
            case FOCUS_INFO_UPDATED:
                focusAngle.setText(sf.format(Math.toDegrees((double) data[1]) % 360) + "°");

                // Dist to cam
                Pair<Double, String> distCam = GlobalResources.doubleToDistanceString((double) data[0]);
                focusDistCam.setText(sf.format(Math.max(0d, distCam.getFirst())) + " " + distCam.getSecond());

                // Dist to sol
                if (data.length > 4) {
                    Pair<Double, String> distSol = GlobalResources.doubleToDistanceString((double) data[4]);
                    focusDistSol.setText(sf.format(Math.max(0d, distSol.getFirst())) + " " + distSol.getSecond());
                }

                focusRA.setText(nf.format((double) data[2] % 360) + "°");
                focusDEC.setText(nf.format((double) data[3] % 360) + "°");
                break;
            case CAMERA_MOTION_UPDATED:
                Vector3d campos = (Vector3d) data[0];
                Pair<Double, String> x = GlobalResources.doubleToDistanceString(campos.x);
                Pair<Double, String> y = GlobalResources.doubleToDistanceString(campos.y);
                Pair<Double, String> z = GlobalResources.doubleToDistanceString(campos.z);
                camPos.setText("X: " + sf.format(x.getFirst()) + " " + x.getSecond() + "\nY: " + sf.format(y.getFirst()) + " " + y.getSecond() + "\nZ: " + sf.format(z.getFirst()) + " " + z.getSecond());
                camVel.setText(sf.format((double) data[1]) + " km/h");
                break;
            case CAMERA_MODE_CMD:
                // Update camera mode selection
                CameraMode mode = (CameraMode) data[0];
                if (mode.equals(CameraMode.FOCUS_MODE)) {
                    displayInfo(focusInfoCell, focusInfo);
                } else {
                    hideInfo(focusInfoCell);
                }
                break;
            case LON_LAT_UPDATED:
                Double lon = (Double) data[0];
                Double lat = (Double) data[1];
                pointerLonLat.setText(nf.format(lat) + "°/" + nf.format(lon) + "°");
                break;
            case RA_DEC_UPDATED:
                Double pra = (Double) data[0];
                Double pdec = (Double) data[1];
                Double vra = (Double) data[2];
                Double vdec = (Double) data[3];
                pointerRADEC.setText(nf.format(pra) + "°/" + nf.format(pdec) + "°");
                viewRADEC.setText(nf.format(vra) + "°/" + nf.format(vdec) + "°");
                break;
            case RULER_ATTACH_0:
                String n0 = (String) data[0];
                rulerName0.setText(capString(n0, MAX_RULER_NAME_LEN));
                displayInfo(rulerCell, rulerInfo);
                break;
            case RULER_ATTACH_1:
                String n1 = (String) data[0];
                rulerName1.setText(capString(n1, MAX_RULER_NAME_LEN));
                displayInfo(rulerCell, rulerInfo);
                break;
            case RULER_CLEAR:
                rulerName0.setText("-");
                rulerName1.setText("-");
                rulerDist.setText(I18n.bundle.get("gui.sc.distance") + ": -");
                hideInfo(rulerCell);
                break;
            case RULER_DIST:
                String rd = (String) data[1];
                rulerDist.setText(I18n.bundle.get("gui.sc.distance") + ": " + rd);
                break;
            default:
                break;
        }

    }

    private String capString(String in, int maxLen) {
        if (in.length() > maxLen) {
            return in.substring(0, maxLen) + "...";
        }
        return in;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
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
