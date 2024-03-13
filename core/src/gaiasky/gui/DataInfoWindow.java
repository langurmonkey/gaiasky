/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Net.HttpMethods;
import com.badlogic.gdx.Net.HttpResponseListener;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.net.HttpStatus;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import gaiasky.GaiaSky;
import gaiasky.render.ComponentTypes;
import gaiasky.scene.Mapper;
import gaiasky.scene.api.IFocus;
import gaiasky.scene.view.FocusView;
import gaiasky.util.*;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector2d;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.util.parse.Parser;
import gaiasky.util.scene2d.Link;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnScrollPane;
import gaiasky.util.scene2d.OwnTextTooltip;
import gaiasky.util.ucd.UCD;
import org.apache.commons.io.FilenameUtils;
import uk.ac.starlink.table.ColumnInfo;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicInteger;

public class DataInfoWindow extends GenericDialog {
    private static final Logger.Log logger = Logger.getLogger(DataInfoWindow.class);
    private static final int TIMEOUT_MS = 5000;
    private final String[] prefixes = {"NGC", "IC"};
    private final String[] suffixes = {"_(planet)", "_(moon)", "_(star)", "_(asteroid)", "_(dwarf_planet)", "_(spacecraft)", "_(star_cluster)", ""};
    private final String[] suffixes_model = {"_(planet)", "_(moon)", "_(asteroid)", "_(dwarf_planet)", "_(spacecraft)", "_(galaxy)", "_Galaxy", "_Dwarf", ""};
    private final String[] suffixes_gal = {"_(dwarf_galaxy)", "_(galaxy)", "_Galaxy", "_Dwarf", "_Cluster", ""};
    private final String[] suffixes_cluster = {"_(planet)", "_(moon)", "_(asteroid)", "_(dwarf_planet)", "_(spacecraft)", ""};
    private final String[] suffixes_star = {"_(star)", ""};

    private Table mainTable, wikiTable, dataTable;
    private OwnScrollPane scroll;
    private final JsonReader reader;
    private Cell<?> linkCell;

    private final float pad;
    private boolean updating = false;

    private final DecimalFormat nf;
    private final Vector3d pos;
    private final Vector3b posb;
    private final float padBottom = 10f;

    final String colNameStyle = "header-blue";
    final String contentStyle = "big";

    public DataInfoWindow(Stage stg, Skin skin) {
        super(I18n.msg("gui.wiki.title", "?"), skin, stg);

        this.reader = new JsonReader();
        this.pad = 8f;
        this.nf = new DecimalFormat("##0.##");
        this.pos = new Vector3d();
        this.posb = new Vector3b();

        setCancelText(I18n.msg("gui.close"));
        setModal(false);

        // Build
        buildSuper();
    }

    public boolean isUpdating() {
        return updating;
    }

    public void update(FocusView object) {
        String name = object.getLocalizedName();
        this.getTitleLabel().setText(I18n.msg("gui.wiki.title", name));

        updateWikiInfo(object);
        updateLocalData(object);

        recalculateWindowSize();

    }

    public void updateWikiInfo(FocusView object) {
        wikiTable.clear();
        linkCell.clearActor();
        fillWikipediaTable(object, new LinkListener() {
            @Override
            public void ok(String link) {
                try {
                    String actualWikiName = link.substring(Constants.URL_WIKIPEDIA.length());
                    wikiTable.clear();
                    linkCell.clearActor();
                    if (!actualWikiName.isEmpty()) {
                        updating = true;
                        fetchWikipediaData(actualWikiName, new WikiDataListener(actualWikiName));
                    }
                } catch (Exception ignored) {
                }
            }

            @Override
            public void ko(String link) {
            }
        });
    }

    public void updateLocalData(FocusView object) {
        dataTable.clear();
        if (object != null && object.isValid()) {
            fetchData(object);
        }
    }

    @Override
    protected void build() {
        /* TABLE and SCROLL */
        mainTable = new Table(skin);
        mainTable.pad(pad);

        // Create wiki and data tables.
        wikiTable = new Table(skin);
        dataTable = new Table(skin);

        // Add to main.
        mainTable.add(wikiTable).expandX().left().row();
        linkCell = mainTable.add().expandX().padBottom(pad34);
        mainTable.row();
        mainTable.add(dataTable).expandX().left();
        mainTable.align(Align.topLeft);

        scroll = new OwnScrollPane(mainTable, skin, "minimalist-nobg");
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        scroll.setOverscroll(false, false);
        scroll.setSmoothScrolling(true);

        content.add(scroll).left().expandX().expandY().row();
        getTitleTable().align(Align.left);

        pack();
    }

    @Override
    protected boolean accept() {
        return true;
    }

    @Override
    protected void cancel() {
    }

    @Override
    public void dispose() {

    }

    private void fillWikipediaTable(IFocus focus, LinkListener listener) {
        try {
            String url = Constants.URL_WIKIPEDIA;
            String wikiName = focus.getName().replace(' ', '_');
            var view = (FocusView) focus;
            if (Mapper.hip.has(view.getEntity())) {
                urlCheck(url, wikiName, suffixes_star, listener);
            } else if (view.isParticle()) {
                urlCheck(url, wikiName, suffixes_gal, listener);
            } else if (Mapper.tagBillboardGalaxy.has(view.getEntity())) {
                urlCheck(url, wikiName, suffixes_gal, listener);
            } else if (Mapper.celestial.has(view.getEntity())) {
                var celestial = Mapper.celestial.get(view.getEntity());
                if (celestial.wikiName != null) {
                    listener.ok(url + celestial.wikiName.replace(' ', '_'));
                } else {
                    urlCheck(url, wikiName, suffixes_model, listener);
                }
            } else if (view.isCluster()) {
                urlCheck(url, wikiName, suffixes_cluster, listener);
            } else if (Mapper.starSet.has(view.getEntity())) {
                urlCheck(url, wikiName, suffixes_star, listener);
            } else if (wikiName.startsWith("NGC") || wikiName.startsWith("ngc")) {
                var third = wikiName.charAt(3);
                if (third != '_') {
                    var numStr = wikiName.substring(3);
                    try {
                        var num = Parser.parseIntException(numStr);
                        wikiName = "NGC_" + num;
                        urlCheck(url, wikiName, suffixes, listener);
                    } catch (Exception e) {
                        urlCheck(url, wikiName, suffixes, listener);
                    }
                } else {
                    urlCheck(url, wikiName, suffixes, listener);
                }
            } else {
                // Check prefixes
                var hit = false;
                for (var prefix : prefixes) {
                    if (wikiName.startsWith(prefix) || wikiName.startsWith(prefix.toLowerCase()) || wikiName.startsWith(prefix.toUpperCase())) {
                        var locNum = prefix.length();
                        var location = wikiName.charAt(locNum);
                        if (location != '_') {
                            var numStr = wikiName.substring(locNum);
                            try {
                                var num = Parser.parseIntException(numStr);
                                wikiName = prefix.toUpperCase() + "_" + num;
                                urlCheck(url, wikiName, suffixes, listener);
                                hit = true;
                                break;
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
                if(!hit) {
                    urlCheck(url, wikiName, suffixes, listener);
                }
            }

        } catch (Exception e) {
            logger.error(e);
            listener.ok(null);
        }
    }

    private void urlCheck(final String base, final String name, final String[] suffixes, LinkListener listener) {
        final AtomicInteger index = new AtomicInteger(0);
        createRequest(base, name, suffixes, index, listener);
    }

    private void createRequest(final String base, final String name, final String[] suffixes, final AtomicInteger index, LinkListener listener) {
        if (index.get() < suffixes.length) {
            final String url = base + name + suffixes[index.get()];
            Net.HttpRequest request = new Net.HttpRequest(HttpMethods.GET);
            request.setUrl(url);
            request.setTimeOut(TIMEOUT_MS);

            Gdx.net.sendHttpRequest(request, new HttpResponseListener() {
                @Override
                public void handleHttpResponse(Net.HttpResponse httpResponse) {
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


    /**
     * Fills up the {@link #dataTable} with the local information of the given object.
     *
     * @param object The object to use.
     */
    private void fetchData(FocusView object) {

        OwnLabel title = new OwnLabel(I18n.msg("gui.focusinfo.data.local"), skin, "header");
        dataTable.add(title).colspan(2).left().padBottom(pad34).row();

        // Name(s).
        addNames(object);

        // Type.
        addType(object);

        // Id.
        addId(object);

        // Coordinates.
        addCoordinates(object);

        // Proper motions.
        addProperMotions(object);

        if (object.isSet()) {
            // Star or particle set.
            var set = object.getSet();
            var bean = set.get(set.focusIndex);
            if (bean.hasExtra()) {
                var columnInfoList = set.columnInfoList;
                var map = bean.getExtra();
                var keys = map.keys();
                Array<UCD> arr = new Array<>();
                for(var ucd : keys) {
                    arr.add(ucd);
                }
                arr.sort();
                for (var ucd : arr) {
                    Object value = map.get(ucd);
                    var columnGroup = new HorizontalGroup();
                    columnGroup.space(7f);
                    var columnLabel = new OwnLabel(TextUtils.capitalise(ucd.colName), skin, colNameStyle);
                    var valueLabel = new OwnLabel(value != null ? value.toString() : I18n.msg("gui.focusinfo.na"), skin, contentStyle);

                    ColumnInfo colInfo = null;
                    if (columnInfoList != null) {
                        for (var ci : columnInfoList) {
                            if (ci.getName().equalsIgnoreCase(ucd.colName)) {
                                colInfo = ci;
                                break;
                            }
                        }
                    }

                    columnGroup.addActor(columnLabel);
                    if (colInfo != null) {
                        if (colInfo.getDescription() != null) {
                            // Desc.
                            var desc = new OwnLabel("[d]", skin, "default-pink");
                            desc.addListener(new OwnTextTooltip(colInfo.getDescription(), skin, 20));
                            columnGroup.addActor(desc);
                        }
                        if (colInfo.getUnitString() != null) {
                            // Units.
                            var units = new OwnLabel("[u]", skin, "default-pink");
                            units.addListener(new OwnTextTooltip(I18n.msg("gui.unit", colInfo.getUnitString()), skin, 20));
                            columnGroup.addActor(units);
                        }
                    }

                    dataTable.add(columnGroup).left().padRight(pad20).padBottom(padBottom);
                    dataTable.add(valueLabel).left().padBottom(padBottom).row();
                }

            }


        } else if (object.isCluster()) {
            // Star cluster.
            addRadius(object);

        } else {
            // Regular object.
            addRadius(object);

        }

    }

    private void addType(FocusView object) {
        OwnLabel objectType = new OwnLabel("", skin, "object-name-large");
        try {
            objectType.setText(TextUtils.capitalise(
                    I18n.msg("element." +
                            ComponentTypes.ComponentType.values()[object.getCt().getFirstOrdinal()].toString().toLowerCase() + ".singular")));
        } catch (Exception e) {
            objectType.setText("");
        }
        dataTable.add(getLabel(I18n.msg("gui.focusinfo.type"))).left().padRight(pad20).padBottom(padBottom);
        dataTable.add(objectType).left().padBottom(padBottom).row();
    }

    private void addId(FocusView object) {
        boolean cappedId = false;
        String id = null;
        if (object.getExtra() != null || object.getStarSet() != null) {
            if (object.getId() > 0) {
                id = String.valueOf(object.getId());
            } else if (object.getHip() > 0) {
                id = "HIP " + object.getHip();
            } else {
                id = String.valueOf(object.getCandidateId());
            }
        } else {
            id = I18n.msg("gui.focusinfo.na");
        }
        String idString = id;
        if (id.length() > 35) {
            idString = TextUtils.capString(id, 35);
            cappedId = true;
        }
        OwnLabel focusId = new OwnLabel(idString, skin, contentStyle);
        OwnLabel focusIdExpand = new OwnLabel("(?)", skin, "question");
        if (cappedId) {
            focusId.addListener(new OwnTextTooltip(id, skin));
            focusIdExpand.addListener(new OwnTextTooltip(id, skin));
            focusIdExpand.setVisible(true);
        } else {
            focusIdExpand.clearListeners();
            focusIdExpand.setVisible(false);
        }
        dataTable.add(getLabel(I18n.msg("gui.focusinfo.id"))).left().padRight(pad20).padBottom(padBottom);
        dataTable.add(hg(focusId, focusIdExpand)).left().padBottom(padBottom).row();
    }

    private void addNames(FocusView object) {
        Table focusNames = new Table(skin);
        String localizedName = object.getLocalizedName();
        String[] names = object.getNames();
        if (!TextUtils.contains(names, localizedName, true)) {
            TextUtils.addToBeginningOfArray(names, localizedName);
        }
        if (names != null && names.length > 0) {
            int chars = 0;
            HorizontalGroup currGroup = new HorizontalGroup();
            currGroup.space(2f);
            for (int i = 0; i < names.length; i++) {
                String name = names[i];
                String nameCapped = TextUtils.capString(name, 35);
                OwnLabel nl = new OwnLabel(nameCapped, skin, "object-name-large");
                if (nameCapped.length() != name.length())
                    nl.addListener(new OwnTextTooltip(name, skin));
                currGroup.addActor(nl);
                chars += nameCapped.length() + 1;
                if (i < names.length - 1) {
                    currGroup.addActor(new OwnLabel(", ", skin));
                    chars++;
                }
                if (i < names.length - 1 && chars > 35) {
                    focusNames.add(currGroup).left().row();
                    currGroup = new HorizontalGroup();
                    chars = 0;
                }
            }
            if (chars > 0)
                focusNames.add(currGroup).left();
        } else {
            focusNames.add(new OwnLabel(I18n.msg("gui.focusinfo.na"), skin));
        }
        dataTable.add(getLabel(I18n.msg("gui.focusinfo.names"))).left().padRight(pad20).padBottom(padBottom);
        dataTable.add(focusNames).left().padBottom(padBottom).row();
    }

    private void addCoordinates(FocusView object) {
        final String deg = I18n.msg("gui.unit.deg");
        var focusRA = new OwnLabel("", skin, contentStyle);
        var focusDEC = new OwnLabel("", skin, contentStyle);
        Vector2d posSph = object.getPosSph();
        if (posSph != null && posSph.len() > 0f) {
            focusRA.setText(nf.format(posSph.x) + deg);
            focusDEC.setText(nf.format(posSph.y) + deg);
        } else {
            Coordinates.cartesianToSpherical(object.getAbsolutePosition(posb), pos);

            focusRA.setText(nf.format(MathUtilsDouble.radDeg * pos.x % 360) + deg);
            focusDEC.setText(nf.format(MathUtilsDouble.radDeg * pos.y % 360) + deg);
        }
        dataTable.add(getLabel(I18n.msg("gui.focusinfo.alpha"))).padBottom(padBottom).left();
        dataTable.add(focusRA).left().padBottom(padBottom).row();
        dataTable.add(getLabel(I18n.msg("gui.focusinfo.delta"))).padBottom(padBottom).left();
        dataTable.add(focusDEC).left().padBottom(padBottom).row();
    }

    private void addProperMotions(FocusView object) {
        var focusMuAlpha = new OwnLabel(nf.format(object.getMuAlpha()) + " " + I18n.msg("gui.unit.masyr"), skin, contentStyle);
        var focusMuDelta = new OwnLabel(nf.format(object.getMuDelta()) + " " + I18n.msg("gui.unit.masyr"), skin, contentStyle);
        var focusRadVel = new OwnLabel("", skin, contentStyle);
        double rv = object.getRadialVelocity();
        if (Double.isFinite(rv)) {
            focusRadVel.setText(nf.format(object.getRadialVelocity()) + " " + I18n.msg("gui.unit.kms"));
        } else {
            focusRadVel.setText(I18n.msg("gui.focusinfo.na"));
        }
        dataTable.add(getLabel(I18n.msg("gui.focusinfo.mualpha"))).left().padBottom(padBottom);
        dataTable.add(focusMuAlpha).left().padBottom(padBottom).row();
        dataTable.add(getLabel(I18n.msg("gui.focusinfo.mudelta"))).padBottom(padBottom).left();
        dataTable.add(focusMuDelta).left().padBottom(padBottom).row();
        dataTable.add(getLabel(I18n.msg("gui.focusinfo.radvel"))).padBottom(padBottom).left();
        dataTable.add(focusRadVel).left().padBottom(padBottom).row();
    }

    private void addRadius(FocusView object) {
        // Radius.
        var focusRadius = new OwnLabel("", skin, contentStyle);
        focusRadius.setText(GlobalResources.formatNumber(object.getRadius() * Constants.U_TO_KM) + " " + I18n.msg("gui.unit.km"));
        dataTable.add(getLabel(I18n.msg("gui.focusinfo.radius"))).left().padRight(pad20).padBottom(padBottom);
        dataTable.add(focusRadius).left().padBottom(padBottom).row();
    }

    private OwnLabel getLabel(String text) {
        var label = new OwnLabel(text, skin, colNameStyle);
        label.setWidth(Math.max(label.getWidth(), 350f));
        return label;
    }

    /**
     * Launches a request to the Wikipedia API to get the information on the object with the given name.
     *
     * @param wikiName The Wikipedia name of the object.
     * @param listener The listener, for callbacks.
     */
    private void fetchWikipediaData(final String wikiName, final WikiDataListener listener) {
        getJSONData(Constants.URL_WIKI_API_SUMMARY + wikiName, listener);
    }

    /**
     * Creates an HTTP request and updates the Wikipedia info table accordingly with the result.
     *
     * @param url      The URL to use.
     * @param listener The callback listener.
     */
    private void getJSONData(String url, final WikiDataListener listener) {
        Net.HttpRequest request = new Net.HttpRequest(HttpMethods.GET);
        request.setUrl(url);
        request.setTimeOut(5000);

        if (Settings.settings.program.offlineMode) {
            listener.ko(I18n.msg("gui.system.offlinemode.tooltip"));
        } else {
            Gdx.net.sendHttpRequest(request, new HttpResponseListener() {
                @Override
                public void handleHttpResponse(Net.HttpResponse httpResponse) {
                    if (httpResponse.getStatus().getStatusCode() == HttpStatus.SC_OK) {
                        // Ok
                        JsonValue root = reader.parse(httpResponse.getResultAsString());
                        listener.ok(root);
                    } else {
                        // Ko with code
                        listener.ko(httpResponse.getStatus().toString());
                    }

                }

                @Override
                public void failed(Throwable t) {
                    // Failed
                    listener.ko();
                }

                @Override
                public void cancelled() {
                    // Cancelled
                    listener.ko();
                }
            });
        }
    }

    private class WikiDataListener {

        private final String wikiName;
        private Cell<?> imgCell;
        //private Cell moreInfoCell;
        private Texture currentImageTexture;

        public WikiDataListener(String wikiName) {
            this.wikiName = wikiName;
        }

        private void buildImage(Path imageFile) {
            GaiaSky.postRunnable(() -> {
                // Load image into texture
                try {
                    if (currentImageTexture != null) {
                        currentImageTexture.dispose();
                    }
                    currentImageTexture = new Texture(imageFile.toString());
                    Image thumbnailImage = new Image(currentImageTexture);
                    if (imgCell != null) {
                        imgCell.setActor(thumbnailImage);
                        recalculateWindowSize();
                    }
                } catch (Exception e) {
                    logger.error(e);
                }
            });
        }

        public void ok(JsonValue root) {
            if (!root.has("displaytitle")) {
                ko(I18n.msg("gui.wiki.attributemissing", "displaytitle"));
                return;
            }
            String title = TextUtils.html2text(root.getString("displaytitle"));
            getTitleLabel().setText(I18n.msg("gui.wiki.title", title));

            // Thumbnail
            if (root.has("thumbnail")) {
                JsonValue thumb = root.get("thumbnail");
                if (thumb.has("source")) {
                    // Get image
                    String thumbUrl = thumb.getString("source");
                    Path imageFile;
                    try {
                        URL turl = (new URI(thumbUrl)).toURL();
                        String filename = FilenameUtils.getName(turl.getPath());
                        Path cacheDir = SysUtils.getCacheDir();

                        imageFile = cacheDir.resolve(filename);

                        if (!Files.exists(imageFile) || !Files.isRegularFile(imageFile) || !Files.isReadable(imageFile)) {
                            // Download image file!
                            Net.HttpRequest request = new Net.HttpRequest(HttpMethods.GET);
                            request.setUrl(thumbUrl);
                            request.setTimeOut(5000);

                            logger.info(I18n.msg("gui.download.starting", thumbUrl));
                            Gdx.net.sendHttpRequest(request, new HttpResponseListener() {
                                @Override
                                public void handleHttpResponse(Net.HttpResponse httpResponse) {
                                    if (httpResponse.getStatus().getStatusCode() == HttpStatus.SC_OK) {
                                        // Ok
                                        InputStream is = httpResponse.getResultAsStream();
                                        // Write to cache
                                        try (FileOutputStream outputStream = new FileOutputStream(imageFile.toString())) {
                                            int read;
                                            byte[] bytes = new byte[1024];

                                            while ((read = is.read(bytes)) != -1) {
                                                outputStream.write(bytes, 0, read);
                                            }
                                        } catch (IOException e) {
                                            logger.error(e);
                                        }
                                        // Convert to RGB if necessary
                                        try {
                                            if (ImageUtils.monochromeToRGB(imageFile.toFile())) {
                                                logger.info(I18n.msg("gui.wiki.imageconverted", imageFile.toString()));
                                            }
                                            // And send to UI
                                            buildImage(imageFile);
                                        } catch (Exception e) {
                                            logger.error(I18n.msg("error.wiki.rgbconversion", imageFile.toString()));
                                        }
                                    } else {
                                        // Ko with code
                                        logger.error(I18n.msg("error.wiki.thumbnail", thumbUrl));
                                    }
                                }

                                @Override
                                public void failed(Throwable t) {
                                    // Failed
                                    logger.error(I18n.msg("error.wiki.thumbnail", thumbUrl));
                                }

                                @Override
                                public void cancelled() {
                                    // Cancelled
                                    logger.error(I18n.msg("error.wiki.thumbnail", thumbUrl));
                                }
                            });
                        } else {
                            // Image already in local cache
                            buildImage(imageFile);
                        }
                    } catch (MalformedURLException | URISyntaxException e) {
                        logger.error("Error parsing thumbnail URL!");
                        logger.error(e);
                        return;
                    }
                }
            }

            // Title
            OwnLabel titleLabel = new OwnLabel(title, skin, "header-large");
            // Text
            if (!root.has("extract")) {
                ko(I18n.msg("gui.wiki.attributemissing", "extract"));
                return;
            }
            String text = TextUtils.html2text(root.getString("extract"));
            OwnLabel textLabel = new OwnLabel(text, skin, "big", 60);

            // Link
            Link wikiLink = null;
            if (root.has("content_urls")) {
                String link = root.get("content_urls").get("desktop").getString("page");
                wikiLink = new Link(TextUtils.capString(link, 50), skin, link);
            }
            if (linkCell != null && wikiLink != null) {
                linkCell.clearActor();
                linkCell.setActor(wikiLink).padTop(pad * 5f);
            }

            // Populate table
            wikiTable.add(titleLabel).left().colspan(2).padTop(pad * 3f).padBottom(pad * 3f).row();
            imgCell = wikiTable.add().left().padBottom(pad * 5f);
            wikiTable.add(textLabel).left().padBottom(pad * 5f).padLeft(pad * 3f).row();
            wikiTable.pack();
            recalculateWindowSize();
            updating = false;
        }

        public void ko() {
            // Error getting data
            GaiaSky.postRunnable(() -> {
                String msg = I18n.msg("error.wiki.data", wikiName);
                wikiTable.add(new OwnLabel(msg, skin, "big"));
                wikiTable.pack();
                if (linkCell != null) {
                    linkCell.clearActor();
                }
                recalculateWindowSize();
                updating = false;
            });
        }

        public void ko(String error) {
            // Error
            GaiaSky.postRunnable(() -> {
                wikiTable.add(new OwnLabel(error, skin, "big"));
                wikiTable.pack();
                recalculateWindowSize();
                updating = false;
            });
        }

    }

    private void recalculateWindowSize() {
        mainTable.pack();
        scroll.setWidth(MathUtilsDouble.clamp(mainTable.getWidth() + scroll.getStyle().vScroll.getMinWidth(), 1200f, 3000f));
        scroll.setHeight(Math.min(mainTable.getHeight(), Gdx.graphics.getHeight() * 0.7f) + pad);
        scroll.setPosition(0, 0);
        pack();
    }

}