/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net.HttpMethods;
import com.badlogic.gdx.Net.HttpRequest;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.Net.HttpResponseListener;
import com.badlogic.gdx.net.HttpStatus;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.InputEvent.Type;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import gaia.cu9.ari.gaiaorbit.scenegraph.IStarFocus;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.scene2d.*;

import java.io.*;

/**
 * This window shows the Gaia Archive information for a single star
 */
public class ArchiveViewWindow extends GenericDialog {
    private static final Log logger = Logger.getLogger(ArchiveViewWindow.class);

    private static String URL_GAIA_JSON_SOURCE = "http://gaia.ari.uni-heidelberg.de/tap/sync?REQUEST=doQuery&LANG=ADQL&FORMAT=json&QUERY=SELECT+*+FROM+gaiadr2.gaia_source+WHERE+source_id=";

    private static String URL_HIP_JSON_SOURCE = "http://gaia.ari.uni-heidelberg.de/tap/sync?REQUEST=doQuery&LANG=ADQL&FORMAT=json&QUERY=SELECT+*+FROM+extcat.hipparcos+WHERE+hip=";

    private static String URL_GAIA_WEB_SOURCE = "http://gaia.ari.uni-heidelberg.de/singlesource.html#id=";

    private static final String separator = "\n";

    private Table table;
    private OwnScrollPane scroll;

    private LabelStyle linkStyle;
    private float pad;

    private IStarFocus st;

    public ArchiveViewWindow(Stage stage, Skin skin){
        super(I18n.txt("gui.data.catalog", "Gaia"), skin, stage);

        setAcceptText(I18n.txt("gui.close"));

        // Build
        buildSuper();

    }

    @Override
    protected void build(){
        this.linkStyle = skin.get("link", LabelStyle.class);
        this.pad = 5 * GlobalConf.SCALE_FACTOR;

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
        // Nothing
    }

    @Override
    protected void cancel() {
        // Nothing
    }

    public void initialize(IStarFocus st) {
        this.st = st;

        table.clear();
        requestData(new GaiaDataListener(st));
        table.pack();

        pack();
        me.setPosition(Math.round(stage.getWidth() / 2f - me.getWidth() / 2f), Math.round(stage.getHeight() / 2f - me.getHeight() / 2f));

    }

    public void display() {
        if (!stage.getActors().contains(me, true))
            stage.addActor(me);
    }

    private void requestData(GaiaDataListener listener) {
        if (st.getCatalogSource() > 0) {

            if (st.getId() > 5000000) {
                this.getTitleLabel().setText(I18n.bundle.format("gui.data.catalog", "Gaia"));
                // Sourceid
                getDataBySourceId(st.getId(), listener);
                return;
            } else if (st.getHip() > 0) {
                this.getTitleLabel().setText(I18n.bundle.format("gui.data.catalog", "Hipparcos"));
                // HIP
                getDataByHipId(st.getHip(), listener);
                return;
            }
        }
        listener.notFound();
    }

    private void getDataBySourceId(long sourceid, GaiaDataListener listener) {
        getTAPData(URL_GAIA_JSON_SOURCE + Long.toString(sourceid), false, "json", listener);
    }

    private void getDataByHipId(int hip, GaiaDataListener listener) {
        getTAPData(URL_HIP_JSON_SOURCE + Integer.toString(hip), true, "json", listener);
    }

    private String[][] getTAPData(String url, boolean hip, final String format, final GaiaDataListener listener) {
        HttpRequest request = new HttpRequest(HttpMethods.GET);
        request.setUrl(url);
        request.setTimeOut(5000);

        Gdx.net.sendHttpRequest(request, new HttpResponseListener() {
            @Override
            public void handleHttpResponse(HttpResponse httpResponse) {
                if (httpResponse.getStatus().getStatusCode() == HttpStatus.SC_OK) {
                    // Ok
                    listener.ok(isToArray(httpResponse.getResultAsStream(), format), hip);
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

        return null;
    }

    public String trim(String stringToTrim, String stringToRemove) {
        String answer = stringToTrim;

        while (answer.startsWith(stringToRemove)) {
            answer = answer.substring(stringToRemove.length());
        }

        while (answer.endsWith(stringToRemove)) {
            answer = answer.substring(0, answer.length() - stringToRemove.length());
        }

        return answer;
    }

    private String slurp(final InputStream is, final int bufferSize) {
        final char[] buffer = new char[bufferSize];
        final StringBuilder out = new StringBuilder();
        try (Reader in = new InputStreamReader(is, "UTF-8")) {
            for (;;) {
                int rsz = in.read(buffer, 0, buffer.length);
                if (rsz < 0)
                    break;
                out.append(buffer, 0, rsz);
            }
        } catch (UnsupportedEncodingException ex) {
            logger.error(ex);
        } catch (IOException ex) {
            logger.error(ex);
        }
        return out.toString();
    }

    private String[][] isToArray(InputStream is, String format) {
        String data = slurp(is, 2046);

        if (format.equalsIgnoreCase("csv")) {
            /** PARSE CSV **/
            String[] rows = data.split(separator);
            if (rows.length <= 1) {
                return null;
            }

            String[][] matrix = new String[rows.length][];

            int r = 0;
            for (String row : rows) {
                String[] tokens = row.split(",");
                int i = 0;
                for (String token : tokens) {
                    token = token.trim();
                    tokens[i] = trim(token, "\"");
                    i++;
                }

                matrix[r] = tokens;
                r++;
            }

            return matrix;
        } else if (format.equalsIgnoreCase("json")) {
            /** PARSE JSON **/
            JsonReader json = new JsonReader();
            JsonValue root = json.parse(data);

            JsonValue metadata = root.child;
            int size = metadata.size;
            JsonValue column = metadata.child;
            String[] colnames = new String[size];
            String[] descriptions = new String[size];
            String[] values = new String[size];

            int i = 0;
            do {
                colnames[i] = column.getString("name");
                descriptions[i] = column.getString("description") + (column.has("unit") ? " [" + column.getString("unit") + "]" : "");
                i++;
                column = column.next;
            } while (column != null);

            JsonValue datacol = metadata.next.child.child;
            i = 0;
            do {
                values[i] = datacol.asString();
                i++;
                datacol = datacol.next;
            } while (datacol != null);

            String[][] matrix = new String[3][];
            matrix[0] = colnames;
            matrix[1] = values;
            matrix[2] = descriptions;
            return matrix;
        }
        return null;
    }

    private class GaiaDataListener {
        private IStarFocus st;

        public GaiaDataListener(IStarFocus st) {
            this.st = st;
        }

        public void ok(final String[][] data, boolean hip) {
            Gdx.app.postRunnable(() -> {

                HorizontalGroup links = new HorizontalGroup();
                links.align(Align.center);
                links.pad(pad / 2f, pad / 2f, pad / 2f, pad / 2f);
                links.space(pad);

                if (hip)
                    links.addActor(new Link(I18n.txt("gui.data.json"), linkStyle, URL_HIP_JSON_SOURCE + st.getHip()));
                else {
                    links.addActor(new Link(I18n.txt("gui.data.json"), linkStyle, URL_GAIA_JSON_SOURCE + st.getId()));
                    links.addActor(new OwnLabel("|", skin));
                    links.addActor(new Link(I18n.txt("gui.data.archive"), linkStyle, URL_GAIA_WEB_SOURCE + st.getId()));
                }

                table.add(links).colspan(2).padTop(pad * 2).padBottom(pad * 2);
                table.row();

                table.add(new OwnLabel(I18n.txt("gui.data.name"), skin, "msg-17")).padLeft(pad * 2).left();
                table.add(new OwnLabel(st.getName(), skin, "msg-17")).padLeft(pad * 2).padRight(pad * 2).left();
                table.row().padTop(pad * 2);
                for (int col = 0; col < data[0].length; col++) {
                    Actor first = null;

                    if (data.length <= 2) {
                        first = new OwnLabel(data[0][col], skin, "ui-13");
                    } else {
                        HorizontalGroup hg = new HorizontalGroup();
                        hg.space(5);
                        ImageButton tooltip = new ImageButton(skin, "tooltip");
                        tooltip.addListener(new TextTooltip(data[2][col], skin));
                        hg.addActor(tooltip);
                        hg.addActor(new OwnLabel(data[0][col], skin, "ui-13"));

                        first = hg;

                    }

                    table.add(first).padLeft(pad * 2).left();
                    table.add(new OwnLabel(data[1][col], skin, "ui-12")).padLeft(pad * 2).padRight(pad * 2).left();
                    left();
                    table.row();
                }
                scroll.setHeight((float) Math.min(table.getMinHeight(), Gdx.graphics.getHeight() * 0.6) + pad);
                finish();
            });
        }

        public void ko() {
            // Error getting data
            Gdx.app.postRunnable(() -> {
                String msg = I18n.bundle.format("error.gaiacatalog.data", st.getName());
                table.add(new OwnLabel(msg, skin, "ui-15"));
                table.pack();
                scroll.setHeight((float) Math.min(table.getHeight(), Gdx.graphics.getHeight() * 0.6) + pad);
                finish();
            });
        }

        public void ko(String error) {
            // Error
            Gdx.app.postRunnable(() -> {
                String msg = error;
                table.add(new OwnLabel(msg, skin, "ui-15"));
                table.pack();
                scroll.setHeight(table.getHeight() + pad);
                finish();
            });
        }

        public void notFound() {
            // Not found
            String msg = I18n.bundle.format("error.gaiacatalog.notfound", st.getName());
            table.add(new OwnLabel(msg, skin, "ui-15"));
            table.pack();
            scroll.setHeight(table.getHeight() + pad);
            finish();
        }

        private void finish() {
            table.pack();

            scroll.setWidth(table.getWidth() + scroll.getStyle().vScroll.getMinWidth());

            pack();
            me.setPosition(Math.round(stage.getWidth() / 2f - me.getWidth() / 2f), Math.round(stage.getHeight() / 2f - me.getHeight() / 2f));
        }

    }

}
