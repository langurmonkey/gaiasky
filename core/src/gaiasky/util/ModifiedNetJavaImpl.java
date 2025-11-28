/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;


import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpStatus;
import com.badlogic.gdx.net.NetJavaImpl;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.StreamUtils;
import gaiasky.GaiaSky;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * This class is a modified version of {@link NetJavaImpl} that ensures the {@code doInput} flag is always set to {@code true}.
 *
 * <p>In the original {@link NetJavaImpl} class, the {@code doInput} flag is determined based on the HTTP method
 * (e.g., it would be set to {@code false} for {@code HEAD} requests). However, this modification forces the flag
 * to be always {@code true}, ensuring that the connection can read the response code and headers, even for {@code HEAD} requests.</p>
 *
 * <p>This change allows the retrieval of HTTP status codes for {@code HEAD} requests, which are typically sent to
 * retrieve only the headers (without the response body). By forcing {@code doInput} to be {@code true}, it ensures
 * that the response code can always be read, while avoiding any attempts to read the body of the response (which will be empty
 * for {@code HEAD} requests).</p>
 *
 * <p>Note that setting {@code doInput} to {@code true} for {@code HEAD} requests does not mean the response body
 * will be transmitted. The body is empty for {@code HEAD} requests; only the headers (including the status code)
 * are accessible.</p>
 *
 * <p>For all HTTP methods, this modified implementation will still handle reading and writing data to the connection as needed,
 * but it will not fail to read the response code due to an incorrect {@code doInput} setting.</p>
 *
 * <p>This class works well for scenarios where you need to test the connection (e.g., for checking if a URL is reachable)
 * by sending a {@code HEAD} request, retrieving the status code, and ensuring the connection is valid.</p>
 *
 * <p>Finally, this implementation does not create its own {@link java.util.concurrent.ThreadPoolExecutor}, but uses the
 * one provided by the main instance of {@link GaiaSky}.</p>
 *
 * @see NetJavaImpl
 * @see HttpURLConnection
 * @see Net.HttpRequest
 * @see Net.HttpResponseListener
 */
public class ModifiedNetJavaImpl {

    static class HttpClientResponse implements Net.HttpResponse {
        private final HttpURLConnection connection;
        private HttpStatus status;

        public HttpClientResponse(HttpURLConnection connection) {
            this.connection = connection;
            try {
                this.status = new HttpStatus(connection.getResponseCode());
            } catch (IOException e) {
                this.status = new HttpStatus(-1);
            }
        }

        @Override
        public byte[] getResult() {
            InputStream input = getInputStream();

            // If the response does not contain any content, input will be null.
            if (input == null) {
                return StreamUtils.EMPTY_BYTES;
            }

            try {
                return StreamUtils.copyStreamToByteArray(input, connection.getContentLength());
            } catch (IOException e) {
                return StreamUtils.EMPTY_BYTES;
            } finally {
                StreamUtils.closeQuietly(input);
            }
        }

        @Override
        public String getResultAsString() {
            InputStream input = getInputStream();

            // If the response does not contain any content, input will be null.
            if (input == null) {
                return "";
            }

            try {
                return StreamUtils.copyStreamToString(input, connection.getContentLength(), "UTF8");
            } catch (IOException e) {
                return "";
            } finally {
                StreamUtils.closeQuietly(input);
            }
        }

        @Override
        public InputStream getResultAsStream() {
            return getInputStream();
        }

        @Override
        public HttpStatus getStatus() {
            return status;
        }

        @Override
        public String getHeader(String name) {
            return connection.getHeaderField(name);
        }

        @Override
        public Map<String, List<String>> getHeaders() {
            return connection.getHeaderFields();
        }

        private InputStream getInputStream() {
            try {
                return connection.getInputStream();
            } catch (IOException e) {
                return connection.getErrorStream();
            }
        }
    }

    final ObjectMap<Net.HttpRequest, HttpURLConnection> connections;
    final ObjectMap<Net.HttpRequest, Net.HttpResponseListener> listeners;
    final ObjectMap<Net.HttpRequest, Future<?>> tasks;

    public ModifiedNetJavaImpl() {
        this(Integer.MAX_VALUE);
    }

    public ModifiedNetJavaImpl(int maxThreads) {
        connections = new ObjectMap<>();
        listeners = new ObjectMap<>();
        tasks = new ObjectMap<>();
    }

    public void sendHttpRequest(final Net.HttpRequest httpRequest, final Net.HttpResponseListener httpResponseListener) {
        if (httpRequest.getUrl() == null) {
            httpResponseListener.failed(new GdxRuntimeException("can't process a HTTP request without URL set"));
            return;
        }

        try {
            final String method = httpRequest.getMethod();
            URL url;

            // should be enabled to upload data.
            final boolean doingOutPut = method.equalsIgnoreCase(Net.HttpMethods.POST) || method.equalsIgnoreCase(Net.HttpMethods.PUT)
                    || method.equalsIgnoreCase(Net.HttpMethods.PATCH);

            if (method.equalsIgnoreCase(Net.HttpMethods.GET) || method.equalsIgnoreCase(Net.HttpMethods.HEAD)) {
                String queryString = "";
                String value = httpRequest.getContent();
                if (value != null && !value.isEmpty()) queryString = "?" + value;
                url = URI.create(httpRequest.getUrl() + queryString).toURL();
            } else {
                url = URI.create(httpRequest.getUrl()).toURL();
            }

            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(doingOutPut);
            // Force doInput to true because we need to read the status code from the response.
            connection.setDoInput(true);
            connection.setRequestMethod(method);
            HttpURLConnection.setFollowRedirects(httpRequest.getFollowRedirects());

            putIntoConnectionsAndListeners(httpRequest, httpResponseListener, connection);

            // Headers get set regardless of the method
            for (Map.Entry<String, String> header : httpRequest.getHeaders().entrySet())
                connection.addRequestProperty(header.getKey(), header.getValue());

            // Set Timeouts
            connection.setConnectTimeout(httpRequest.getTimeOut());
            connection.setReadTimeout(httpRequest.getTimeOut());

            tasks.put(httpRequest, GaiaSky.instance.getExecutorService().submit(() -> {
                try {
                    // Set the content for POST and PUT (GET has the information embedded in the URL)
                    if (doingOutPut) {
                        // we probably need to use the content as stream here instead of using it as a string.
                        String contentAsString = httpRequest.getContent();
                        if (contentAsString != null) {
                            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8);
                            try {
                                writer.write(contentAsString);
                            } finally {
                                StreamUtils.closeQuietly(writer);
                            }
                        } else {
                            InputStream contentAsStream = httpRequest.getContentStream();
                            if (contentAsStream != null) {
                                OutputStream os = connection.getOutputStream();
                                try {
                                    StreamUtils.copyStream(contentAsStream, os);
                                } finally {
                                    StreamUtils.closeQuietly(os);
                                }
                            }
                        }
                    }

                    connection.connect();

                    final HttpClientResponse clientResponse = new HttpClientResponse(connection);
                    try {
                        Net.HttpResponseListener listener = getFromListeners(httpRequest);

                        if (listener != null) {
                            listener.handleHttpResponse(clientResponse);
                        }
                    } finally {
                        removeFromConnectionsAndListeners(httpRequest);
                        connection.disconnect();
                    }
                } catch (final Exception e) {
                    connection.disconnect();
                    try {
                        httpResponseListener.failed(e);
                    } finally {
                        removeFromConnectionsAndListeners(httpRequest);
                    }
                }
            }));
        } catch (Exception e) {
            try {
                httpResponseListener.failed(e);
            } finally {
                removeFromConnectionsAndListeners(httpRequest);
            }
        }
    }

    synchronized void removeFromConnectionsAndListeners(final Net.HttpRequest httpRequest) {
        connections.remove(httpRequest);
        listeners.remove(httpRequest);
        tasks.remove(httpRequest);
    }

    synchronized void putIntoConnectionsAndListeners(final Net.HttpRequest httpRequest,
                                                     final Net.HttpResponseListener httpResponseListener, final HttpURLConnection connection) {
        connections.put(httpRequest, connection);
        listeners.put(httpRequest, httpResponseListener);
    }

    synchronized Net.HttpResponseListener getFromListeners(Net.HttpRequest httpRequest) {
        Net.HttpResponseListener httpResponseListener = listeners.get(httpRequest);
        return httpResponseListener;
    }
}
