/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.update;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net.HttpMethods;
import com.badlogic.gdx.Net.HttpRequest;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.Net.HttpResponseListener;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import gaiasky.util.Logger;
import gaiasky.util.parse.Parser;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.TreeSet;

public class VersionChecker implements Runnable {
    public static final int MAX_VERSION_NUMBER = 100000000;
    public static final int MIN_VERSION_NUMBER = 999999;
    private static final Logger.Log logger = Logger.getLogger(VersionChecker.class);
    private static final int VERSION_CHECK_TIMEOUT_MS = 5000;
    private final String stringUrl;
    private EventListener listener;

    public VersionChecker(String stringUrl) {
        this.stringUrl = stringUrl;
    }

    /**
     * Attempts to convert a tag string (maj.min.rev-seq) to an integer
     * number:
     * num = maj*1000000 + min*10000 + rev*100 + seq
     * <p>
     * So, for instance:
     * 1.6.2 ->   01060200
     * 2.2.0 ->   02020000
     * 3.5.3-2 -> 03050302
     * <p>
     * Things like 2.4.0-RC4 are also detected, but the -RC4 suffix is ignored.
     * If no conversion is possible (no maj.min.rev is detected in the string),
     * the version is assumed to be a development branch and the maximum
     * version number (100000000) is given.
     *
     * @param tag The tag string.
     * @return The integer.
     */
    public static Integer stringToVersionNumber(String tag) {
        try {
            int v = 0;
            String[] tokens = tag.split("[.\\-]");
            if (tokens.length < 3) {
                logger.debug("Could not parse version '" + tag + "', assuming development or beta version");
                return MAX_VERSION_NUMBER;
            }
            String major = tokens[0];
            String minor = tokens[1];
            String rev = tokens[2];
            String seq = tokens.length > 3 ? tokens[3] : "00";

            v += Parser.parseInt(major) * 1000000;
            v += Parser.parseInt(minor) * 10000;
            v += Parser.parseInt(rev) * 100;
            v += Parser.parseIntOrElse(seq, 0);

            return v;
        } catch (Exception e) {
            logger.debug("Could not parse version '" + tag + "', assuming development or beta version");
            // Development-branch
            return MAX_VERSION_NUMBER;
        }
    }

    @Override
    public void run() {

        HttpRequest request = new HttpRequest(HttpMethods.GET);
        request.setUrl(stringUrl);
        request.setTimeOut(VERSION_CHECK_TIMEOUT_MS);

        Gdx.net.sendHttpRequest(request, new HttpResponseListener() {
            public void handleHttpResponse(HttpResponse httpResponse) {
                JsonReader reader = new JsonReader();

                // Get latest tag
                TreeSet<VersionObject> tags = new TreeSet<>();

                JsonValue result = reader.parse(httpResponse.getResultAsStream());
                // Parse commit url to get date
                int n = result.size;
                for (int i = 0; i < n; i++) {
                    String tag = result.get(i).getString("name");

                    // Check tag is "major.minor.revision-sequence"
                    if (tag.matches("^(\\D)?\\d+.\\d+(\\D{1})?(.\\d+)?(-\\d+)?$")) {
                        Integer version = stringToVersionNumber(tag);
                        String commitDate = result.get(i).get("commit").getString("created");
                        //Format 2016-12-07T10:41:35+01:00
                        DateTimeFormatter df = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
                        try {
                            LocalDateTime tagDate = LocalDateTime.parse(commitDate, df);
                            tags.add(new VersionObject(result.get(i), version, tagDate.toInstant(ZoneOffset.UTC)));
                        } catch (DateTimeParseException e) {
                            logger.error(e, "Can't parse datetime: " + commitDate);
                        }
                    }
                }

                // Find newest tag.
                if (!tags.isEmpty()) {
                    VersionObject newest = null;
                    for (var tag : tags) {
                        if (newest == null || tag.created.isAfter(newest.created)) {
                            newest = tag;
                        }
                    }

                    // Here is the commit object
                    listener.handle(new VersionCheckEvent(newest.json.getString("name"), newest.version, newest.created));
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

    private static class VersionObject implements Comparable<VersionObject> {
        JsonValue json;
        Integer version;
        Instant created;

        public VersionObject(JsonValue value, Integer version, Instant created) {
            super();
            this.json = value;
            this.version = version;
            this.created = created;
        }

        @Override
        public int compareTo(VersionObject versionObject) {
            return this.version.compareTo(versionObject.version);
        }
    }

    /**
     * Adapts to the new min.maj.rev-seq format automagically.
     *
     * @param versionNumber The configuration version.
     * @return The new version number, corrected for the new format.
     */
    public static int correctVersionNumber(int versionNumber) {
        if (versionNumber > 0 && versionNumber <= VersionChecker.MIN_VERSION_NUMBER) {
            versionNumber *= 100;
        }
        return versionNumber;
    }

}
