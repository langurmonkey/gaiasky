/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.render.postprocess.filters.CubemapProjectionsFilter;
import gaiasky.render.postprocess.PostProcessorEffect;
import gaiasky.render.util.GaiaSkyFrameBuffer;

import java.util.function.Function;

public final class CubmeapProjectionEffect extends PostProcessorEffect {
    private final CubemapProjectionsFilter filter;

    public CubmeapProjectionEffect(float w, float h) {
        filter = new CubemapProjectionsFilter(w, h);
        disposables.add(filter);
    }

    @Override
    public void rebind() {
        filter.rebind();
    }

    public void setSides(FrameBuffer xpositive, FrameBuffer xnegative, FrameBuffer ypositive, FrameBuffer ynegative, FrameBuffer zpositive, FrameBuffer znegative) {
        filter.setSides(xpositive, xnegative, ypositive, ynegative, zpositive, znegative);
    }

    public void setViewportSize(float w, float h) {
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
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        filter.setInput(src).setOutput(dest).render();
    }

    public CubemapProjection getProjection() {
        return filter.getProjection();
    }

    public void setProjection(CubemapProjection projection) {
        filter.setProjection(projection);
    }

    public enum CubemapProjection {
        EQUIRECTANGULAR,
        CYLINDRICAL,
        HAMMER,
        ORTHOGRAPHIC,
        ORTHOSPHERE,
        ORTHOSPHERE_CROSSEYE,
        AZIMUTHAL_EQUIDISTANT,
        SPHERICAL_MIRROR;

        public boolean isPlanetarium() {
            return isAzimuthalEquidistant() || isSphericalMirror();
        }

        public boolean isDomeMaster() {
            return isAzimuthalEquidistant();
        }

        public boolean isFisheye() {
            return isAzimuthalEquidistant();
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
            final int n = CubemapProjection.values().length;
            int i = this.ordinal();
            while (true) {
                i = (i + 1) % n;
                var proj = CubemapProjection.values()[i];
                if (supplier.apply(proj)) {
                    return proj;
                }
            }
        }
    }

}
