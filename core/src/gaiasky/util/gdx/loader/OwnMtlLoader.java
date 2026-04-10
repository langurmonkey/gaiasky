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
        Color metcolor = null;
        Color roughcolor = null;
        float ior = -1f;
        float metallic = -1f;
        float roughness = -1f;
        float opacity = 1.0f;
        float shininess = -1f;
        String texDiffuseFilename = null;
        String texEmissiveFilename = null;
        String texNormalFilename = null;
        String texSpecularFilename = null;
        String texRoughnessFilename = null;
        String texMetallicFilename = null;

        if (file == null || !file.exists()) {
            logger.error("ERROR: Material file not found: " + (file == null ? "Null file!" : file.name()));
            return;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(file.read()), 4096);
        try {
            while ((line = reader.readLine()) != null) {

                if (!line.isEmpty() && line.charAt(0) == '\t')
                    line = line.substring(1).trim();

                tokens = line.split("\\s+");

                if (tokens[0].isEmpty() || tokens[0].charAt(0) == '#') {
                    // Skip.
                } else {
                    final String key = tokens[0].toLowerCase(Locale.ROOT);
                    switch (key) {
                        case "newmtl" -> {
                            addCurrentMat(curMatName,
                                          difcolor,
                                          speccolor,
                                          emicolor,
                                          metcolor,
                                          roughcolor,
                                          metallic,
                                          roughness,
                                          ior,
                                          opacity,
                                          shininess,
                                          texDiffuseFilename,
                                          texEmissiveFilename,
                                          texNormalFilename,
                                          texSpecularFilename,
                                          texRoughnessFilename,
                                          texMetallicFilename,
                                          materials);
                            if (tokens.length > 1) {
                                curMatName = tokens[1];
                                curMatName = curMatName.replace('.', '_');
                            } else {
                                curMatName = "default";
                            }
                            difcolor = Color.WHITE;
                            speccolor = Color.WHITE;
                            emicolor = Color.WHITE;
                            metcolor = null;
                            roughcolor = null;
                            texDiffuseFilename = null;
                            texEmissiveFilename = null;
                            texNormalFilename = null;
                            texRoughnessFilename = null;
                            texMetallicFilename = null;
                            ior = -1f;
                            roughness = -1f;
                            metallic = -1f;
                            opacity = 1.f;
                            shininess = -1f;
                        }
                        case "kd", "ks", "ke", "km", "kr" -> {
                            // Colors: diffuse, specular, emissive, metallic, roughness

                            float r = Float.parseFloat(tokens[1]);
                            float g = Float.parseFloat(tokens[2]);
                            float b = Float.parseFloat(tokens[3]);
                            float a = tokens.length > 4 ? Float.parseFloat(tokens[4]) : 1f;
                            switch (key) {
                                case "kd" -> {
                                    difcolor = new Color();
                                    difcolor.set(r, g, b, a);
                                }
                                case "ks" -> {
                                    speccolor = new Color();
                                    speccolor.set(r, g, b, a);
                                }
                                case "ke" -> {
                                    emicolor = new Color();
                                    emicolor.set(r, g, b, a);
                                }
                                case "km" -> {
                                    metcolor = new Color();
                                    metcolor.set(r, g, b, a);
                                }
                                case "kr" -> {
                                    roughcolor = new Color();
                                    roughcolor.set(r, g, b, a);
                                }
                            }
                        }
                        case "pr" -> roughness = Float.parseFloat(tokens[1]);
                        case "pm" -> metallic = Float.parseFloat(tokens[1]);

                        case "d" -> opacity = Float.parseFloat(tokens[1]);
                        case "tr" -> opacity = 1f - Float.parseFloat(tokens[1]);
                        case "ni" -> ior = Float.parseFloat(tokens[1]);
                        case "ns" -> shininess = MathUtils.clamp(Float.parseFloat(tokens[1]), 0, 1);
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
            logger.error(e);
            return;
        }

        // last material
        addCurrentMat(curMatName,
                      difcolor,
                      speccolor,
                      emicolor,
                      metcolor,
                      roughcolor,
                      metallic,
                      roughness,
                      ior,
                      opacity,
                      shininess,
                      texDiffuseFilename,
                      texEmissiveFilename,
                      texNormalFilename,
                      texSpecularFilename,
                      texRoughnessFilename,
                      texMetallicFilename,
                      materials);
    }

    private void addCurrentMat(String curMatName,
                               Color difcolor,
                               Color speccolor,
                               Color emicolor,
                               Color metcolor,
                               Color roughcolor,
                               float metallic,
                               float roughness,
                               float ior,
                               float opacity,
                               float shininess,
                               String texDiffuseFilename,
                               String texEmissiveFilename,
                               String texNormalFilename,
                               String texSpecularFilename,
                               String texRoughnessFilename,
                               String texMetallicFilename,
                               Array<OwnModelMaterial> materials) {
        OwnModelMaterial mat = new OwnModelMaterial();
        mat.id = curMatName;
        mat.diffuseColor = new Color(difcolor);
        if (!ColorUtils.isZero(speccolor))
            mat.specularColor = new Color(speccolor);
        if (!ColorUtils.isZero(emicolor))
            mat.emissiveColor = new Color(emicolor);
        if (metcolor != null)
            mat.metallicColor = new Color(metcolor);
        if (roughcolor != null)
            mat.roughnessColor = new Color(roughcolor);
        mat.opacity = opacity;
        mat.roughness = roughness;
        mat.metallic = metallic;
        mat.ior = ior;
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
        mat.diffuseColor = new Color(Color.WHITE);
        materials.add(mat);
        return mat;
    }
}
