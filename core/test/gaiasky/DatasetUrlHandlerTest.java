/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import gaiasky.util.datadesc.DatasetUrlHandler;
import org.junit.Assert;
import org.junit.Test;

public class DatasetUrlHandlerTest {

    @Test
    public void testIsDatasetUrl() {
        // Valid URLs.
        Assert.assertTrue(DatasetUrlHandler.isDatasetUrl("gaiasky://load?dataset=key"));
        Assert.assertTrue(DatasetUrlHandler.isDatasetUrl("gaiasky://load?dataset=https%3A%2F%2Fexample.com%2Fdataset.tar.gz"));
        Assert.assertTrue(DatasetUrlHandler.isDatasetUrl("GAIASKY://load?dataset=key"));

        // Invalid URLs.
        Assert.assertFalse(DatasetUrlHandler.isDatasetUrl(null));
        Assert.assertFalse(DatasetUrlHandler.isDatasetUrl(""));
        Assert.assertFalse(DatasetUrlHandler.isDatasetUrl("gaiasky:load"));
        Assert.assertFalse(DatasetUrlHandler.isDatasetUrl("http://load?dataset=key"));
        Assert.assertFalse(DatasetUrlHandler.isDatasetUrl("other://load?dataset=key"));
        Assert.assertFalse(DatasetUrlHandler.isDatasetUrl("gaiaskyload?dataset=key"));
    }

    @Test
    public void testHandleMalformedUrlDoesNotThrow() {
        // Malformed URLs must be logged and ignored, not thrown.
        DatasetUrlHandler.handle("gaiasky://", null, null);
        DatasetUrlHandler.handle("gaiasky://%zz", null, null);
    }

    @Test
    public void testHandleUnknownActionIsIgnored() {
        // Unknown actions must be ignored without exceptions.
        DatasetUrlHandler.handle("gaiasky://unknown?action=foo", null, null);
    }

    @Test
    public void testHandleMissingParameterIsIgnored() {
        // Missing dataset parameter must be ignored without exceptions.
        DatasetUrlHandler.handle("gaiasky://load", null, null);
        DatasetUrlHandler.handle("gaiasky://load?other=value", null, null);
    }

    @Test
    public void testHandleNonUrlArgumentIsIgnored() {
        // Non-URL arguments must be ignored.
        DatasetUrlHandler.handle("not-a-url", null, null);
        DatasetUrlHandler.handle("", null, null);
    }

}
