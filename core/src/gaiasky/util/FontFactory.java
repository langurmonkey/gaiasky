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

    private FreeTypeFontGenerator regularGen, monoGen, boldGen, titleGen, interGen, notoGen;

    /** Western languages character set (Spanish, Catala, German, French, Italian, Slovenian, Turkish, Russian, Bulgarian). **/
    public static final String COMMON_CHARS = """
            AÀÁÄÂBCDEÈÉËÊFGĞHIÌÍÏÎİJKLMNOÒÓÔÖPQRSŞTUÙÚÜÛVWXYZÑÇ
            aàáäâbcdeèéêëfgğhiìíîïıjȷklmnoòóôöpqrsştuùúûüvwxyzñç
            1234567890!`?'.,;:()[]{}<>|/@\\^$-%+=#_&~*
            θωερτψυιοπασδφγηςκλζχξωβνμΘΩΕΡΤΨΥΙΟΠΑΣΔΦΓΗςΚΛΖΧΞΩΒΝΜ
            АаБбВвГгДдЕеЁёЖжЗзИиЙйКкЛлМмНнОоПпРрСсТтУуФфХхЦцЧчШшЩщЪъЫыЬьЭэЮюЯя
            ±∓×·÷√∑∫∮¬∝∞≠≈⇔↔≪≫≤≥°ℏÅ⊙☉☼☀¶ß©®£€¼½¾²³čžšČŽŠ„“"←↑→↓↔↕↖↗↘↙⇐⇑⇒⇓⇔⇕⬅⬆➡⬇◀▲▶▼↩↪↺↻
            """;

    public void generateFonts(Skin skin, String lang) {
        // Dispose old generators.
        dispose();

        // Timing.
        long start = System.currentTimeMillis();

        // Prepare Generators.
        String regularFontPath = "fonts/SarasaUiSC-Regular-Subset.ttf";
        String boldFontPath = "fonts/SarasaUiSC-Bold-Subset.ttf";
        String titleFontPath = "fonts/Ethnocentric-Regular.ttf";

        boldGen = new FreeTypeFontGenerator(Gdx.files.internal(boldFontPath));
        titleGen = new FreeTypeFontGenerator(Gdx.files.internal(titleFontPath));
        regularGen = new FreeTypeFontGenerator(Gdx.files.internal(regularFontPath));

        // Generate regular Fonts.
        int[] regularSizes = {15, 16, 17, 18, 19, 20, 21, 22, 23, 25, 26, 27, 28, 29, 30, 33};
        for (int size : regularSizes) {
            skin.add("regular-" + size, regularGen.generateFont(fontParams(size)));
        }

        // Generate bold Fonts.
        int[] boldSizes = {17, 20, 21, 23, 25, 27, 30, 40};
        for (int size : boldSizes) {
            skin.add("bold-" + size,boldGen.generateFont(fontParams(size)));
        }

        // Generate title fonts
        int[] titleSizes = {30, 60};
        for (int size : titleSizes) {
            skin.add("title-" + size, titleGen.generateFont(fontParams(size)));
        }

        logger.info("Font generation for [" + lang + "] took " + (System.currentTimeMillis() - start) + "ms");
    }

    /**
     * Gets the font parameters instance for the given size. It is important that the parameters
     * are recreated for each size because we use {@link FreeTypeFontParameter#incremental}.
     * @param size The font size.
     * @return The parameters instance.
     */
    private FreeTypeFontParameter fontParams(int size) {
        FreeTypeFontParameter params = new FreeTypeFontParameter();
        params.mono = false;
        params.hinting = FreeTypeFontGenerator.Hinting.Slight;
        params.magFilter = TextureFilter.Linear;
        params.minFilter = TextureFilter.Linear;
        // Common characters.
        params.characters = COMMON_CHARS;
        // Chinese characters are loaded on-demand (incremental).
        params.incremental = true;
        FreeTypeFontGenerator.setMaxTextureSize(1024);
        params.size = size;
        return params;
    }

    public void dispose() {
        if (regularGen != null) {
            regularGen.dispose();
        }
        if (boldGen != null) {
            boldGen.dispose();
        }
        if (titleGen != null) {
            titleGen.dispose();
        }
    }
}