/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Array;
import gaiasky.render.postprocess.PostProcessorEffect;
import gaiasky.render.postprocess.filters.CubemapProjectionsFilter;
import gaiasky.render.util.GaiaSkyFrameBuffer;
import gaiasky.util.LocalizedEnum;
import gaiasky.util.i18n.I18n;

import java.util.Locale;
import java.util.function.Function;

public final class CubmeapProjectionEffect extends PostProcessorEffect {
    private final CubemapProjectionsFilter filter;

    public CubmeapProjectionEffect(float w,
                                   float h) {
        filter = new CubemapProjectionsFilter(w, h);
        disposables.add(filter);
    }

    @Override
    public void rebind() {
        filter.rebind();
    }

    public void setSides(FrameBuffer xpositive,
                         FrameBuffer xnegative,
                         FrameBuffer ypositive,
                         FrameBuffer ynegative,
                         FrameBuffer zpositive,
                         FrameBuffer znegative) {
        filter.setSides(xpositive, xnegative, ypositive, ynegative, zpositive, znegative);
    }

    public void setViewportSize(float w,
                                float h) {
        filter.setViewportSize(w, h);
    }

    public float getPlanetariumAngle() {
        return filter.getPlanetariumAngle();
    }

    public void setPlanetariumAngle(float angle) {
        filter.setPlanetariumAngle(angle);
    }

    public float getPlanetariumAperture() {
        return filter.getPlanetariumAperture();
    }

    public void setPlanetariumAperture(float ap) {
        filter.setPlanetariumAperture(ap);
    }

    public float getCelestialSphereIndexOfRefraction() {
        return filter.getCelestialSphereIndexOfRefraction();
    }

    public void setCelestialSphereIndexOfRefraction(float ior) {
        filter.setCelestialSphereIndexOfRefraction(ior);
    }

    @Override
    public void render(FrameBuffer src,
                       FrameBuffer dest,
                       GaiaSkyFrameBuffer full,
                       GaiaSkyFrameBuffer half) {
        restoreViewport(dest);
        filter.setInput(src).setOutput(dest).render();
    }

    public CubemapProjection getProjection() {
        return filter.getProjection();
    }

    public void setProjection(CubemapProjection projection) {
        filter.setProjection(projection);
    }

    public enum CubemapProjection implements LocalizedEnum {
        // Common panorama modes
        EQUIRECTANGULAR,
        CYLINDRICAL,
        HAMMER,
        ORTHOGRAPHIC,

        // Orthosphere modes
        ORTHOSPHERE,
        ORTHOSPHERE_CROSSEYE,

        // Planetarium: Domemaster and spherical mirror
        AZIMUTHAL_EQUIDISTANT,
        SPHERICAL_MIRROR;

        public boolean isPlanetarium() {
            return isAzimuthalEquidistant() || isSphericalMirror();
        }

        public boolean isAzimuthalEquidistant() {
            return this.equals(AZIMUTHAL_EQUIDISTANT);
        }

        public boolean isSphericalMirror() {
            return this.equals(SPHERICAL_MIRROR);
        }

        public boolean isOrthosphere() {
            return this.equals(ORTHOSPHERE) || this.equals(ORTHOSPHERE_CROSSEYE);
        }

        public boolean isPanorama() {
            return !isPlanetarium() && !isOrthosphere();
        }

        public CubemapProjection getNextPlanetariumProjection() {
            return getNext(CubemapProjection::isPlanetarium);
        }

        public CubemapProjection getNextPanoramaProjection() {
            return getNext(CubemapProjection::isPanorama);
        }

        public CubemapProjection getNextOrthosphereProfile() {
            return getNext(CubemapProjection::isOrthosphere);
        }

        public CubemapProjection getNext(Function<CubemapProjection, Boolean> supplier) {
            int n = CubemapProjection.values().length;
            int i = this.ordinal();
            while (true) {
                i = (i + 1) % n;
                var proj = CubemapProjection.values()[i];
                if (supplier.apply(proj)) {
                    return proj;
                }
            }
        }

        public static Array<CubemapProjection> getPanoramaProjections() {
            Array<CubemapProjection> result = new Array<>();
            var values = CubemapProjection.values();
            for (var value : values) {
                if (value.isPanorama()) {
                    result.add(value);
                }
            }
            return result;
        }

        public static Array<CubemapProjection> getPlanetariumProjections() {
            Array<CubemapProjection> result = new Array<>();
            var values = CubemapProjection.values();
            for (var value : values) {
                if (value.isPlanetarium()) {
                    result.add(value);
                }
            }
            return result;
        }

        public static Array<CubemapProjection> getOrthosphereProjections() {
            Array<CubemapProjection> result = new Array<>();
            var values = CubemapProjection.values();
            for (var value : values) {
                if (value.isOrthosphere()) {
                    result.add(value);
                }
            }
            return result;
        }

        public String localizedName() {
            return I18n.get("gui.cubemap.projection." + name().toLowerCase(Locale.ROOT));
        }
    }

}
