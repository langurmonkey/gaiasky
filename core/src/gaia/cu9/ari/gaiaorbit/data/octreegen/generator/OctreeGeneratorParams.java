package gaia.cu9.ari.gaiaorbit.data.octreegen.generator;

public class OctreeGeneratorParams {

    public int maxPart;
    public boolean sunCentre;
    public double maxDistanceCap = 1e6;
    public boolean postprocess = false;
    public long childCount = 100;
    public long parentCount = 1000;

    public OctreeGeneratorParams(int maxPart, boolean sunCentre, boolean postprocess, long childCount, long parentCount) {
        super();
        this.maxPart = maxPart;
        this.sunCentre = sunCentre;
        this.postprocess = postprocess;
        this.childCount = childCount;
        this.parentCount = parentCount;
    }
    public OctreeGeneratorParams(int maxPart, boolean sunCentre) {
        super();
        this.maxPart = maxPart;
        this.sunCentre = sunCentre;
    }

    public OctreeGeneratorParams(boolean sunCentre) {
        super();
        this.sunCentre = sunCentre;
    }

}
