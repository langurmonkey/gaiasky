/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Net.HttpMethods;
import com.badlogic.gdx.Net.HttpResponseListener;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.net.HttpStatus;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import gaiasky.GaiaSky;
import gaiasky.desktop.util.SysUtils;
import gaiasky.util.*;
import gaiasky.util.scene2d.Link;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnScrollPane;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class WikiInfoWindow extends GenericDialog {
    private static final Logger.Log logger = Logger.getLogger(WikiInfoWindow.class);

    private Table table;
    private OwnScrollPane scroll;
    private JsonReader reader;

    private float pad;
    private boolean updating = false;

    public WikiInfoWindow(Stage stg, Skin skin) {
        super(I18n.txt("gui.help.meminfo"), skin, stg);

        this.reader = new JsonReader();
        this.pad = 8f;

        setCancelText(I18n.txt("gui.close"));
        setModal(false);

        // Build
        buildSuper();
    }

    public boolean isUpdating() {
        return updating;
    }

    public void update(String searchName) {
        updating = true;
        this.getTitleLabel().setText("Object information: " + searchName);

        table.clear();
        getDataByWikiName(searchName, new WikiDataListener(searchName));
    }

    @Override
    protected void build() {
        /** TABLE and SCROLL **/
        table = new Table(skin);
        table.pad(pad);
        scroll = new OwnScrollPane(table, skin, "minimalist-nobg");
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        scroll.setOverscroll(false, false);
        scroll.setSmoothScrolling(true);

        content.add(scroll);
        getTitleTable().align(Align.left);

        pack();
    }

    @Override
    protected void accept() {
    }

    @Override
    protected void cancel() {
    }

    private void getDataByWikiName(final String wikiname, final WikiDataListener listener) {
        getJSONData(Constants.URL_WIKI_API_SUMMARY + wikiname, listener);
    }

    private void getJSONData(String url, final WikiDataListener listener) {
        Net.HttpRequest request = new Net.HttpRequest(HttpMethods.GET);
        request.setUrl(url);
        request.setTimeOut(5000);

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

    private class WikiDataListener {

        private String wikiname;
        private Cell imgCell;
        private Texture currentImageTexture;

        public WikiDataListener(String wikiname) {
            this.wikiname = wikiname;
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
                        finish();
                    }
                } catch (Exception e) {
                    logger.error(e);
                }
            });
        }

        public void ok(JsonValue root) {
            if (!root.has("displaytitle")) {
                ko("'displaytitle' attribute missing");
                return;
            }
            String title = TextUtils.html2text(root.getString("displaytitle"));
            getTitleLabel().setText("Object information: " + title);

            // Thumbnail
            if (root.has("thumbnail")) {
                JsonValue thumb = root.get("thumbnail");
                if (thumb.has("source")) {
                    // Get image
                    String thumbUrl = thumb.getString("source");
                    String filename = Path.of(thumbUrl).getFileName().toString();
                    Path cacheDir = SysUtils.getCacheDir();

                    Path imageFile = cacheDir.resolve(filename);

                    if (!Files.exists(imageFile) || !Files.isRegularFile(imageFile) || !Files.isReadable(imageFile)) {
                        // Download image file!
                        Net.HttpRequest request = new Net.HttpRequest(HttpMethods.GET);
                        request.setUrl(thumbUrl);
                        request.setTimeOut(5000);

                        logger.info(I18n.txt("gui.download.starting", thumbUrl));
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
                                    } catch (FileNotFoundException e) {
                                        logger.error(e);
                                    } catch (IOException e) {
                                        logger.error(e);
                                    }
                                    // Convert to RGB if necessary
                                    try {
                                        if (ImageUtils.monochromeToRGB(imageFile.toFile())) {
                                            logger.info("Image converted to RGB: " + imageFile.toString());
                                        }
                                        // And send to UI
                                        buildImage(imageFile);
                                    } catch (Exception e) {
                                        logger.error("Error converting monochrome image to RGB: " + imageFile.toString());
                                    }
                                } else {
                                    // Ko with code
                                    logger.error("Error getting thumbnail image from " + thumbUrl);
                                }
                            }

                            @Override
                            public void failed(Throwable t) {
                                // Failed
                                logger.error("Error getting thumbnail image from " + thumbUrl);
                            }

                            @Override
                            public void cancelled() {
                                // Cancelled
                                logger.error("Error getting thumbnail image from " + thumbUrl);
                            }
                        });
                    } else {
                        // Image already in local cache
                        buildImage(imageFile);
                    }
                }
            }

            // Title
            OwnLabel titleLabel = new OwnLabel(title, skin, "header-large");
            // Text
            if (!root.has("extract")) {
                ko("'extract' attribute missing");
                return;
            }
            String text = TextUtils.html2text(root.getString("extract"));
            OwnLabel textLabel = new OwnLabel(text, skin, "ui-23", 60);
            // Link
            Link wikiLink = null;
            if (root.has("content_urls")) {
                String link = root.get("content_urls").get("desktop").getString("page");
                wikiLink = new Link("More information...", skin, link);
            }

            // Populate table
            table.add(titleLabel).left().colspan(2).padTop(pad * 3f).padBottom(pad * 3f).row();
            imgCell = table.add().left().padBottom(pad * 5f);
            table.add(textLabel).left().padBottom(pad * 5f).padLeft(pad * 3f).row();
            if (wikiLink != null)
                table.add(wikiLink).center().colspan(2);
            table.pack();
            finish();
        }

        public void ko() {
            // Error getting data
            GaiaSky.postRunnable(() -> {
                String msg = I18n.bundle.format("error.gaiacatalog.data", wikiname);
                table.add(new OwnLabel(msg, skin, "ui-21"));
                table.pack();
                finish();
            });
        }

        public void ko(String error) {
            // Error
            GaiaSky.postRunnable(() -> {
                String msg = error;
                table.add(new OwnLabel(msg, skin, "ui-21"));
                table.pack();
                finish();
            });
        }

        private void finish() {
            table.pack();
            scroll.setWidth(table.getWidth() + scroll.getStyle().vScroll.getMinWidth());
            scroll.setHeight(Math.min(table.getHeight(), Gdx.graphics.getHeight()) + pad);
            pack();
            updating = false;
        }

    }
}
