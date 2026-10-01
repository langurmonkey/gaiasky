/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import gaiasky.util.urlprotocol.URLProtocolHandler;
import org.junit.Assert;
import org.junit.Test;

public class URLProtocolHandlerTest {

    @Test
    public void testIsDatasetUrl() {
        // Valid URLs.
        Assert.assertTrue(URLProtocolHandler.isDatasetUrl("gaiasky://load?dataset=key"));
        Assert.assertTrue(URLProtocolHandler.isDatasetUrl("gaiasky://load?dataset=https%3A%2F%2Fexample.com%2Fdataset.tar.gz"));
        Assert.assertTrue(URLProtocolHandler.isDatasetUrl("GAIASKY://load?dataset=key"));

        // Invalid URLs.
        Assert.assertFalse(URLProtocolHandler.isDatasetUrl(null));
        Assert.assertFalse(URLProtocolHandler.isDatasetUrl(""));
        Assert.assertFalse(URLProtocolHandler.isDatasetUrl("gaiasky:load"));
        Assert.assertFalse(URLProtocolHandler.isDatasetUrl("http://load?dataset=key"));
        Assert.assertFalse(URLProtocolHandler.isDatasetUrl("other://load?dataset=key"));
        Assert.assertFalse(URLProtocolHandler.isDatasetUrl("gaiaskyload?dataset=key"));
    }

    @Test
    public void testHandleMalformedUrlDoesNotThrow() {
        // Malformed URLs must be logged and ignored, not thrown.
        URLProtocolHandler.handle("gaiasky://", null, null);
        URLProtocolHandler.handle("gaiasky://%zz", null, null);
    }

    @Test
    public void testHandleUnknownActionIsIgnored() {
        // Unknown actions must be ignored without exceptions.
        URLProtocolHandler.handle("gaiasky://unknown?action=foo", null, null);
    }

    @Test
    public void testHandleMissingParameterIsIgnored() {
        // Missing dataset parameter must be ignored without exceptions.
        URLProtocolHandler.handle("gaiasky://load", null, null);
        URLProtocolHandler.handle("gaiasky://load?other=value", null, null);
    }

    @Test
    public void testHandleNonUrlArgumentIsIgnored() {
        // Non-URL arguments must be ignored.
        URLProtocolHandler.handle("not-a-url", null, null);
        URLProtocolHandler.handle("", null, null);
    }

}
