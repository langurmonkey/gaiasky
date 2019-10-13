/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import gaia.cu9.ari.gaiaorbit.GaiaSky;
import gaia.cu9.ari.gaiaorbit.data.stars.UncertaintiesHandler;
import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.scenegraph.*;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.CameraManager.CameraMode;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.camera.CameraUtils;
import gaia.cu9.ari.gaiaorbit.util.gravwaves.RelativisticEffectsManager;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;
import gaia.cu9.ari.gaiaorbit.util.scene2d.ContextMenu;
import gaia.cu9.ari.gaiaorbit.util.scene2d.MenuItem;

public class GaiaSkyContextMenu extends ContextMenu {

    // The skin
    private Skin skin;
    // The candidate, if any
    private IFocus candidate;
    // The name of the candidate
    private String cname;
    // Screen coordinates
    private int screenX, screenY;
    // Scene graph
    private ISceneGraph sg;

    // Added items
    private int nItems = 0;

    // Uncertainties disabled by default
    private boolean uncertainties = false;
    // Rel effects off
    private boolean releffects = false;

    public GaiaSkyContextMenu(Skin skin, String styleName, int screenX, int screenY, IFocus candidate) {
        super(skin, styleName);
        this.skin = skin;
        this.screenX = screenX;
        this.screenY = screenY;
        this.sg = GaiaSky.instance.sg;
        this.candidate = candidate;
        if (candidate != null)
            this.cname = candidate.getCandidateName();
        build();
    }

    public void addItem(MenuItem item) {
        super.addItem(item);
        nItems++;
    }

    private void build() {
        if (candidate != null) {
            MenuItem select = new MenuItem(I18n.txt("context.select", cname), skin, "default");
            select.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    EventManager.instance.post(Events.CAMERA_MODE_CMD, CameraMode.FOCUS_MODE);
                    EventManager.instance.post(Events.FOCUS_CHANGE_CMD, candidate);
                }
                return false;
            });
            addItem(select);

            MenuItem go = new MenuItem(I18n.txt("context.goto", cname), skin, "default");
            go.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    candidate.makeFocus();
                    EventManager.instance.post(Events.NAVIGATE_TO_OBJECT, candidate);
                }
                return false;
            });
            addItem(go);

            if (candidate instanceof Planet) {
                addSeparator();

                MenuItem landOn = new MenuItem(I18n.txt("context.landon", cname), skin, "default");
                landOn.addListener(event -> {
                    if (event instanceof ChangeEvent) {
                        EventManager.instance.post(Events.LAND_ON_OBJECT, candidate);
                        return true;
                    }
                    return false;
                });
                addItem(landOn);

                double[] lonlat = new double[2];
                boolean ok = CameraUtils.getLonLat((Planet) candidate, GaiaSky.instance.getICamera(), screenX, screenY, new Vector3(), new Vector3(), new Vector3(), new Vector3(), new Vector3d(), new Vector3d(), new Matrix4(), lonlat);
                if (ok) {
                    final Double pointerLon = lonlat[0];
                    final Double pointerLat = lonlat[1];
                    // Add mouse pointer
                    MenuItem landOnPointer = new MenuItem(I18n.txt("context.landatpointer", cname), skin, "default");
                    landOnPointer.addListener(event -> {
                        if (event instanceof ChangeEvent) {
                            EventManager.instance.post(Events.LAND_AT_LOCATION_OF_OBJECT, candidate, pointerLon, pointerLat);
                            return true;
                        }
                        return false;
                    });
                    addItem(landOnPointer);
                }

                MenuItem landOnCoord = new MenuItem(I18n.txt("context.landatcoord", cname), skin, "default");
                landOnCoord.addListener(event -> {
                    if (event instanceof ChangeEvent) {
                        EventManager.instance.post(Events.SHOW_LAND_AT_LOCATION_ACTION, candidate);
                        return true;
                    }
                    return false;
                });
                addItem(landOnCoord);

            }

            if (candidate instanceof IStarFocus && uncertainties) {
                boolean sep = false;
                if (UncertaintiesHandler.getInstance().containsStar(candidate.getCandidateId())) {
                    addSeparator();
                    sep = true;

                    MenuItem showUncertainties = new MenuItem(I18n.txt("context.showuncertainties"), skin, "default");
                    showUncertainties.addListener(event -> {
                        if (event instanceof ChangeEvent) {
                            EventManager.instance.post(Events.SHOW_UNCERTAINTIES, candidate);
                            return true;
                        }
                        return false;
                    });
                    addItem(showUncertainties);
                }

                if (UncertaintiesHandler.getInstance().containsUncertainties()) {
                    if (!sep)
                        addSeparator();

                    MenuItem hideUncertainties = new MenuItem(I18n.txt("context.hideuncertainties"), skin, "default");
                    hideUncertainties.addListener(event -> {
                        if (event instanceof ChangeEvent) {
                            EventManager.instance.post(Events.HIDE_UNCERTAINTIES, candidate);
                            return true;
                        }
                        return false;
                    });
                    addItem(hideUncertainties);

                }
            }

            addSeparator();

            // Cosmic ruler
            CosmicRuler cr = (CosmicRuler) sg.getNode("Cosmicruler");
            if (cr == null) {
                sg.insert((cr = new CosmicRuler()), true);
            }
            MenuItem rulerAttach0 = null, rulerAttach1 = null;
            if (!cr.hasObject0() && !cr.hasObject1()) {
                // No objects attached
                rulerAttach0 = new MenuItem(I18n.txt("context.ruler.attach", "0",  cname), skin, "default");
                rulerAttach0.addListener((ev) -> {
                    if (ev instanceof ChangeEvent) {
                        EventManager.instance.post(Events.RULER_ATTACH_0, cname);
                        return true;
                    }
                    return false;
                });
            } else if (cr.hasObject0() && !cr.hasObject1()) {
                // Only 0 is attached
                rulerAttach1 = new MenuItem(I18n.txt("context.ruler.attach", "1",  cname), skin, "default");
                rulerAttach1.addListener((ev) -> {
                    if (ev instanceof ChangeEvent) {
                        EventManager.instance.post(Events.RULER_ATTACH_1, cname);
                        return true;
                    }
                    return false;
                });
            } else {
                // All attached, show both
                rulerAttach0 = new MenuItem(I18n.txt("context.ruler.attach", "0",  cname), skin, "default");
                rulerAttach0.addListener((ev) -> {
                    if (ev instanceof ChangeEvent) {
                        Gdx.app.postRunnable(() -> {
                            EventManager.instance.post(Events.RULER_ATTACH_0, cname);
                        });
                        return true;
                    }
                    return false;
                });
                rulerAttach1 = new MenuItem(I18n.txt("context.ruler.attach", "1",  cname), skin, "default");
                rulerAttach1.addListener((ev) -> {
                    if (ev instanceof ChangeEvent) {
                        Gdx.app.postRunnable(() -> {
                            EventManager.instance.post(Events.RULER_ATTACH_1, cname);
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

        // Clear ruler
        CosmicRuler cr = (CosmicRuler) sg.getNode("Cosmicruler");
        if (cr == null) {
            sg.insert((cr = new CosmicRuler()), true);
        }
        if (cr.rulerOk() || cr.hasAttached()) {
            MenuItem clearRuler = new MenuItem("Clear ruler", skin, "default");
            clearRuler.addListener((evt) -> {
                if (evt instanceof ChangeEvent) {
                    Gdx.app.postRunnable(() -> {
                        EventManager.instance.post(Events.RULER_CLEAR);
                    });
                    return true;
                }
                return false;
            });
            addItem(clearRuler);
        }

        if (releffects) {
            // Spawn gravitational waves
            MenuItem gravWaveStart = new MenuItem(I18n.txt("context.startgravwave"), skin, "default");
            gravWaveStart.addListener(event -> {
                if (event instanceof ChangeEvent) {
                    EventManager.instance.post(Events.GRAV_WAVE_START, screenX, screenY);
                    return true;
                }
                return false;
            });
            addItem(gravWaveStart);

            if (RelativisticEffectsManager.getInstance().gravWavesOn()) {
                // Cancel gravitational waves
                MenuItem gravWaveStop = new MenuItem(I18n.txt("context.stopgravwave"), skin, "default");
                gravWaveStop.addListener(event -> {
                    if (event instanceof ChangeEvent) {
                        EventManager.instance.post(Events.GRAV_WAVE_STOP);
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
        MenuItem quit = new MenuItem(I18n.txt("context.quit"), skin, "default");
        quit.addListener((event) -> {
            if (event instanceof ChangeEvent) {
                EventManager.instance.post(Events.QUIT_ACTION);
                return true;
            }
            return false;
        });
        addItem(quit);
    }
}
