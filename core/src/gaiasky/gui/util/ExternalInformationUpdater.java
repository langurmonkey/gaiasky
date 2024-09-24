/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net.HttpMethods;
import com.badlogic.gdx.Net.HttpRequest;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.Net.HttpResponseListener;
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
import gaiasky.gui.api.LinkListener;
import gaiasky.scene.view.FocusView;
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
    private Cell<Link> dataCell, gaiaCell, simbadCell;
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

    public void update(final FocusView focus) {
        GaiaSky.postRunnable(() -> {
            if (focus != null) {
                logger.debug("Looking up network resources for '" + focus.getName() + "'");

                table.row();
                dataCell = table.add().left();
                gaiaCell = table.add().left();
                simbadCell = table.add().left();

                // Gaia archive.
                if (focus.isStar()) {
                    EventManager.publish(Event.UPDATE_ARCHIVE_VIEW_CMD, this, focus);
                    if (gaiaButton != null)
                        gaiaButton.remove();
                    gaiaButton = new OwnTextButton(I18n.msg("gui.focusinfo.archive"), skin);
                    gaiaButton.pad(pad / 3f, pad, pad / 3f, pad);
                    gaiaButton.addListener(new GaiaArchiveButtonListener(focus));
                    gaiaButton.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.gaiaarchive"), skin));
                    gaiaCell.setActor(gaiaButton).padRight(pad);
                } else {
                    gaiaCell.padRight(0);
                }

                // Data button (+ Info).
                EventManager.publish(Event.UPDATE_DATA_INFO_CMD, this, focus);
                if (infoButton != null)
                    infoButton.remove();
                infoButton = new OwnTextButton(I18n.msg("gui.focusinfo.moreinfo"), skin);
                infoButton.setDisabled(Settings.settings.program.offlineMode);
                infoButton.addListener(new OwnTextTooltip(I18n.msg("gui.tooltip.wiki"), skin));
                infoButton.pad(pad / 3f, pad, pad / 3f, pad);
                infoButton.addListener((event) -> {
                    if (event instanceof ChangeEvent) {
                        EventManager.publish(Event.SHOW_DATA_INFO_CMD, this, focus);
                        return true;
                    }
                    return false;
                });
                dataCell.setActor(infoButton).padRight(pad);

                // Simbad link.
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

    private void setSimbadLink(FocusView focus, LinkListener listener) {
        if (focus.isStar()) {
            String url = Constants.URL_SIMBAD;
            if (focus.getHip() > 0) {
                listener.ok(url + "HIP+" + focus.getHip());
            } else {
                listener.ko(null);
            }
        } else {
            listener.ko(null);
        }
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
            listener.ok(null);
        }
    }


    private static class GaiaArchiveButtonListener implements EventListener {
        private final FocusView focus;

        public GaiaArchiveButtonListener(FocusView focus) {
            super();
            this.focus = focus;
        }

        @Override
        public boolean handle(com.badlogic.gdx.scenes.scene2d.Event event) {
            if (event instanceof ChangeEvent) {
                EventManager.publish(Event.SHOW_ARCHIVE_VIEW_CMD, this, focus);
                return true;
            }
            return false;
        }
    }
}
