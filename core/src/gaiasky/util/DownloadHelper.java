/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net.HttpMethods;
import com.badlogic.gdx.Net.HttpRequest;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.Net.HttpResponseListener;
import com.badlogic.gdx.files.FileHandle;
import gaiasky.util.Logger.Log;
import gaiasky.util.i18n.I18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;

public class DownloadHelper {
    private static final Log logger = Logger.getLogger(DownloadHelper.class);

    /*
     * Spawns a new thread which downloads the file from the given location, running the
     * progress {@link ProgressRunnable} while downloading, and running the finish {@link java.lang.Runnable}
     * when finished.
     */
    public static HttpRequest downloadFile(String url, FileHandle to, boolean offlineMode, ProgressRunnable progressDownload, ProgressRunnable progressHashResume, ChecksumRunnable finish, Runnable fail, Runnable cancel) {

        if (url.startsWith("file://")) {
            // Local file!
            String srcString = url.replaceFirst("file://", "");
            Path source = Paths.get(srcString);
            logger.info("Using file:// protocol: " + srcString);
            if (Files.exists(source) && Files.isRegularFile(source) && Files.isReadable(source)) {
                Path target = Path.of(to.path());
                try {
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    // Run finish with empty digest
                    finish.run("");
                } catch (IOException e) {
                    logger.error(I18n.msg("error.file.copy", srcString, to.path()));
                }

            } else {
                logger.error(I18n.msg("error.loading.notexistent", srcString));
                if (fail != null)
                    fail.run();
            }
            return null;
        } else {
            // Make a GET request to get data descriptor
            HttpRequest request = new HttpRequest(HttpMethods.GET);
            request.setFollowRedirects(true);
            request.setTimeOut(2500);
            request.setUrl(url);

            // Resume download if target file (?.tar.gz.part) exists
            final boolean resume = to.exists() && to.name().endsWith(".part");
            final long startSize;

            if (resume) {
                startSize = to.file().length();
                request.setHeader("Range", "bytes=" + startSize + "-");
                logger.info("Resuming download, part file found: " + to.file().toPath());
            } else {
                startSize = 0;
            }

            if (offlineMode) {
                if (fail != null) {
                    fail.run();
                }
            } else {
                sendRequest(request, url, resume, to, startSize, progressDownload, progressHashResume, finish, fail, cancel);
            }

            return request;
        }
    }

    public static void sendRequest(HttpRequest request, String url, boolean resume, FileHandle to, long startSize, ProgressRunnable progressDownload, ProgressRunnable progressHashResume, ChecksumRunnable finish, Runnable fail, Runnable cancel) {
        // Send the request, listen for the response
        Gdx.net.sendHttpRequest(request, new HttpResponseListener() {
            private boolean cancelled = false;

            @Override
            public void handleHttpResponse(HttpResponse httpResponse) {
                // Determine how much we have to download
                int status = httpResponse.getStatus().getStatusCode();
                if (status == 416) {
                    // Range not satisfiable, clean and try again
                    if (resume) {
                        try {
                            Files.delete(to.file().toPath());
                        } catch (IOException e) {
                            logger.error(e);
                        }
                    }
                    sendRequest(request, url, false, to, 0, progressDownload, progressHashResume, finish, fail, cancel);

                    return;
                }
                long length = Long.parseLong(httpResponse.getHeader("Content-Length"));

                if (status < 400) {
                    // We're going to download the file, create the streams
                    try {
                        InputStream is = httpResponse.getResultAsStream();
                        OutputStream os = to.write(resume);

                        byte[] bytes = new byte[1024];
                        int count;
                        long read = 0;
                        long lastTimeMs = System.currentTimeMillis();
                        long lastRead = 0;
                        double bytesPerMs = 0;
                        long totalLength = length + startSize;
                        logger.info(I18n.msg("gui.download.starting", url));
                        logger.info("Reading " + length + " bytes " + (resume ? "(resuming from " + startSize + ", total is " + totalLength + ")" : ""));
                        MessageDigest md = MessageDigest.getInstance("SHA-256");
                        DigestInputStream dis = new DigestInputStream(is, md);
                        // Keep reading bytes and storing them until there are no more.
                        while ((count = dis.read(bytes, 0, bytes.length)) != -1 && !cancelled) {
                            os.write(bytes, 0, count);
                            read += count;

                            // Compute progress value
                            final double progressValue = ((double) (startSize + read) / (double) totalLength) * 100;

                            // Compute speed
                            long currentTimeMs = System.currentTimeMillis();
                            // Update each second
                            boolean updateSpeed = currentTimeMs - lastTimeMs >= 1000;
                            if (updateSpeed) {
                                long elapsedMs = currentTimeMs - lastTimeMs;
                                long readInterval = read - lastRead;
                                bytesPerMs = readInterval / elapsedMs;
                            }

                            // Run progress runnable
                            if (progressDownload != null)
                                progressDownload.run(startSize + read, totalLength, progressValue, bytesPerMs);

                            // Reset
                            if (updateSpeed) {
                                lastTimeMs = currentTimeMs;
                                lastRead = read;
                            }
                        }
                        is.close();
                        os.close();
                        logger.info(I18n.msg("gui.download.finished", to.file().toPath()));

                        // Get digest and run finish runnable
                        if (finish != null && !cancelled) {
                            if (resume) {
                                // We have only read part of the file, the digest in dis is not valid!
                                // Compute digest
                                try {
                                    logger.info("Recomputing sha156 checksum due to being a resumed download: " + to.file().toPath());
                                    md.reset();
                                    InputStream fis = to.read();
                                    length = to.length();
                                    read = 0;
                                    while ((count = fis.read(bytes, 0, bytes.length)) != -1) {
                                        md.update(bytes, 0, count);
                                        read += count;

                                        // Compute progress value
                                        final double progressValue = ((double) read / (double) length) * 100;

                                        // Compute speed
                                        long currentTimeMs = System.currentTimeMillis();
                                        // Update each second
                                        boolean updateSpeed = currentTimeMs - lastTimeMs >= 1000;
                                        if (updateSpeed) {
                                            long elapsedMs = currentTimeMs - lastTimeMs;
                                            long readInterval = read - lastRead;
                                            bytesPerMs = readInterval / elapsedMs;
                                        }

                                        // Run progress runnable
                                        if (progressHashResume != null)
                                            progressHashResume.run(read, length, progressValue, bytesPerMs);
                                    }
                                } catch (IOException e) {
                                    logger.error("Failed computing sha256 checksum of resumed download.", e);
                                }

                            }
                            byte[] digestBytes = md.digest();
                            String digest = bytesToHex(digestBytes);
                            finish.run(digest);
                        }
                    } catch (Exception e) {
                        logger.error(e);
                        if (fail != null)
                            fail.run();
                    }
                } else {
                    logger.error(I18n.msg("gui.download.error.httpstatus", status));
                    if (fail != null)
                        fail.run();
                }
            }

            private String bytesToHex(byte[] hash) {
                StringBuilder hexString = new StringBuilder(2 * hash.length);
                for (int i = 0; i < hash.length; i++) {
                    String hex = Integer.toHexString(0xff & hash[i]);
                    if (hex.length() == 1) {
                        hexString.append('0');
                    }
                    hexString.append(hex);
                }
                return hexString.toString();
            }

            @Override
            public void failed(Throwable t) {
                logger.error(I18n.msg("gui.download.fail"));
                if (fail != null)
                    fail.run();
            }

            @Override
            public void cancelled() {
                logger.error(I18n.msg("gui.download.cancelled", url));
                cancelled = true;
                if (cancel != null)
                    cancel.run();
            }
        });
    }

}
