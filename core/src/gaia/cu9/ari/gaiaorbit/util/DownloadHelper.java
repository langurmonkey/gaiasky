package gaia.cu9.ari.gaiaorbit.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net.HttpMethods;
import com.badlogic.gdx.Net.HttpRequest;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.Net.HttpResponseListener;
import com.badlogic.gdx.files.FileHandle;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;

/**
 * Contains utilities to download files
 * @author tsagrista
 *
 */
public class DownloadHelper {
    private static final Log logger = Logger.getLogger(DownloadHelper.class);

    /*
     * Spawns a new thread which downloads the file from the given location, running the
     * progress {@link ProgressRunnable} while downloading, and running the finish {@link java.lang.Runnable}
     * when finished.
     */
    public static void downloadFile(String url, FileHandle to, ProgressRunnable progress, ChecksumRunnable finish, Runnable fail, Runnable cancel) {

        // Make a GET request to get data descriptor
        HttpRequest request = new HttpRequest(HttpMethods.GET);
        request.setTimeOut(2500);
        request.setUrl(url);

        // Send the request, listen for the response
        Gdx.net.sendHttpRequest(request, new HttpResponseListener() {
            @Override
            public void handleHttpResponse(HttpResponse httpResponse) {
                finish.run(null);
                return;
            }

            @Override
            public void failed(Throwable t) {
                System.out.println(txt("gui.download.fail"));
                if (fail != null)
                    fail.run();
            }

            @Override
            public void cancelled() {
                logger.error("Download cancelled: " + url);
                if (cancel != null)
                    cancel.run();
            }
        });
    }

    protected static String txt(String key) {
        return I18n.bundle.get(key);
    }

    protected static String txt(String key, Object... args) {
        return I18n.bundle.format(key, args);
    }

}
