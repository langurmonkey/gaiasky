package gaia.cu9.ari.gaiaorbit.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net.HttpMethods;
import com.badlogic.gdx.Net.HttpRequest;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.Net.HttpResponseListener;
import com.badlogic.gdx.files.FileHandle;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;

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
                // Determine how much we have to download
                long length = Long.parseLong(httpResponse.getHeader("Content-Length"));

                int status = httpResponse.getStatus().getStatusCode();
                if (status < 400) {
                    // We're going to download the file, create the streams
                    InputStream is = httpResponse.getResultAsStream();
                    OutputStream os = to.write(false);

                    byte[] bytes = new byte[1024];
                    int count = -1;
                    long read = 0;
                    long lastTimeMs = System.currentTimeMillis();
                    long lastRead = 0;
                    double bytesPerMs = 0;
                    try {
                        logger.info("Downloading: " + url);
                        MessageDigest md = MessageDigest.getInstance("MD5");
                        DigestInputStream dis = new DigestInputStream(is, md);
                        // Keep reading bytes and storing them until there are no more.
                        while ((count = dis.read(bytes, 0, bytes.length)) != -1) {
                            os.write(bytes, 0, count);
                            read += count;

                            // Compute progress value
                            final double progressValue = ((double) read / (double) length) * 100;

                            // Compute speed
                            long currentTimeMs = System.currentTimeMillis();
                            // Update each second
                            boolean updateSpeed = currentTimeMs - lastTimeMs >= 1000;
                            if(updateSpeed) {
                                long elapsedMs = currentTimeMs - lastTimeMs;
                                long readInterval = read - lastRead;
                                bytesPerMs = readInterval / elapsedMs;
                            }

                            // Run progress runnable
                            if (progress != null)
                                progress.run(read, length, progressValue, bytesPerMs);

                            // Reset
                            if(updateSpeed) {
                                lastTimeMs = currentTimeMs;
                                lastRead = read;
                            }
                        }
                        is.close();
                        os.close();
                        logger.info(I18n.txt("gui.download.finished", to.path()));

                        // Run finish runnable
                        if (finish != null) {
                            byte[] digest = md.digest();
                            StringBuffer md5 = new StringBuffer();
                            for (int i = 0; i < digest.length; i++) {
                                if ((0xff & digest[i]) < 0x10) {
                                    md5.append("0" + Integer.toHexString((0xFF & digest[i])));
                                } else {
                                    md5.append(Integer.toHexString(0xFF & digest[i]));
                                }
                            }
                            String md5sum = md5.toString();
                            finish.run(md5sum);
                        }
                    } catch (Exception e) {
                        logger.error(e);
                        if (fail != null)
                            fail.run();
                    }
                } else {
                    logger.error("HTTP Error : Status Code : " + status);
                    if(fail != null)
                        fail.run();
                }
            }

            @Override
            public void failed(Throwable t) {
                System.out.println(I18n.txt("gui.download.fail"));
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

}
