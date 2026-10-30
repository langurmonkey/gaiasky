/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Pins the volume occlusion invariants: the colour pass compares two stored depths (its own front and
 * the shared buffer) instead of testing a recomputed one, it clips the march at that distance rather
 * than dropping the fragment, and the pre-pass does not clip at all.
 */
public class VolumeDepthLatticeTest {

    private static final List<String> SHADERS = List.of("assets/shader/volume.galaxy.fragment.glsl",
                                                         "assets/shader/volume.fragment.glsl");

    private static String read(String relative)
            throws IOException {
        // Try the usual working directories.
        for (String root : List.of("..", ".", "../..")) {
            Path p = Paths.get(root, relative);
            if (Files.exists(p)) {
                return Files.readString(p);
            }
        }
        throw new IOException("Cannot find " + relative + " from " + Paths.get(".").toAbsolutePath());
    }

    /** Drops line comments. **/
    private static String stripComments(String src) {
        StringBuilder sb = new StringBuilder();
        for (String line : src.split("\n")) {
            int comment = line.indexOf("//");
            sb.append(comment >= 0 ? line.substring(0, comment) : line).append('\n');
        }
        return sb.toString();
    }

    @Test
    public void thePrePassIsPhaseStable()
            throws IOException {
        for (String shader : SHADERS) {
            String src = read(shader);

            // The pre-pass jitter must be phase-stable, or the stored front crawls.
            Assert.assertTrue(shader + " must keep the pre-pass jitter free of u_time",
                              src.contains("#ifdef DEPTH_PASS\n    float jitter = hash12(gl_FragCoord.xy);"));
        }
    }

    @Test
    public void theOcclusionTestComparesStoredDepths()
            throws IOException {
        for (String shader : SHADERS) {
            String code = stripComments(read(shader));

            // Both depths read as textures.
            Assert.assertTrue(shader + " must read this volume's own front from its mask texture",
                              code.contains("texelFetch(u_volumeDepthTexture, texel, 0).r"));
            Assert.assertTrue(shader + " must read the shared depth buffer",
                              code.contains("texelFetch(u_volumeSceneDepth, texel, 0).r"));

            // Both lookups must use the same texel, or a half-resolution volume clips its own dust.
            Assert.assertTrue(shader + " must look the two depths up at one texel",
                              code.contains("ivec2 texel = ivec2(gl_FragCoord.xy * u_volumeDepthMaskScale);"));

            // Both are stored log depths, so the margin is just the attachment quantum.
            Assert.assertTrue(shader + " must treat a nearer buffer as a foreign occluder",
                              code.contains("if (!(scene < mine - 1.0e-5))"));
        }
    }

    @Test
    public void theColourPassClipsInsteadOfTesting()
            throws IOException {
        for (String shader : SHADERS) {
            String code = stripComments(read(shader));

            // A distance applied to the span of the march.
            Assert.assertTrue(shader + " must clip the march at the occluder's distance",
                              code.contains("exitDist = min(exitDist, clipDist);"));
            // Threaded into every march of the file.
            Assert.assertEquals(shader + " must thread the clip into every raymarch",
                                count(code, "clipDist);"), count(code, "raymarchVolume"));

            // Only the colour pass clips: the pre-pass writes that very depth.
            Assert.assertTrue(shader + " must not clip in the pre-pass",
                              code.contains("#ifndef DEPTH_PASS"));
            // The mask code is compiled unconditionally, so no attribute has to exist for the shader
            // to be correct; the scale uniform, zero until a mask is bound, is what gates it.
            Assert.assertTrue(shader + " must gate the clip on the mask being bound",
                              code.contains("if (u_volumeDepthMaskScale.x <= 0.0)"));
        }
    }

    @Test
    public void thePrePassIsComposedBackIntoTheScene()
            throws IOException {
        String renderer = read("core/src/gaiasky/scene/system/render/draw/VolumeRenderer.java");

        // Without the compose, the dust would stop occluding the stars and labels.
        Assert.assertTrue("the depth pass must bind a per-volume target", renderer.contains("depthMask.begin(volume)"));
        Assert.assertTrue("the depth pass must compose the fronts into the scene depth", renderer.contains("depthMask.compose("));
        // The colour pass needs both textures before its shader compiles.
        Assert.assertTrue("the colour pass must bind the mask", renderer.contains("setVolumeDepthMask("));
        Assert.assertTrue("the pre-pass must go back to the scene's framebuffer",
                          renderer.contains("depthMask.restore();"));
    }

    /** The colour pass must not depth-test against its own front. */
    @Test
    public void theColourPassDoesNotTestAgainstItsOwnDepth()
            throws IOException {
        String renderer = read("core/src/gaiasky/scene/system/render/draw/model/ModelEntityRenderSystem.java");
        String colourPass = renderer.substring(renderer.indexOf("} else if (volume.writeDepth) {"));
        String branch = colourPass.substring(0, colourPass.indexOf("} else {"));
        Assert.assertTrue("the volume colour pass must not test or write depth",
                          branch.contains("mc.depthTestDisable();"));
        Assert.assertFalse("the volume colour pass must not depth-test against its own front",
                           branch.contains("depthTestReadOnly"));
        Assert.assertFalse("the volume colour pass must not depth-test against its own front",
                           branch.contains("depthTestReadWrite"));
    }

    private static int count(String haystack,
                             String needle) {
        int n = 0, i = 0;
        while ((i = haystack.indexOf(needle, i)) >= 0) {
            n++;
            i += needle.length();
        }
        return n;
    }
}
