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
 * The volume depth mask is only usable if the pre-pass and the colour pass march the same lattice.
 * <p>
 * {@link gaiasky.scene.system.render.draw.model.ModelEntityRenderSystem#renderVolume} now draws the
 * colour pass with {@code GL_LEQUAL} and no depth writes, against the mask the pre-pass left in the
 * depth buffer - that is what lets two volumes hide each other. The shader writes the depth of its
 * own first dense sample, so the test compares the volume against its own mask as well, and the
 * distance to that sample is discontinuous near a dust edge: one step either way and a thin filament
 * is missed. Any difference in the sample positions between the two passes therefore punches the
 * volume with its own mask, in speckles along every lane, which is exactly what happened when the
 * pre-pass stepped at 0.02 UVW and the colour pass at 0.01 with a phase that re-rolled every frame.
 * <p>
 * These assertions pin the properties that keep the two lattices identical.
 */
public class VolumeDepthLatticeTest {

    private static final List<String> SHADERS = List.of("assets/shader/volume.galaxy.fragment.glsl",
                                                         "assets/shader/volume.fragment.glsl");

    private static String read(String relative)
            throws IOException {
        // Tests run from the module directory, but be forgiving about how they are launched.
        for (String root : List.of("..", ".", "../..")) {
            Path p = Paths.get(root, relative);
            if (Files.exists(p)) {
                return Files.readString(p);
            }
        }
        throw new IOException("Cannot find " + relative + " from " + Paths.get(".").toAbsolutePath());
    }

    /** Drops line comments, so a note about a symbol does not read as a use of it. **/
    private static String stripComments(String src) {
        StringBuilder sb = new StringBuilder();
        for (String line : src.split("\n")) {
            int comment = line.indexOf("//");
            sb.append(comment >= 0 ? line.substring(0, comment) : line).append('\n');
        }
        return sb.toString();
    }

    @Test
    public void theMarchLatticeDoesNotDependOnThePass()
            throws IOException {
        for (String shader : SHADERS) {
            String src = read(shader);

            // Only the pre-pass' jitter may be phase-stable: its mask has to hold still. The colour
            // pass animates its phase, or the march noise freezes into a stipple.
            Assert.assertTrue(shader + " must keep the pre-pass jitter free of u_time",
                              src.contains("#ifdef DEPTH_PASS\n    float jitter = hash12(gl_FragCoord.xy);"));

            // The step must be the same in both passes.
            Assert.assertTrue(shader + " must declare the march step once, outside any DEPTH_PASS branch",
                              src.contains("const float TARGET_STEP_UVW = 0.01;"));
            Assert.assertFalse(shader + " must not vary the march step per pass",
                               src.contains("#ifdef DEPTH_PASS\n    const float TARGET_STEP_UVW"));

            // Nor may a pass drop a box: that changes the union span and therefore the step count.
            Assert.assertFalse(shader + " must not drop a box in one pass only",
                               src.contains("iEmissive = vec2(-1.0)"));
            Assert.assertFalse(shader + " must not drop a box in one pass only",
                               src.contains("iOpaque = vec2(-1.0)"));
            Assert.assertFalse(shader + " must not narrow a slot's extent in one pass only",
                               src.contains("uvwScaleEmissive = 0.0"));
            Assert.assertFalse(shader + " must not narrow a slot's extent in one pass only",
                               src.contains("uvwScaleOpaque = 0.0"));
        }
    }

    @Test
    public void theColourPassWritesTheDepthItIsTestedAgainst()
            throws IOException {
        for (String shader : SHADERS) {
            String src = read(shader);

            // The colour pass is drawn with a depth test, which only makes sense because the fragment
            // shader replaces the rasterised depth (the far side of the proxy mesh) with the depth of
            // its own first dense sample. Without this write the GL_LEQUAL test would compare the far
            // side and delete every fragment.
            Assert.assertTrue(shader + " must write gl_FragDepth from the first dense sample",
                              src.contains("gl_FragDepth = firstDepth > 0.0"));

            // A hair of margin for float noise between two separately compiled programs. It is not a
            // lattice tolerance: it is four orders of magnitude below a step.
            Assert.assertTrue(shader + " must keep a float-noise margin on the depth it writes",
                              src.contains("firstDepth * 0.9999"));
        }
    }
}
