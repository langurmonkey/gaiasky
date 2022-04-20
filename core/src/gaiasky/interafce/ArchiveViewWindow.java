/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net.HttpMethods;
import com.badlogic.gdx.Net.HttpRequest;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.Net.HttpResponseListener;
import com.badlogic.gdx.net.HttpStatus;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import gaiasky.GaiaSky;
import gaiasky.scenegraph.IStarFocus;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.Link;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnScrollPane;
import gaiasky.util.scene2d.OwnTextTooltip;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * This window shows the Gaia Archive information for a single star
 */
public class ArchiveViewWindow extends GenericDialog {
    private static final Log logger = Logger.getLogger(ArchiveViewWindow.class);

    private static final String separator = "\n";

    private Table table;
    private OwnScrollPane scroll;

    private float pad;

    private IStarFocus st;
    private boolean updating = false;


    public ArchiveViewWindow(Stage stage, Skin skin){
        super(I18n.msg("gui.data.catalog", "Gaia", "?"), skin, stage);

        setAcceptText(I18n.msg("gui.close"));
        setModal(false);

        // Build
        buildSuper();

    }

    public boolean isUpdating(){
        return updating;
    }

    public void update(IStarFocus st) {
        updating = true;
        this.st = st;

        this.getTitleLabel().setText(I18n.msg("gui.data.catalog", "Gaia", st.getName()));

        table.clear();
        requestData(new GaiaDataListener(st));
    }

    @Override
    protected void build(){
        this.pad = 8f;

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

    @Override
    public void dispose() {

    }

    private void requestData(GaiaDataListener listener) {
        if (st.getCatalogSource() > 0) {
            if (st.getId() > 5000000) {
                this.getTitleLabel().setText(I18n.msg("gui.data.catalog", "Gaia", st.getName()));
                // Sourceid
                getDataBySourceId(st.getId(), listener);
                return;
            } else if (st.getHip() > 0) {
                this.getTitleLabel().setText(I18n.msg("gui.data.catalog", "Hipparcos", st.getName()));
                // HIP
                getDataByHipId(st.getHip(), listener);
                return;
            }
        }
        listener.notFound();
    }

    private void getDataBySourceId(long sourceid, GaiaDataListener listener) {
        getTAPData(Constants.URL_GAIA_JSON_SOURCE + sourceid, false, "json", listener);
    }

    private void getDataByHipId(int hip, GaiaDataListener listener) {
        getTAPData(Constants.URL_HIP_JSON_SOURCE + hip, true, "json", listener);
    }

    private void getTAPData(String url, boolean hip, final String format, final GaiaDataListener listener) {
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
        try (Reader in = new InputStreamReader(is, StandardCharsets.UTF_8)) {
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
        private final IStarFocus st;

        public GaiaDataListener(IStarFocus st) {
            this.st = st;
        }

        public void ok(final String[][] data, boolean hip) {
            GaiaSky.postRunnable(() -> {

                HorizontalGroup links = new HorizontalGroup();
                links.align(Align.center);
                links.pad(pad / 2f, pad / 2f, pad / 2f, pad / 2f);
                links.space(pad);

                if (hip)
                    links.addActor(new Link(I18n.msg("gui.data.json"), skin, Constants.URL_HIP_JSON_SOURCE + st.getHip()));
                else {
                    links.addActor(new Link(I18n.msg("gui.data.json"), skin, Constants.URL_GAIA_JSON_SOURCE + st.getId()));
                    links.addActor(new OwnLabel("|", skin));
                    links.addActor(new Link(I18n.msg("gui.data.archive"), skin, Constants.URL_GAIA_WEB_SOURCE + st.getId()));
                }

                table.add(links).colspan(2).padTop(pad * 2).padBottom(pad * 2);
                table.row();

                table.add(new OwnLabel(I18n.msg("gui.data.name"), skin, "msg-21")).padLeft(pad * 2).left();
                table.add(new OwnLabel(st.getName(), skin, "msg-21")).padLeft(pad * 2).padRight(pad * 2).left();
                table.row().padTop(pad * 2);
                for (int col = 0; col < data[0].length; col++) {
                    Actor first = null;

                    if (data.length <= 2) {
                        first = new OwnLabel(data[0][col], skin, "ui-19");
                    } else {
                        HorizontalGroup hg = new HorizontalGroup();
                        hg.space(5);
                        ImageButton tooltip = new ImageButton(skin, "tooltip");
                        tooltip.addListener(new OwnTextTooltip(data[2][col], skin));
                        hg.addActor(tooltip);
                        hg.addActor(new OwnLabel(data[0][col], skin, "ui-19"));

                        first = hg;

                    }

                    table.add(first).padLeft(pad * 2).left();
                    table.add(new OwnLabel(data[1][col], skin, "ui-21")).padLeft(pad * 2f).padRight(pad * 2f).left();
                    left();
                    table.row();
                }
                finish();
            });
        }

        public void ko() {
            // Error getting data
            GaiaSky.postRunnable(() -> {
                String msg = I18n.msg("error.gaiacatalog.data", st.getName());
                table.add(new OwnLabel(msg, skin, "ui-21"));
                finish();
            });
        }

        public void ko(String error) {
            // Error
            GaiaSky.postRunnable(() -> {
                String msg = error;
                table.add(new OwnLabel(msg, skin, "ui-21"));
                finish();
            });
        }

        public void notFound() {
            // Not found
            String msg = I18n.msg("error.gaiacatalog.notfound", st.getName());
            table.add(new OwnLabel(msg, skin, "ui-21"));
            finish();
        }

        private void finish() {
            table.pack();
            scroll.setWidth(table.getWidth() + scroll.getStyle().vScroll.getMinWidth());
            scroll.setHeight(Math.min(table.getHeight(), Gdx.graphics.getHeight() * 0.8f) + pad);
            pack();
            updating = false;
        }

    }

}
