package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class Perimeter implements Component {

    public float[][][] loc2d, loc3d;

    /** Max latitude/longitude and min latitude/longitude **/
    public Vector2 maxlonlat;
    public Vector2 minlonlat;
    /** Cartesian points corresponding to maximum lonlat and minimum lonlat **/
    public Vector3 cart0;

    public void setPerimeter(double[][][] perimeter) {
        this.maxlonlat = new Vector2();
        this.minlonlat = new Vector2();
        this.loc2d = new float[perimeter.length][][];
        for (int i = 0; i < perimeter.length; i++) {
            float[][] arr = new float[perimeter[i].length][];
            for (int j = 0; j < perimeter[i].length; j++) {
                arr[j] = new float[2];
                arr[j][0] = (float) perimeter[i][j][0];
                arr[j][1] = (float) perimeter[i][j][1];

                // Longitude
                if (arr[j][0] > maxlonlat.x) {
                    maxlonlat.x = arr[j][0];
                }

                if (arr[j][0] < minlonlat.x) {
                    minlonlat.x = arr[j][0];
                }

                // Latitude
                if (arr[j][1] > maxlonlat.y) {
                    maxlonlat.y = arr[j][1];
                }

                if (arr[j][1] < minlonlat.y) {
                    minlonlat.y = arr[j][1];
                }

            }
            this.loc2d[i] = arr;
        }
    }
}
