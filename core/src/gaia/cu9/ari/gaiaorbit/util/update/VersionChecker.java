package gaia.cu9.ari.gaiaorbit.util.update;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net.HttpMethods;
import com.badlogic.gdx.Net.HttpRequest;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.Net.HttpResponseListener;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import gaia.cu9.ari.gaiaorbit.util.Logger;

public class VersionChecker implements Runnable {
    private static final int VERSIONCHECK_TIMEOUT_MS = 5000;
    private String stringUrl;
    private EventListener listener;

    public VersionChecker(String stringUrl) {
        this.stringUrl = stringUrl;
    }

    @Override
    public void run() {

        HttpRequest request = new HttpRequest(HttpMethods.GET);
        request.setUrl(stringUrl);
        request.setTimeOut(VERSIONCHECK_TIMEOUT_MS);

        Gdx.net.sendHttpRequest(request, new HttpResponseListener() {
            public void handleHttpResponse(HttpResponse httpResponse) {
                JsonReader reader = new JsonReader();
                JsonValue result = reader.parse(httpResponse.getResultAsStream());
                // Parse commit url to get date
                int n = result.size;
                for (int i = 0; i < n; i++) {
                    String tag = result.get(i).getString("name");

                    // Check tag is major.minor.rev
                    if (tag.matches("^(\\D{1})?\\d+.\\d+(\\D{1})?(.\\d+)?$")) {
                        String commitUrl = result.get(0).get("commit").getString("url");
                        HttpRequest request = new HttpRequest(HttpMethods.GET);
                        request.setUrl(commitUrl);
                        request.setTimeOut(VERSIONCHECK_TIMEOUT_MS);
                        Gdx.net.sendHttpRequest(request, new HttpResponseListener() {

                            @Override
                            public void handleHttpResponse(HttpResponse httpResponse) {
                                JsonReader reader = new JsonReader();
                                JsonValue result = reader.parse(httpResponse.getResultAsStream());
                                String date = result.getChild("commit").getString("date");
                                //Format 2016-12-07T10:41:35Z
                                DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
                                try {
                                    LocalDateTime tagDate = LocalDateTime.parse(date, df);
                                    // Here is the commit object
                                    listener.handle(new VersionCheckEvent(tag, tagDate.toInstant(ZoneOffset.UTC)));
                                } catch (DateTimeParseException e) {
                                    Logger.getLogger(this.getClass()).error(e);
                                }
                            }

                            @Override
                            public void failed(Throwable t) {
                                listener.handle(new VersionCheckEvent(true));
                            }

                            @Override
                            public void cancelled() {
                                listener.handle(new VersionCheckEvent(true));
                            }

                        });
                        break;
                    }
                }

            }

            public void failed(Throwable t) {
                listener.handle(new VersionCheckEvent(true));
            }

            @Override
            public void cancelled() {
                listener.handle(new VersionCheckEvent(true));
            }
        });

    }

    public void setListener(EventListener listener) {
        this.listener = listener;
    }

}
