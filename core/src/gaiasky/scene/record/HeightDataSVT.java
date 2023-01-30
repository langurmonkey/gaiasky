package gaiasky.scene.record;

import gaiasky.util.svt.SVTQuadtree;

import java.nio.file.Path;

public class HeightDataSVT implements IHeightData {

    private SVTQuadtree<Path> svt;

    @Override
    public double getNormalizedHeight(double u, double v) {
        return 0;
    }
}
