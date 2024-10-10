/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.desktop.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.ETC1;
import com.badlogic.gdx.graphics.glutils.KTXTextureData;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.tools.ktx.KTXProcessor;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.commons.io.FileUtils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

/**
 * Compresses PNG and JPG textures into ETC1-compressed KTX files. Uses {@link KTXProcessor}.
 */
public class TextureCompressor {

    final static byte[] HEADER_MAGIC = {(byte)0x0AB, (byte)0x04B, (byte)0x054, (byte)0x058, (byte)0x020, (byte)0x031, (byte)0x031,
            (byte)0x0BB, (byte)0x00D, (byte)0x00A, (byte)0x01A, (byte)0x00A};
    private final static int DISPOSE_DONT = 0;
    private final static int DISPOSE_PACK = 1;
    private final static int DISPOSE_FACE = 2;
    private final static int DISPOSE_LEVEL = 4;

    /**
     * Compresses all PNG and JPG files in the given location into ETC1-compressed KTX files.
     *
     * @param args The directory to process.
     */
    public static void main(String[] args) {

        CLIArgs cliArgs = new CLIArgs();
        JCommander jc = JCommander.newBuilder().addObject(cliArgs).build();
        jc.setProgramName("texturecompressor");
        try {
            jc.parse(args);

            if (cliArgs.help) {
                printUsage(jc);
                return;
            }
        } catch (Exception e) {
            System.out.print("gaiasky: bad program arguments\n\n");
            printUsage(jc);
            return;
        }

        var in = Path.of(cliArgs.input);
        var out = Path.of(cliArgs.output);
        boolean z = cliArgs.z;
        boolean etc1 = true;
        boolean alpha = cliArgs.etc1a;

        if (!Files.exists(in)) {
            System.out.println(String.format("Input directory does not exist: %s", in));
            return;
        }
        if (!Files.exists(out)) {
            boolean created = out.toFile().mkdirs();
            if (created) {
                System.out.println(String.format("Created output directory: %s", out));
            }

        }

        final var compressor = new TextureCompressor();
        final var useAlpha = alpha;
        try (Stream<Path> list = Files.list(in)) {
            list
                    .filter((entry) ->
                            !entry.toFile().isDirectory()
                                    && entry.toFile().exists()
                                    && entry.toFile().isFile()
                                    && entry.toFile().canRead()
                                    && (entry.toString().endsWith(".jpg") || entry.toString().endsWith(".jpeg") || entry.toString().endsWith("png"))
                    )
                    .forEach((entry) -> {
                        var inputName = entry.getFileName().toString();
                        try {
                            var outputName = inputName.substring(0, inputName.lastIndexOf('.')) + (z ? ".zktx" : ".ktx");
                            var outputFile = out.resolve(outputName).toFile();
                            if (outputFile.exists()) {
                                // Delete!
                                FileUtils.delete(outputFile);
                            }
                            System.out.println(inputName + " -> " + outputName);
                            compressor.process(new File[]{entry.toFile()}, outputFile, false, useAlpha, false);
                        } catch (Exception e) {
                            System.err.println("Error: " + e);
                        }
                    });
        } catch (IOException e) {
            System.err.println("Error: " + e);
        }
    }

    @SuppressWarnings("all")
    private void process(File[] input, File output, boolean cubemap, boolean alphaCh, boolean mipmaps) {
        boolean isCubemap = cubemap;
        boolean isPackETC1 = true;
        boolean isAlphaAtlas = alphaCh;
        boolean isGenMipMaps = mipmaps;

        // Check if we have a cube-mapped ktx file as input
        int ktxDispose = DISPOSE_DONT;
        KTXTextureData ktx = null;
        FileHandle file = new FileHandle(input[0]);
        if (file.name().toLowerCase().endsWith(".ktx") || file.name().toLowerCase().endsWith(".zktx")) {
            ktx = new KTXTextureData(file, false);
            if (ktx.getNumberOfFaces() == 6) isCubemap = true;
            ktxDispose = DISPOSE_PACK;
        }

        // Process all faces
        int nFaces = isCubemap ? 6 : 1;
        Image[][] images = new Image[nFaces][];
        int texWidth = -1, texHeight = -1, texFormat = -1, nLevels = 0;
        for (int face = 0; face < nFaces; face++) {
            ETC1.ETC1Data etc1 = null;
            Pixmap facePixmap = null;
            int ktxFace = 0;

            // Load source image (ends up with either ktx, etc1 or facePixmap initialized)
            if (ktx != null && ktx.getNumberOfFaces() == 6) {
                // No loading since we have a ktx file with cubemap as input
                nLevels = ktx.getNumberOfMipMapLevels();
                ktxFace = face;
            } else {
                file = new FileHandle(input[face]);
                System.out.println("Processing : " + file + " for face #" + face);
                if (file.name().toLowerCase().endsWith(".ktx") || file.name().toLowerCase().endsWith(".zktx")) {
                    if (ktx == null || ktx.getNumberOfFaces() != 6) {
                        ktxDispose = DISPOSE_FACE;
                        ktx = new KTXTextureData(file, false);
                        ktx.prepare();
                    }
                    nLevels = ktx.getNumberOfMipMapLevels();
                    texWidth = ktx.getWidth();
                    texHeight = ktx.getHeight();
                } else if (file.name().toLowerCase().endsWith(".etc1")) {
                    etc1 = new ETC1.ETC1Data(file);
                    nLevels = 1;
                    texWidth = etc1.width;
                    texHeight = etc1.height;
                } else {
                    facePixmap = new Pixmap(file);
                    facePixmap.setBlending(Pixmap.Blending.None);
                    facePixmap.setFilter(Pixmap.Filter.BiLinear);
                    nLevels = 1;
                    texWidth = facePixmap.getWidth();
                    texHeight = facePixmap.getHeight();
                }
                if (isGenMipMaps) {
                    if (!MathUtils.isPowerOfTwo(texWidth) || !MathUtils.isPowerOfTwo(texHeight)) throw new GdxRuntimeException(
                            "Invalid input : mipmap generation is only available for power of two textures : " + file);
                    nLevels = Math.max(Integer.SIZE - Integer.numberOfLeadingZeros(texWidth),
                            Integer.SIZE - Integer.numberOfLeadingZeros(texHeight));
                }
            }

            // Process each mipmap level
            images[face] = new Image[nLevels];
            for (int level = 0; level < nLevels; level++) {
                int levelWidth = Math.max(1, texWidth >> level);
                int levelHeight = Math.max(1, texHeight >> level);

                // Get pixmap for this level (ends with either levelETCData or levelPixmap being non null)
                Pixmap levelPixmap = null;
                ETC1.ETC1Data levelETCData = null;
                if (ktx != null) {
                    ByteBuffer ktxData = ktx.getData(level, ktxFace);
                    if (ktxData != null && ktx.getGlInternalFormat() == ETC1.ETC1_RGB8_OES)
                        levelETCData = new ETC1.ETC1Data(levelWidth, levelHeight, ktxData, 0);
                }
                if (ktx != null && levelETCData == null && facePixmap == null) {
                    ByteBuffer ktxData = ktx.getData(0, ktxFace);
                    if (ktxData != null && ktx.getGlInternalFormat() == ETC1.ETC1_RGB8_OES)
                        facePixmap = ETC1.decodeImage(new ETC1.ETC1Data(levelWidth, levelHeight, ktxData, 0), Pixmap.Format.RGB888);
                }
                if (level == 0 && etc1 != null) {
                    levelETCData = etc1;
                }
                if (levelETCData == null && etc1 != null && facePixmap == null) {
                    facePixmap = ETC1.decodeImage(etc1, Pixmap.Format.RGB888);
                }
                if (levelETCData == null) {
                    levelPixmap = new Pixmap(levelWidth, levelHeight, facePixmap.getFormat());
                    levelPixmap.setBlending(Pixmap.Blending.None);
                    levelPixmap.setFilter(Pixmap.Filter.BiLinear);
                    levelPixmap.drawPixmap(facePixmap, 0, 0, facePixmap.getWidth(), facePixmap.getHeight(), 0, 0,
                            levelPixmap.getWidth(), levelPixmap.getHeight());
                }
                if (levelETCData == null && levelPixmap == null)
                    throw new GdxRuntimeException("Failed to load data for face " + face + " / mipmap level " + level);

                // Create alpha atlas
                if (isAlphaAtlas) {
                    if (levelPixmap == null) levelPixmap = ETC1.decodeImage(levelETCData, Pixmap.Format.RGB888);
                    int w = levelPixmap.getWidth(), h = levelPixmap.getHeight();
                    Pixmap pm = new Pixmap(w, h * 2, levelPixmap.getFormat());
                    pm.setBlending(Pixmap.Blending.None);
                    pm.setFilter(Pixmap.Filter.BiLinear);
                    pm.drawPixmap(levelPixmap, 0, 0);
                    for (int y = 0; y < h; y++) {
                        for (int x = 0; x < w; x++) {
                            int alpha = (levelPixmap.getPixel(x, y)) & 0x0FF;
                            pm.drawPixel(x, y + h, (alpha << 24) | (alpha << 16) | (alpha << 8) | 0x0FF);
                        }
                    }
                    levelPixmap.dispose();
                    levelPixmap = pm;
                    levelETCData = null;
                }

                // Perform ETC1 compression
                if (levelETCData == null && isPackETC1) {
                    if (levelPixmap.getFormat() != Pixmap.Format.RGB888 && levelPixmap.getFormat() != Pixmap.Format.RGB565) {
                        if (!isAlphaAtlas)
                            System.out.println("Converting from " + levelPixmap.getFormat() + " to RGB888 for ETC1 compression");
                        Pixmap tmp = new Pixmap(levelPixmap.getWidth(), levelPixmap.getHeight(), Pixmap.Format.RGB888);
                        tmp.setBlending(Pixmap.Blending.None);
                        tmp.setFilter(Pixmap.Filter.BiLinear);
                        tmp.drawPixmap(levelPixmap, 0, 0, 0, 0, levelPixmap.getWidth(), levelPixmap.getHeight());
                        levelPixmap.dispose();
                        levelPixmap = tmp;
                    }
                    // System.out.println("Compress : " + levelWidth + " x " + levelHeight);
                    levelETCData = ETC1.encodeImagePKM(levelPixmap);
                    levelPixmap.dispose();
                    levelPixmap = null;
                }

                // Save result to ouput ktx
                images[face][level] = new Image();
                images[face][level].etcData = levelETCData;
                images[face][level].pixmap = levelPixmap;
                if (levelPixmap != null) {
                    levelPixmap.dispose();
                    facePixmap = null;
                }
            }

            // Dispose resources
            if (facePixmap != null) {
                facePixmap.dispose();
                facePixmap = null;
            }
            if (etc1 != null) {
                etc1.dispose();
                etc1 = null;
            }
            if (ktx != null && ktxDispose == DISPOSE_FACE) {
                ktx.disposePreparedData();
                ktx = null;
            }
        }
        if (ktx != null) {
            ktx.disposePreparedData();
            ktx = null;
        }

        int glType, glTypeSize, glFormat, glInternalFormat, glBaseInternalFormat;
        if (isPackETC1) {
            glType = glFormat = 0;
            glTypeSize = 1;
            glInternalFormat = ETC1.ETC1_RGB8_OES;
            glBaseInternalFormat = GL20.GL_RGB;
        } else if (images[0][0].pixmap != null) {
            glType = images[0][0].pixmap.getGLType();
            glTypeSize = 1;
            glFormat = images[0][0].pixmap.getGLFormat();
            glInternalFormat = images[0][0].pixmap.getGLInternalFormat();
            glBaseInternalFormat = glFormat;
        } else
            throw new GdxRuntimeException("Unsupported output format");

        int totalSize = 12 + 13 * 4;
        for (int level = 0; level < nLevels; level++) {
            System.out.println("Level: " + level);
            int faceLodSize = images[0][level].getSize();
            int faceLodSizeRounded = (faceLodSize + 3) & ~3;
            totalSize += 4;
            totalSize += nFaces * faceLodSizeRounded;
        }

        try {
            DataOutputStream out;
            if (output.getName().toLowerCase().endsWith(".zktx")) {
                out = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(output)));
                out.writeInt(totalSize);
            } else
                out = new DataOutputStream(new FileOutputStream(output));

            out.write(HEADER_MAGIC);
            out.writeInt(0x04030201);
            out.writeInt(glType);
            out.writeInt(glTypeSize);
            out.writeInt(glFormat);
            out.writeInt(glInternalFormat);
            out.writeInt(glBaseInternalFormat);
            out.writeInt(texWidth);
            out.writeInt(isAlphaAtlas ? (2 * texHeight) : texHeight);
            out.writeInt(0); // depth (not supported)
            out.writeInt(0); // n array elements (not supported)
            out.writeInt(nFaces);
            out.writeInt(nLevels);
            out.writeInt(0); // No additional info (key/value pairs)
            for (int level = 0; level < nLevels; level++) {
                int faceLodSize = images[0][level].getSize();
                int faceLodSizeRounded = (faceLodSize + 3) & ~3;
                out.writeInt(faceLodSize);
                for (int face = 0; face < nFaces; face++) {
                    byte[] bytes = images[face][level].getBytes();
                    out.write(bytes);
                    for (int j = bytes.length; j < faceLodSizeRounded; j++)
                        out.write((byte)0x00);
                }
            }

            out.close();
            System.out.println("Finished");
        } catch (Exception e) {
            Gdx.app.error("KTXProcessor", "Error writing to file: " + output.getName(), e);
        }
    }
    private static class Image {

        public ETC1.ETC1Data etcData;
        public Pixmap pixmap;

        public Image () {
        }

        public int getSize () {
            if (etcData != null) return etcData.compressedData.limit() - etcData.dataOffset;
            throw new GdxRuntimeException("Unsupported output format, try adding '-etc1' as argument");
        }

        public byte[] getBytes () {
            if (etcData != null) {
                byte[] result = new byte[getSize()];
                ((Buffer)etcData.compressedData).position(etcData.dataOffset);
                etcData.compressedData.get(result);
                return result;
            }
            throw new GdxRuntimeException("Unsupported output format, try adding '-etc1' as argument");
        }

    }

    private static void printUsage(JCommander jc) {
        jc.usage();
    }

    /**
     * Program CLI arguments.
     */
    private static class CLIArgs {
        @Parameter(names = {"-h", "--help"}, description = "Show program options and usage information.", help = true, order = 0)
        private boolean help = false;

        @Parameter(names = {"-i", "--input"}, description = "Specify the input directory where the source images are located.", order = 1, required = true)
        private String input = null;

        @Parameter(names = {"-o", "--output"}, description = "Specify the output directory.", order = 2, required = true)
        private String output = null;

        @Parameter(names = {"-z"}, description = "Produce zipped .zktx files as output.", order = 4)
        private boolean z = false;

        @Parameter(names = {"--etc1a"}, description = "Use ETC1 with alpha, instead of regular ETC1.", order = 5)
        private boolean etc1a = false;
    }
}
