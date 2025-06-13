/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.loader;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.gdx.model.data.OwnModelMaterial;
import gaiasky.util.gdx.model.data.OwnModelTexture;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

public class OwnMtlLoader {
    private static final Log logger = Logger.getLogger(OwnMtlLoader.class);

    public Array<OwnModelMaterial> materials = new Array<>();

    /**
     * loads .mtl file
     */
    public void load(FileHandle file) {
        String line;
        String[] tokens;
        String curMatName = "default";
        Color difcolor = Color.WHITE;
        Color speccolor = Color.WHITE;
        Color emicolor = Color.WHITE;
        Color metcolor = Color.BLACK;
        float opacity = 1.f;
        float shininess = 0.f;
        String texDiffuseFilename = null;
        String texEmissiveFilename = null;
        String texNormalFilename = null;
        String texSpecularFilename = null;
        String texRoughnessFilename = null;
        String texMetallicFilename = null;

        if (file == null || !file.exists()) {
            logger.error("ERROR: Material file not found: " + file.name());
            return;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(file.read()), 4096);
        try {
            while ((line = reader.readLine()) != null) {

                if (!line.isEmpty() && line.charAt(0) == '\t')
                    line = line.substring(1).trim();

                tokens = line.split("\\s+");

                if (tokens[0].isEmpty()) {
                    continue;
                } else if (tokens[0].charAt(0) == '#')
                    continue;
                else {
                    final String key = tokens[0].toLowerCase(Locale.ROOT);
                    switch (key) {
                    case "newmtl" -> {
                        addCurrentMat(curMatName, difcolor, speccolor, emicolor, metcolor, opacity, shininess, texDiffuseFilename, texEmissiveFilename, texNormalFilename, texSpecularFilename, texRoughnessFilename, texMetallicFilename, materials);
                        if (tokens.length > 1) {
                            curMatName = tokens[1];
                            curMatName = curMatName.replace('.', '_');
                        } else {
                            curMatName = "default";
                        }
                        difcolor = Color.WHITE;
                        speccolor = Color.WHITE;
                        emicolor = Color.WHITE;
                        metcolor = Color.BLACK;
                        texDiffuseFilename = null;
                        texEmissiveFilename = null;
                        texNormalFilename = null;
                        texRoughnessFilename = null;
                        texMetallicFilename = null;
                        opacity = 1.f;
                        shininess = 0.f;
                    }
                    case "kd", "ks", "ke", "kr", "km" -> {
                        // diffuse, specular, emissive, metallic

                        float r = Float.parseFloat(tokens[1]);
                        float g = Float.parseFloat(tokens[2]);
                        float b = Float.parseFloat(tokens[3]);
                        float a = 1;
                        if (tokens.length > 4)
                            a = Float.parseFloat(tokens[4]);
                        if (tokens[0].equalsIgnoreCase("kd")) {
                            difcolor = new Color();
                            difcolor.set(r, g, b, a);
                        } else if (tokens[0].equalsIgnoreCase("ks")) {
                            speccolor = new Color();
                            speccolor.set(r, g, b, a);
                        } else if (tokens[0].equalsIgnoreCase("ke")) {
                            emicolor = new Color();
                            emicolor.set(r, g, b, a);
                        } else if (tokens[0].equalsIgnoreCase("kr") || tokens[0].equalsIgnoreCase("km")) {
                            metcolor = new Color();
                            metcolor.set(r, g, b, a);
                        }
                    }
                    case "tr", "d" -> opacity = Float.parseFloat(tokens[1]);
                    case "ns" ->
                        // Shininess, normalize in [0,1]
                            shininess = MathUtils.clamp(Float.parseFloat(tokens[1]), 0, 300) / 300;
                    case "map_kd" -> texDiffuseFilename = file.parent().child(tokens[1]).path();
                    case "map_ke" -> texEmissiveFilename = file.parent().child(tokens[1]).path();
                    case "map_kn", "map_bump", "norm" -> texNormalFilename = file.parent().child(tokens[1]).path();
                    case "map_ks" -> texSpecularFilename = file.parent().child(tokens[1]).path();
                    case "map_kr", "map_km", "map_pm" -> texMetallicFilename = file.parent().child(tokens[1]).path();
                    case "map_pr", "map_ns" -> texRoughnessFilename = file.parent().child(tokens[1]).path();
                    }
                }
            }
            reader.close();
        } catch (IOException e) {
            return;
        }

        // last material
        addCurrentMat(curMatName, difcolor, speccolor, emicolor, metcolor, opacity, shininess, texDiffuseFilename, texEmissiveFilename, texNormalFilename, texSpecularFilename, texRoughnessFilename, texMetallicFilename, materials);
    }

    private void addCurrentMat(String curMatName, Color difcolor, Color speccolor, Color emicolor, Color metcolor, float opacity, float shininess, String texDiffuseFilename, String texEmissiveFilename, String texNormalFilename, String texSpecularFilename, String texRoughnessFilename, String texMetallicFilename, Array<OwnModelMaterial> materials) {
        OwnModelMaterial mat = new OwnModelMaterial();
        mat.id = curMatName;
        mat.diffuse = new Color(difcolor);
        if (!ColorUtils.isZero(speccolor))
            mat.specular = new Color(speccolor);
        if (!ColorUtils.isZero(emicolor))
            mat.emissive = new Color(emicolor);
        if (!ColorUtils.isZero(metcolor))
            mat.metallic = new Color(metcolor);
        mat.opacity = opacity;
        mat.shininess = shininess;
        if (texDiffuseFilename != null) {
            OwnModelTexture tex = new OwnModelTexture();
            tex.usage = OwnModelTexture.USAGE_DIFFUSE;
            tex.fileName = texDiffuseFilename;
            if (mat.textures == null)
                mat.textures = new Array<>(1);
            mat.textures.add(tex);
        }
        if (texEmissiveFilename != null) {
            OwnModelTexture tex = new OwnModelTexture();
            tex.usage = OwnModelTexture.USAGE_EMISSIVE;
            tex.fileName = texEmissiveFilename;
            if (mat.textures == null)
                mat.textures = new Array<>(1);
            mat.textures.add(tex);
        }
        if (texNormalFilename != null) {
            OwnModelTexture tex = new OwnModelTexture();
            tex.usage = OwnModelTexture.USAGE_NORMAL;
            tex.fileName = texNormalFilename;
            if (mat.textures == null)
                mat.textures = new Array<>(1);
            mat.textures.add(tex);
        }
        if (texSpecularFilename != null) {
            OwnModelTexture tex = new OwnModelTexture();
            tex.usage = OwnModelTexture.USAGE_SPECULAR;
            tex.fileName = texSpecularFilename;
            if (mat.textures == null)
                mat.textures = new Array<>(1);
            mat.textures.add(tex);
        }
        if (texRoughnessFilename != null) {
            OwnModelTexture tex = new OwnModelTexture();
            tex.usage = OwnModelTexture.USAGE_ROUGHNESS;
            tex.fileName = texRoughnessFilename;
            if (mat.textures == null)
                mat.textures = new Array<>(1);
            mat.textures.add(tex);
        }
        if (texMetallicFilename != null) {
            OwnModelTexture tex = new OwnModelTexture();
            tex.usage = OwnModelTexture.USAGE_METALLIC;
            tex.fileName = texMetallicFilename;
            if (mat.textures == null)
                mat.textures = new Array<>(1);
            mat.textures.add(tex);
        }
        materials.add(mat);
    }

    public OwnModelMaterial getMaterial(final String name) {
        for (final OwnModelMaterial m : materials)
            if (m.id.equals(name))
                return m;
        OwnModelMaterial mat = new OwnModelMaterial();
        mat.id = name;
        mat.diffuse = new Color(Color.WHITE);
        materials.add(mat);
        return mat;
    }
}
