/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */
package gaiasky.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Disposable;
import gaiasky.util.Logger.Log;

/**
 * Helper to generate and inject FreeType fonts into the Gaia Sky skin.
 */
public class FontFactory implements Disposable {
    private static final Log logger = Logger.getLogger(FontFactory.class);

    private FreeTypeFontGenerator uiGen;
    private FreeTypeFontGenerator monoGen;
    private FreeTypeFontGenerator titleGen;
    private FreeTypeFontGenerator titleBigGen;

    /** Western languages character set (Spanish, Catala, German, French, Italian, Slovenian, Turkish, Russian, Bulgarian). **/
    public static final String COMMON_CHARS = """
            AÀÁÄÂBCDEÈÉËÊFGĞHIÌÍÏÎİJKLMNOÒÓÔÖPQRSŞTUÙÚÜÛVWXYZÑÇ
            aàáäâbcdeèéêëfgğhiìíîïıjȷklmnoòóôöpqrsştuùúûüvwxyzñç
            1234567890!`?'.,;:()[]{}<>|/@\\^$-%+=#_&~*
            θωερτψυιοπασδφγηςκλζχξωβνμΘΩΕΡΤΨΥΙΟΠΑΣΔΦΓΗςΚΛΖΧΞΩΒΝΜ
            АаБбВвГгДдЕеЁёЖжЗзИиЙйКкЛлМмНнОоПпРрСсТтУуФфХхЦцЧчШшЩщЪъЫыЬьЭэЮюЯя
            ±∓×·÷√∑∫∮¬∝∞≠≈⇔↔≪≫≤≥°ℏÅ⊙☉☼☀¶ß©®£€¼½¾²³čžšČŽŠ„“"
            """;

    public void generateFonts(Skin skin, String lang) {
        // Dispose old generators.
        dispose();

        // Timing.
        long start = System.currentTimeMillis();

        // Prepare Generators.
        boolean isChinese = lang.startsWith("zh");
        String uiFontPath = isChinese ? "fonts/NotoSansSC-Regular-Subset.ttf" : "fonts/InterDisplay-Regular.ttf";
        String monoFontPath = isChinese ? "fonts/NotoSansSC-Regular-Subset.ttf" : "fonts/LiberationMono-Bold.ttf";
        String titleFontPath = isChinese ? "fonts/NotoSansSC-Bold-Subset.ttf" : "fonts/InterDisplay-Bold.ttf";
        String titleBigFontPath = "fonts/Ethnocentric-Regular.ttf";

        monoGen = new FreeTypeFontGenerator(Gdx.files.internal(monoFontPath));
        titleGen = new FreeTypeFontGenerator(Gdx.files.internal(titleFontPath));
        titleBigGen = new FreeTypeFontGenerator(Gdx.files.internal(titleBigFontPath));
        uiGen = new FreeTypeFontGenerator(Gdx.files.internal(uiFontPath));

        FreeTypeFontParameter params = new FreeTypeFontParameter();
        params.mono = false;
        params.hinting = FreeTypeFontGenerator.Hinting.Slight;
        params.magFilter = TextureFilter.Linear;
        params.minFilter = TextureFilter.Linear;
        // Common characters.
        params.characters = COMMON_CHARS;
        // Chinese characters are loaded on-demand (incremental).
        if (isChinese) {
            params.incremental = true;
            FreeTypeFontGenerator.setMaxTextureSize(1024);
        }

        // Generate UI Fonts.
        int[] uiSizes = {15, 16, 17, 18, 19, 20, 21, 22, 23, 25, 26, 27, 28, 29, 30, 33};
        for (int size : uiSizes) {
            params.size = size;
            skin.add("ui-" + size, uiGen.generateFont(params));
        }

        // Generate Title Fonts.
        // Use Ethnocentric only for size 60.
        int[] titleSizes = {17, 20, 21, 23, 25, 27, 30, 40, 60};
        for (int size : titleSizes) {
            params.size = size;
            skin.add("title-" + size, size < 60 ? titleGen.generateFont(params) : titleBigGen.generateFont(params));
        }

        // Generate mono fonts.
        params.mono = true;
        params.kerning = false;
        params.size = 22; // default mono
        skin.add("mono", monoGen.generateFont(params));
        params.size = 32; // mono-big
        skin.add("mono-big", monoGen.generateFont(params));

        logger.info("Font generation for [" + lang + "] took " + (System.currentTimeMillis() - start) + "ms");
    }

    public void dispose() {
        if (uiGen != null) {
            uiGen.dispose();
        }
        if (monoGen != null) {
            monoGen.dispose();
        }
        if (titleGen != null) {
            titleGen.dispose();
        }
        if (titleBigGen != null) {
            titleBigGen.dispose();
        }
    }
}