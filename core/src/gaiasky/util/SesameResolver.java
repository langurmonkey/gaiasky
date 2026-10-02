/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import gaiasky.GaiaSky;
import gaiasky.util.Logger.Log;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Queries the Sesame name resolver of the <a href="http://cds.unistra.fr">CDS</a> to find out
 * information about an object by its name, asynchronously with respect to the render
 * thread.
 */
public class SesameResolver {
    private static final Log logger = Logger.getLogger(SesameResolver.class);

    /** Timeout in seconds for the query. **/
    private static final int TIMEOUT_S = 10;

    private static final Pattern P_ONAME = Pattern.compile("<oname>(.*?)</oname>", Pattern.DOTALL);
    private static final Pattern P_ALIAS = Pattern.compile("<alias>(.*?)</alias>", Pattern.DOTALL);
    private static final Pattern P_OTYPE = Pattern.compile("<otype>(.*?)</otype>", Pattern.DOTALL);
    private static final Pattern P_JRADEG = Pattern.compile("<jradeg>(.*?)</jradeg>", Pattern.DOTALL);
    private static final Pattern P_JDEDEG = Pattern.compile("<jdedeg>(.*?)</jdedeg>", Pattern.DOTALL);

    /**
     * The result of a Sesame name resolution.
     *
     * @param oname  The main object name.
     * @param aliases The list of aliases of the object.
     * @param otype  The object type, if available.
     * @param raDeg  The right ascension in degrees, if available.
     * @param decDeg The declination in degrees, if available.
     */
    public record Result(String oname, List<String> aliases, String otype, Double raDeg, Double decDeg) {
    }

    private SesameResolver() {
    }

    /**
     * Resolves the given object name in Sesame, and calls the consumer in the render thread.
     * The consumer receives the result, which is null if the object was not found, and an
     * error message, which is null when the query succeeded.
     *
     * @param name     The object name.
     * @param consumer The consumer of the result and the error, if any.
     */
    public static void resolve(String name, BiConsumer<Result, String> consumer) {
        if (name == null || name.isBlank()) {
            return;
        }
        String url = GaiaSky.settings().program.search.sesameUrl;
        if (url == null || url.isBlank()) {
            logger.warn("No Sesame URL configured.");
            return;
        }
        // Sesame expects the object name as a bare query key, i.e. "-oxpI?Betelgeuse".
        // The "NAME=" form always resolves to "Nothing found".
        String separator = url.contains("?") ? "&" : "?";
        String query = url + separator + URLEncoder.encode(name.trim(), StandardCharsets.UTF_8);
        GaiaSky.instance.getExecutorService().submit(() -> {
            Result result = null;
            String error = null;
            try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(TIMEOUT_S)).build()) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(query))
                        .timeout(Duration.ofSeconds(TIMEOUT_S))
                        .header("User-Agent", "Gaia Sky")
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    result = parse(response.body());
                } else {
                    error = "HTTP " + response.statusCode();
                    logger.warn("Sesame query returned status " + response.statusCode() + ".");
                }
            } catch (Exception e) {
                error = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                logger.warn("Error querying Sesame: " + e.getMessage());
            }
            Result r = result;
            String err = error;
            GaiaSky.postRunnable(() -> consumer.accept(r, err));
        });
    }

    /**
     * Parses the XML response of a Sesame query.
     *
     * @param xml The XML document.
     * @return The result, or null if the object was not found.
     */
    private static Result parse(String xml) {
        Matcher m = P_ONAME.matcher(xml);
        if (!m.find()) {
            return null;
        }
        String oname = clean(m.group(1));

        List<String> aliases = new ArrayList<>();
        Matcher ma = P_ALIAS.matcher(xml);
        while (ma.find()) {
            String a = clean(ma.group(1));
            if (!a.isEmpty()) {
                aliases.add(a);
            }
        }

        String otype = match(P_OTYPE, xml);
        Double ra = number(match(P_JRADEG, xml));
        Double dec = number(match(P_JDEDEG, xml));

        return new Result(oname, aliases, otype, ra, dec);
    }

    private static String match(Pattern p, String xml) {
        Matcher m = p.matcher(xml);
        return m.find() ? clean(m.group(1)) : "";
    }

    private static Double number(String s) {
        try {
            return s.isEmpty() ? null : Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String clean(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").trim();
    }
}