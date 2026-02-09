/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */
package gaiasky.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.util.Logger.Log;

/**
 * Helper to generate and inject FreeType fonts into the Gaia Sky skin.
 */
public class FontFactory {
    private static final Log logger = Logger.getLogger(FontFactory.class);

    /** Western languages character set (Spanish, Catala, German, French, Italian, Slovenian, Turkish, Russian, Bulgarian). **/
    public static final String COMMON_CHARS = """
            AÀÁÄÂBCDEÈÉËÊFGĞHIÌÍÏÎİJKLMNOÒÓÔÖPQRSŞTUÙÚÜÛVWXYZÑÇ
            aàáäâbcdeèéêëfgğhiìíîïıjȷklmnoòóôöpqrsştuùúûüvwxyzñç
            1234567890!`?'.,;:()[]{}<>|/@\\^$-%+=#_&~*
            θωερτψυιοπασδφγηςκλζχξωβνμΘΩΕΡΤΨΥΙΟΠΑΣΔΦΓΗςΚΛΖΧΞΩΒΝΜ
            АаБбВвГгДдЕеЁёЖжЗзИиЙйКкЛлМмНнОоПпРрСсТтУуФфХхЦцЧчШшЩщЪъЫыЬьЭэЮюЯя
            ±∓×·÷√∑∫∮¬∝∞≠≈⇔↔≪≫≤≥°ℏÅ⊙☉☼☀¶ß©®£€¼½¾²³čžšČŽŠ„“"
            """;

    /** Common 3500 simplified Chinese characters. **/
    public static final String CHINESE_CHARS = loadChineseChars();
    private static String loadChineseChars() {
        try {
            return Gdx.files.internal("fonts/sc-chars.txt").readString("utf-8");
        } catch (Exception e) {
            // Fallback to a very basic set if file is missing
            return """
                    的一是在不了有和人这中大为上个国我以要他时来用们生到作地于出就
                    分对成会可主发年样能下过子说产种面而方最后多分业意宣由本事其里所去
                    前用后方种形想看起得定法通性都题那现把事此物则明实各自本形理道才
                    外正其公向情者意果其见之问第此由也机由
                    """;
        }
    }

    public static void generateFonts(Skin skin, String lang) {
        long start = System.currentTimeMillis();
        boolean isChinese = lang.startsWith("zh");

        // Prepare Generators
        String uiFontPath = isChinese ? "fonts/NotoSansSC-Regular.ttf" : "fonts/InterDisplay-Regular.ttf";
        String monoFontPath = isChinese ? "fonts/NotoSansSC-Regular.ttf" : "fonts/LiberationMono-Bold.ttf";
        String titleFontPath = "fonts/InterDisplay-Bold.ttf";
        String titleBigFontPath = "fonts/Ethnocentric-Regular.ttf";

        FreeTypeFontGenerator uiGen = new FreeTypeFontGenerator(Gdx.files.internal(uiFontPath));
        FreeTypeFontGenerator monoGen = new FreeTypeFontGenerator(Gdx.files.internal(monoFontPath));
        FreeTypeFontGenerator titleGen = new FreeTypeFontGenerator(Gdx.files.internal(titleFontPath));
        FreeTypeFontGenerator titleBigGen = new FreeTypeFontGenerator(Gdx.files.internal(titleBigFontPath));

        FreeTypeFontParameter params = new FreeTypeFontParameter();
        params.hinting = FreeTypeFontGenerator.Hinting.AutoMedium;
        params.magFilter = TextureFilter.Linear;
        params.minFilter = TextureFilter.Linear;
        params.incremental = isChinese;

        // Add Chinese characters to the generation set if needed
        params.characters = COMMON_CHARS + (isChinese ? CHINESE_CHARS : "");

        // Generate UI Fonts
        int[] uiSizes = {15, 16, 17, 18, 19, 20, 21, 22, 23, 25, 26, 27, 28, 29, 30, 33};
        for (int size : uiSizes) {
            params.size = size;
            skin.add("ui-" + size, uiGen.generateFont(params));
        }

        // Generate Title Fonts
        // Only use Ethnocentric for non-Chinese. For Chinese, use bold UI font.
        int[] titleSizes = {17, 20, 21, 23, 25, 27, 30, 40, 60};
        if (isChinese) {
            // Re-use UI generator for titles in Chinese
            for (int size : titleSizes) {
                params.size = size;
                skin.add("title-" + size, uiGen.generateFont(params));
            }
        } else {
            for (int size : titleSizes) {
                params.size = size;
                skin.add("title-" + size, size < 60 ? titleGen.generateFont(params) : titleBigGen.generateFont(params));
            }
        }

        // Generate mono fonts
        params.mono = true;
        params.size = 22; // default mono
        skin.add("mono", monoGen.generateFont(params));
        params.size = 32; // mono-big
        skin.add("mono-big", monoGen.generateFont(params));

        // Generate distance field Font
        FreeTypeFontParameter sdfParams = new FreeTypeFontParameter();
        // The key settings for Distance Field
        if (isChinese) {
            sdfParams.size = 32; // Base size for the SDF generation
            sdfParams.renderCount = 3; // Quality of the distance field
            skin.add("font-distance-field", uiGen.generateFont(sdfParams));
        } else {
            // For English/Latin, load your existing static SDF font
            skin.add("font-distance-field", new BitmapFont(Gdx.files.internal("skins/fonts/font-distance-field.fnt")));
        }

        // Cleanup
        if (!params.incremental) {
            uiGen.dispose();
            monoGen.dispose();
            titleGen.dispose();
        }

        logger.info("Font generation for [" + lang + "] took " + (System.currentTimeMillis() - start) + "ms");
    }
}