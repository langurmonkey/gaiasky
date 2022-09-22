/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net.HttpMethods;
import com.badlogic.gdx.Net.HttpRequest;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.Net.HttpResponseListener;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter.Particle;
import com.badlogic.gdx.net.HttpStatus;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.scene.Mapper;
import gaiasky.scene.view.FocusView;
import gaiasky.scenegraph.*;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.Link;
import gaiasky.util.scene2d.OwnTextButton;
import gaiasky.util.scene2d.OwnTextTooltip;

import java.util.concurrent.atomic.AtomicInteger;

public class ExternalInformationUpdater {
    private static final Log logger = Logger.getLogger(ExternalInformationUpdater.class);

    private static final int TIMEOUT_MS = 5000;

    private Skin skin;
    private LabelStyle linkStyle;

    private Cell<Link> infoCell, gaiaCell, simbadCell;
    private Link simbadLink;
    private OwnTextButton infoButton, gaiaButton;

    // The table to modify
    private Table table;
    private float pad;

    public ExternalInformationUpdater() {
    }

    public void setParameters(Table table, Skin skin, float pad) {
        this.table = table;
        this.skin = skin;
        this.linkStyle = skin.get("link", LabelStyle.class);
        this.pad = pad;
    }

    private class GaiaButtonListener implements EventListener {
        private final IStarFocus focus;

        public GaiaButtonListener(IStarFocus focus) {
            super();
            this.focus = focus;
        }

        @Override
        public boolean handle(com.badlogic.gdx.scenes.scene2d.Event event) {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.SHOW_ARCHIVE_VIEW_ACTION, this, focus);
                return true;
            }
            return false;
        }
    }

    public void update(final IFocus focus) {
        GaiaSky.postRunnable(() -> {
            if (focus != null) {
                logger.debug("Looking up network resources for '" + focus.getName() + "'");

                infoCell = table.add().left();
                gaiaCell = table.add().left();
                simbadCell = table.add().left();

                // Add table
                if (focus instanceof IStarFocus) {
                    EventManager.publish(Event.UPDATE_ARCHIVE_VIEW_ACTION, this, focus);
                    if (gaiaButton != null)
                        gaiaButton.remove();
                    gaiaButton = new OwnTextButton(I18n.msg("gui.focusinfo.archive"), skin);
                    gaiaButton.pad(pad / 3f, pad, pad / 3f, pad);
                    gaiaButton.addListener(new GaiaButtonListener((IStarFocus) focus));
                    gaiaButton.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.gaiaarchive"), skin));
                    gaiaCell.setActor(gaiaButton).padRight(pad);
                } else {
                    gaiaCell.padRight(0);
                }

                String wikiname = focus.getName().replace(' ', '_');

                setWikiLink(wikiname, focus, new LinkListener() {
                    @Override
                    public void ok(String link) {
                        if (infoCell != null) {
                            try {
                                String actualWikiname = link.substring(Constants.URL_WIKIPEDIA.length());
                                EventManager.publish(Event.UPDATE_WIKI_INFO_ACTION, this, actualWikiname);
                                if (infoButton != null)
                                    infoButton.remove();
                                infoButton = new OwnTextButton(I18n.msg("gui.focusinfo.moreinfo"), skin);
                                infoButton.setDisabled(Settings.settings.program.offlineMode);
                                infoButton.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.wiki"), skin));
                                infoButton.pad(pad / 3f, pad, pad / 3f, pad);
                                infoButton.addListener((event) -> {
                                    if (event instanceof ChangeEvent) {
                                        EventManager.publish(Event.SHOW_WIKI_INFO_ACTION, this, actualWikiname);
                                        return true;
                                    }
                                    return false;
                                });
                                infoCell.setActor(infoButton).padRight(pad);
                            } catch (Exception ignored) {
                            }
                        }
                    }

                    @Override
                    public void ko(String link) {
                        if (infoCell != null)
                            infoCell.padRight(0);
                    }
                });
                setSimbadLink(focus, new LinkListener() {
                    @Override
                    public void ok(String link) {
                        if (simbadCell != null) {
                            try {
                                if (simbadLink != null) {
                                    simbadLink.remove();
                                }
                                simbadLink = new Link(I18n.msg("gui.focusinfo.simbad"), linkStyle, "");
                                simbadLink.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.simbad"), skin));
                                simbadLink.setLinkURL(link);
                                simbadCell.setActor(simbadLink);
                            } catch (Exception ignored) {
                            }
                        }
                    }

                    @Override
                    public void ko(String link) {
                    }

                });
            }

        });
    }

    private void setSimbadLink(IFocus focus, LinkListener listener) {
        if (focus instanceof IStarFocus) {
            String url = Constants.URL_SIMBAD;
            IStarFocus st = (IStarFocus) focus;
            if (st.getHip() > 0) {
                listener.ok(url + "HIP+" + st.getHip());
            } else {
                listener.ko(null);
            }
        } else {
            listener.ko(null);
        }
    }

    private final String[] suffixes = { "_(planet)", "_(moon)", "_(star)", "_(asteroid)", "_(dwarf_planet)", "_(spacecraft)", "_(star_cluster)", "" };
    private final String[] suffixes_model = { "_(planet)", "_(moon)", "_(asteroid)", "_(dwarf_planet)", "_(spacecraft)", "_(galaxy)", "_Galaxy", "_Dwarf", "" };
    private final String[] suffixes_gal = { "_(dwarf_galaxy)", "_(galaxy)", "_Galaxy", "_Dwarf", "_Cluster", "" };
    private final String[] suffixes_cluster = { "_(planet)", "_(moon)", "_(asteroid)", "_(dwarf_planet)", "_(spacecraft)", "" };
    private final String[] suffixes_star = { "_(star)", "" };

    private void setWikiLink(String wikiname, IFocus focus, LinkListener listener) {
        try {
            String url = Constants.URL_WIKIPEDIA;
            var view = (FocusView) focus;
            if (Mapper.hip.has(view.getEntity())) {
                urlCheck(url, wikiname, suffixes_star, listener);
            } else if (view.isParticle()) {
                urlCheck(url, wikiname, suffixes_gal, listener);
            } else if (Mapper.tagBillboardGalaxy.has(view.getEntity())) {
                urlCheck(url, wikiname, suffixes_gal, listener);
            } else if (Mapper.celestial.has(view.getEntity())) {
                var celestial = Mapper.celestial.get(view.getEntity());
                if (celestial.wikiname != null) {
                    listener.ok(url + celestial.wikiname.replace(' ', '_'));
                } else {
                    urlCheck(url, wikiname, suffixes_model, listener);
                }
            } else if (view.isCluster()) {
                urlCheck(url, wikiname, suffixes_cluster, listener);
            } else if (Mapper.starSet.has(view.getEntity())) {
                urlCheck(url, wikiname, suffixes_star, listener);
            } else {
                urlCheck(url, wikiname, suffixes, listener);
            }

        } catch (Exception e) {
            logger.error(e);
            listener.ko(null);
        }
    }

    private void urlCheck(final String base, final String name, final String[] suffixes, LinkListener listener) {
        final AtomicInteger index = new AtomicInteger(0);
        createRequest(base, name, suffixes, index, listener);
    }

    private void createRequest(final String base, final String name, final String[] suffixes, final AtomicInteger index, LinkListener listener) {
        if (index.get() < suffixes.length) {
            final String url = base + name + suffixes[index.get()];
            HttpRequest request = new HttpRequest(HttpMethods.GET);
            request.setUrl(url);
            request.setTimeOut(TIMEOUT_MS);

            Gdx.net.sendHttpRequest(request, new HttpResponseListener() {
                @Override
                public void handleHttpResponse(HttpResponse httpResponse) {
                    if (httpResponse.getStatus().getStatusCode() == HttpStatus.SC_OK) {
                        listener.ok(url);
                    } else {
                        // Next
                        index.incrementAndGet();
                        createRequest(base, name, suffixes, index, listener);
                    }
                }

                @Override
                public void failed(Throwable t) {
                    // Next
                    index.incrementAndGet();
                    createRequest(base, name, suffixes, index, listener);
                }

                @Override
                public void cancelled() {
                    // Next
                    index.incrementAndGet();
                    createRequest(base, name, suffixes, index, listener);
                }
            });
        } else {
            // Ran out of suffixes!
            listener.ko(base + name);
        }
    }

    private interface LinkListener {
        void ok(String link);

        void ko(String link);
    }
}
