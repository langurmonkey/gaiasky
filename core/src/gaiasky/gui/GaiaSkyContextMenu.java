/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.camera.CameraManager.CameraMode;
import gaiasky.scene.view.FocusView;
import gaiasky.util.CatalogInfo;
import gaiasky.util.CatalogManager;
import gaiasky.util.Settings;
import gaiasky.util.TextUtils;
import gaiasky.util.camera.CameraUtils;
import gaiasky.util.gravwaves.RelativisticEffectsManager;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Vector3d;
import gaiasky.util.scene2d.ContextMenu;
import gaiasky.util.scene2d.MenuItem;
import gaiasky.util.scene2d.OwnCheckBox;
import gaiasky.util.scene2d.OwnImage;

import java.util.Collection;

public class GaiaSkyContextMenu extends ContextMenu {

    // The skin
    private final Skin skin;
    // The candidate, if any
    private final FocusView candidate;
    // Screen coordinates
    private final int screenX;
    private final int screenY;
    // Default pad
    private final float pad;
    private final CatalogManager catalogManager;
    private final Scene scene;
    // Rel effects off
    private final boolean relativisticEffects = false;
    // The name of the candidate
    private String candidateName;
    // Short name of candidate
    private String candidateNameShort;
    // Added items
    private int nItems = 0;

    public GaiaSkyContextMenu(final Skin skin, final String styleName, final int screenX, final int screenY, final FocusView candidate, final CatalogManager catalogManager, final Scene scene) {
        super(skin, styleName);
        this.skin = skin;
        this.screenX = (int) (screenX / Settings.settings.program.ui.scale);
        this.screenY = screenY;
        this.candidate = candidate;
        this.catalogManager = catalogManager;
        this.pad = 8f;
        this.scene = scene;
        if (candidate != null && candidate.isValid()) {
            this.candidateName = candidate.getCandidateName();
            this.candidateNameShort = TextUtils.capString(this.candidateName, 10);
        }
        build();
    }

    public void addItem(MenuItem item) {
        super.addItem(item);
        nItems++;
    }

    private void build() {
        Drawable rulerDwb = skin.getDrawable("icon-elem-ruler");
        if (candidate != null && candidate.isValid()) {
            MenuItem select = new MenuItem(I18n.msg("context.select", candidateNameShort), skin, skin.getDrawable("highlight-off"));
            select.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    EventManager.publish(Event.CAMERA_MODE_CMD, select, CameraMode.FOCUS_MODE);
                    EventManager.publish(Event.FOCUS_CHANGE_CMD, select, candidate);
                }
                return false;
            });
            addItem(select);

            MenuItem go = new MenuItem(I18n.msg("context.goto", candidateNameShort), skin, skin.getDrawable("go-to"));
            go.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    candidate.makeFocus();
                    EventManager.publish(Event.NAVIGATE_TO_OBJECT, go, candidate);
                }
                return false;
            });
            addItem(go);

            addSeparator();

            // Tracking object
            MenuItem track = new MenuItem(I18n.msg("context.track", candidateNameShort), skin, skin.getDrawable("highlight-on"));
            track.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    EventManager.publish(Event.CAMERA_TRACKING_OBJECT_CMD, track, candidate.getEntity(), candidateName);
                }
                return false;
            });
            addItem(track);

            MenuItem noTrack = new MenuItem(I18n.msg("context.notrack", candidateNameShort), skin, skin.getDrawable("iconic-delete"));
            noTrack.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    EventManager.publish(Event.CAMERA_TRACKING_OBJECT_CMD, noTrack, (Object) null, (Object) null);
                }
                return false;
            });
            addItem(noTrack);

            addSeparator();

            // Add bounding shape at object position
            MenuItem addShape = new MenuItem(I18n.msg("context.shape.new", candidateNameShort), skin, skin.getDrawable("icon-elem-grids"));
            addShape.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    AddShapeDialog dialog = new AddShapeDialog(I18n.msg("context.shape.new", candidateNameShort), candidate, candidateName, skin, getStage());
                    dialog.setAcceptRunnable(() -> {
                        double size = dialog.units.getSelected().unit.toKm(dialog.size.getDoubleValue(1)) * 2.0;
                        float[] color = dialog.color.getPickedColor();
                        String shape = dialog.shape.getSelected().shape.toString();
                        String primitive = dialog.primitive.getSelected().primitive.toString();
                        boolean showLabel = dialog.showLabel.isChecked();
                        boolean trackObj = dialog.track.isChecked();
                        GaiaSky.instance.scripting().addShapeAroundObject(dialog.name.getText().trim(), shape, primitive, size, candidateName, color[0], color[1], color[2], color[3], showLabel, trackObj);
                    });
                    dialog.show(getStage());
                    return true;
                }
                return false;
            });
            addItem(addShape);

            MenuItem removeShapesObj = new MenuItem(I18n.msg("context.shape.remove", candidateNameShort), skin, skin.getDrawable("iconic-delete"));
            removeShapesObj.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    GaiaSky.postRunnable(() -> {
                        ImmutableArray<Entity> shapes = scene.engine.getEntitiesFor(scene.getFamilies().shapes);
                        for (Entity entity : shapes) {
                            var shape = Mapper.shape.get(entity);
                            if (shape.track != null && shape.track.getEntity() == candidate.getEntity()) {
                                EventManager.publish(Event.SCENE_REMOVE_OBJECT_NO_POST_CMD, removeShapesObj, entity, false);
                            } else if (shape.trackName != null && shape.trackName.equalsIgnoreCase(candidateName)) {
                                EventManager.publish(Event.SCENE_REMOVE_OBJECT_NO_POST_CMD, removeShapesObj, entity, false);
                            }
                        }
                    });
                }
                return false;
            });
            addItem(removeShapesObj);

            MenuItem removeShapesAll = new MenuItem(I18n.msg("context.shape.remove.all"), skin, skin.getDrawable("iconic-delete"));
            removeShapesAll.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    GaiaSky.postRunnable(() -> {
                        ImmutableArray<Entity> shapes = scene.engine.getEntitiesFor(scene.getFamilies().shapes);
                        for (Entity entity : shapes) {
                            var shape = Mapper.shape.get(entity);
                            if (shape.track != null || shape.trackName != null) {
                                EventManager.publish(Event.SCENE_REMOVE_OBJECT_NO_POST_CMD, removeShapesAll, entity, false);
                            }
                        }
                    });

                }
                return false;
            });
            addItem(removeShapesAll);

            if (Mapper.atmosphere.has(candidate.getEntity())) {
                addSeparator();

                MenuItem landOn = new MenuItem(I18n.msg("context.landon", candidateNameShort), skin, skin.getDrawable("land-on"));
                landOn.addListener(event -> {
                    if (event instanceof ChangeEvent) {
                        EventManager.publish(Event.LAND_ON_OBJECT, landOn, candidate);
                        return true;
                    }
                    return false;
                });
                addItem(landOn);

                double[] lonlat = new double[2];
                FocusView view = candidate;
                boolean ok = CameraUtils.getLonLat(view, view.getEntity(), GaiaSky.instance.getICamera(), Gdx.input.getX(), Gdx.input.getY(), new Vector3(), new Vector3(), new Vector3(), new Vector3(), new Vector3d(), new Vector3d(), new Matrix4(), lonlat);
                if (ok) {
                    final Double pointerLon = lonlat[0];
                    final Double pointerLat = lonlat[1];
                    // Add mouse pointer
                    MenuItem landOnPointer = new MenuItem(I18n.msg("context.landatpointer", candidateNameShort), skin, skin.getDrawable("land-on"));
                    landOnPointer.addListener(event -> {
                        if (event instanceof ChangeEvent) {
                            EventManager.publish(Event.LAND_AT_LOCATION_OF_OBJECT, landOnPointer, candidate, pointerLon, pointerLat);
                            return true;
                        }
                        return false;
                    });
                    addItem(landOnPointer);
                }

                MenuItem landOnCoord = new MenuItem(I18n.msg("context.landatcoord", candidateNameShort), skin, skin.getDrawable("land-at"));
                landOnCoord.addListener(event -> {
                    if (event instanceof ChangeEvent) {
                        EventManager.publish(Event.SHOW_LAND_AT_LOCATION_ACTION, landOnCoord, candidate);
                        return true;
                    }
                    return false;
                });
                addItem(landOnCoord);

                addSeparator();

                MenuItem proceduralSurface = new MenuItem(I18n.msg("context.proceduralmenu", candidateNameShort), skin, skin.getDrawable("iconic-infinity"));
                proceduralSurface.addListener(event -> {
                    if (event instanceof ChangeEvent) {
                        EventManager.publish(Event.SHOW_PROCEDURAL_GEN_ACTION, proceduralSurface, candidate);
                        return true;
                    }
                    return false;
                });
                addItem(proceduralSurface);

            }

            addSeparator();

            // Cosmic ruler
            Entity cr = scene.getEntity("Cosmicruler");
            if (cr != null) {
                var ruler = Mapper.ruler.get(cr);
                MenuItem rulerAttach0 = null, rulerAttach1 = null;
                if (!ruler.hasObject0() && !ruler.hasObject1()) {
                    // No objects attached
                    rulerAttach0 = new MenuItem(I18n.msg("context.ruler.attach", "0", candidateNameShort), skin, rulerDwb);
                    final MenuItem ra = rulerAttach0;
                    rulerAttach0.addListener((ev) -> {
                        if (ev instanceof ChangeEvent) {
                            EventManager.publish(Event.RULER_ATTACH_0, ra, candidateName);
                            return true;
                        }
                        return false;
                    });
                } else if (ruler.hasObject0() && !ruler.hasObject1()) {
                    // Only 0 is attached
                    rulerAttach1 = new MenuItem(I18n.msg("context.ruler.attach", "1", candidateNameShort), skin, rulerDwb);
                    final MenuItem ra = rulerAttach1;
                    rulerAttach1.addListener((ev) -> {
                        if (ev instanceof ChangeEvent) {
                            EventManager.publish(Event.RULER_ATTACH_1, ra, candidateName);
                            return true;
                        }
                        return false;
                    });
                } else {
                    // All attached, show both
                    rulerAttach0 = new MenuItem(I18n.msg("context.ruler.attach", "0", candidateNameShort), skin, rulerDwb);
                    final MenuItem ra0 = rulerAttach0;
                    rulerAttach0.addListener((ev) -> {
                        if (ev instanceof ChangeEvent) {
                            GaiaSky.postRunnable(() -> {
                                EventManager.publish(Event.RULER_ATTACH_0, ra0, candidateName);
                            });
                            return true;
                        }
                        return false;
                    });
                    rulerAttach1 = new MenuItem(I18n.msg("context.ruler.attach", "1", candidateNameShort), skin, rulerDwb);
                    final MenuItem ra1 = rulerAttach1;
                    rulerAttach1.addListener((ev) -> {
                        if (ev instanceof ChangeEvent) {
                            GaiaSky.postRunnable(() -> {
                                EventManager.publish(Event.RULER_ATTACH_1, ra1, candidateName);
                            });
                            return true;
                        }
                        return false;
                    });
                }
                if (rulerAttach0 != null)
                    addItem(rulerAttach0);
                if (rulerAttach1 != null)
                    addItem(rulerAttach1);
            }
        }

        // Clear ruler
        Entity cr = scene.getEntity("Cosmicruler");
        if (cr != null) {
            var ruler = Mapper.ruler.get(cr);
            if (ruler.rulerOk() || ruler.hasAttached()) {
                MenuItem clearRuler = new MenuItem(I18n.msg("context.ruler.clear"), skin, rulerDwb);
                clearRuler.addListener((evt) -> {
                    if (evt instanceof ChangeEvent) {
                        GaiaSky.postRunnable(() -> {
                            EventManager.publish(Event.RULER_CLEAR, clearRuler);
                        });
                        return true;
                    }
                    return false;
                });
                addItem(clearRuler);
            }
        }

        // Load
        MenuItem dsLoad = new MenuItem(I18n.msg("context.dataset.load"), skin, skin.getDrawable("open-icon"));
        dsLoad.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.SHOW_LOAD_CATALOG_ACTION, dsLoad);
                return true;
            }
            return false;
        });
        addItem(dsLoad);

        // Dataset highlight
        Collection<CatalogInfo> cis = catalogManager.getCatalogInfos();
        if (cis != null && cis.size() > 0) {
            MenuItem dsHighlight = new MenuItem(I18n.msg("context.dataset.highlight"), skin, skin.getDrawable("highlight-on"));
            ContextMenu dsHighlightSubmenu = new ContextMenu(skin, "default");
            for (CatalogInfo ci : cis) {
                if (ci.isVisible()) {
                    MenuItem cim = new MenuItem(ci.name, skin, "default");
                    cim.align(Align.right);
                    OwnCheckBox cb = new OwnCheckBox(null, skin, pad);
                    cb.setChecked(ci.highlighted);
                    cim.add(cb).right().expand();
                    cim.addListener(event -> {
                        if (event instanceof ChangeEvent) {
                            EventManager.publish(Event.CATALOG_HIGHLIGHT, cim, ci, !ci.highlighted);
                            return true;
                        }
                        return false;
                    });
                    dsHighlightSubmenu.addItem(cim);
                }
            }
            dsHighlight.setSubMenu(dsHighlightSubmenu);
            addItem(dsHighlight);
        }

        // Dataset visibility
        if (cis != null && cis.size() > 0) {
            MenuItem dsVisibility = new MenuItem(I18n.msg("context.dataset.visibility"), skin, skin.getDrawable("eye-icon"));
            ContextMenu dsVisibilitySubmenu = new ContextMenu(skin, "default");
            for (CatalogInfo ci : cis) {
                MenuItem cim = new MenuItem(ci.name, skin, "default");
                cim.align(Align.right);

                Drawable icon = ci.isVisible(true) ? skin.getDrawable("eye-icon") : skin.getDrawable("eye-closed-icon");
                OwnImage img = new OwnImage(icon);
                cim.add(img).right().padRight(pad).expand();
                cim.addListener(event -> {
                    if (event instanceof ChangeEvent) {
                        EventManager.publish(Event.CATALOG_VISIBLE, cim, ci.name, !ci.isVisible(true));
                        return true;
                    }
                    return false;
                });
                dsVisibilitySubmenu.addItem(cim);
            }
            dsVisibility.setSubMenu(dsVisibilitySubmenu);
            addItem(dsVisibility);
        }

        MenuItem VRUI = new MenuItem("VR UI", skin, "default");
        VRUI.addListener(event -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.SHOW_VR_UI, this);
                return true;
            }
            return false;
        });
        //addItem(VRUI);

        if (relativisticEffects) {
            // Spawn gravitational waves
            MenuItem gravWaveStart = new MenuItem(I18n.msg("context.startgravwave"), skin, "default");
            gravWaveStart.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    EventManager.publish(Event.GRAV_WAVE_START, gravWaveStart, screenX, screenY);
                    return true;
                }
                return false;
            });
            addItem(gravWaveStart);

            if (RelativisticEffectsManager.getInstance().gravWavesOn()) {
                // Cancel gravitational waves
                MenuItem gravWaveStop = new MenuItem(I18n.msg("context.stopgravwave"), skin, "default");
                gravWaveStop.addListener(event -> {
                    if (event instanceof ChangeEvent) {
                        EventManager.publish(Event.GRAV_WAVE_STOP, gravWaveStop);
                        return true;
                    }
                    return false;
                });
                addItem(gravWaveStop);
            }
        }

        if (nItems > 0) {
            addSeparator();
        }
        // Quit
        MenuItem quit = new MenuItem(I18n.msg("context.quit"), skin, skin.getDrawable("quit-icon"));
        quit.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.SHOW_QUIT_ACTION, quit);
                return true;
            }
            return false;
        });
        addItem(quit);
    }
}
