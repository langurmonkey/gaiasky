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
import gaiasky.util.Constants;
import gaiasky.util.I18n;
import gaiasky.util.Logger;
import gaiasky.util.scene2d.Link;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnScrollPane;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public class WikiInfoWindow extends GenericDialog {
    private static final Logger.Log logger = Logger.getLogger(WikiInfoWindow.class);

    private Table table;
    private OwnScrollPane scroll;
    private JsonReader reader;

    private float pad;

    public WikiInfoWindow(Stage stg, Skin skin) {
        super(I18n.txt("gui.help.meminfo"), skin, stg);

        this.reader = new JsonReader();
        this.pad = 8f;

        setCancelText(I18n.txt("gui.close"));
        setModal(false);

        // Build
        buildSuper();
    }

    public void update(String searchName) {
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

        public WikiDataListener(String wikiname) {
            this.wikiname = wikiname;
        }

        private boolean checkAttrib(JsonValue json, String attrib) {
            if (!json.has(attrib)) {
                ko("Could not parse wikipedia API JSON response object");
                return false;
            }
            return true;
        }

        public void ok(JsonValue root) {
            if (!checkAttrib(root, "displaytitle"))
                return;
            String title = root.getString("displaytitle");
            getTitleLabel().setText("Object information: " + title);

            // Thumbnail
            if (checkAttrib(root, "thumbnail")) {
                JsonValue thumb = root.get("thumbnail");
                if (checkAttrib(thumb, "source")) {
                    // Get image
                    String thumbUrl = thumb.getString("source");

                    Net.HttpRequest request = new Net.HttpRequest(HttpMethods.GET);
                    request.setUrl(thumbUrl);
                    request.setTimeOut(5000);

                    Gdx.net.sendHttpRequest(request, new HttpResponseListener() {
                        @Override
                        public void handleHttpResponse(Net.HttpResponse httpResponse) {
                            if (httpResponse.getStatus().getStatusCode() == HttpStatus.SC_OK) {
                                // Ok, have image
                                logger.info("Got image: " + thumbUrl);
                                Path cacheDir = SysUtils.getCacheDir();
                                InputStream is = httpResponse.getResultAsStream();

                                Path fileName = Path.of(thumbUrl).getFileName();
                                Path imageFile = cacheDir.resolve(fileName);
                                try (FileOutputStream outputStream = new FileOutputStream(imageFile.toString())) {
                                    int read;
                                    byte[] bytes = new byte[1024];

                                    while ((read = is.read(bytes)) != -1) {
                                        outputStream.write(bytes, 0, read);
                                    }

                                    GaiaSky.postRunnable(()-> {
                                        // Load image
                                        Texture tex = new Texture(imageFile.toString());
                                        Image thumbnailImage = new Image(tex);
                                        if(imgCell != null){
                                            imgCell.setActor(thumbnailImage);
                                            finish();
                                            scroll.setHeight(Math.min(table.getMinHeight(), Gdx.graphics.getHeight()) * 1.2f + pad);
                                        }
                                    });
                                } catch (FileNotFoundException e) {
                                    logger.error(e);
                                } catch (IOException e) {
                                    logger.error(e);
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
                }
            }

            // Title
            OwnLabel titleLabel = new OwnLabel(title, skin, "header-large");
            // Text
            if (!checkAttrib(root, "extract"))
                return;
            String text = root.getString("extract");
            OwnLabel textLabel = new OwnLabel(text, skin, "ui-21", 70);
            // Link
            if (!checkAttrib(root, "content_urls"))
                return;
            String link = root.get("content_urls").get("desktop").getString("page");
            Link wikiLink = new Link("More information...", skin, link);

            // Populate table
            table.add(titleLabel).left().colspan(2).padTop(pad * 3f).padBottom(pad * 3f).row();
            imgCell = table.add().left().padBottom(pad * 5f);
            table.add(textLabel).left().padBottom(pad * 5f).padLeft(pad).row();
            table.add(wikiLink).center().colspan(2);
            table.pack();

            scroll.setHeight(Math.min(table.getMinHeight(), Gdx.graphics.getHeight()) * 1.2f + pad);
            finish();
        }

        public void ko() {
            // Error getting data
            GaiaSky.postRunnable(() -> {
                String msg = I18n.bundle.format("error.gaiacatalog.data", wikiname);
                table.add(new OwnLabel(msg, skin, "ui-21"));
                table.pack();
                scroll.setHeight(Math.min(table.getHeight(), Gdx.graphics.getHeight() * 0.6f) + pad);
                finish();
            });
        }

        public void ko(String error) {
            // Error
            GaiaSky.postRunnable(() -> {
                String msg = error;
                table.add(new OwnLabel(msg, skin, "ui-21"));
                table.pack();
                scroll.setHeight(table.getHeight() + pad);
                finish();
            });
        }

        private void finish() {
            table.pack();
            scroll.setWidth(table.getWidth() + scroll.getStyle().vScroll.getMinWidth());
            pack();
        }

    }
}
