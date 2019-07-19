/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.noise;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class NoiseUtils {

    private static Vector2 floor(Vector2 a){
        return new Vector2((float) Math.floor(a.x), (float) Math.floor(a.y));
    }
    private static Vector3 floor(Vector3 a){
        return new Vector3((float) Math.floor(a.x), (float) Math.floor(a.y), (float) Math.floor(a.z));
    }

    private static Vector2 fract(Vector2 a){
        return new Vector2((float)(a.x - Math.floor(a.x)), (float) (a.y - Math.floor(a.y)));
    }

    private static Vector3 fract(Vector3 a){
        return new Vector3((float)(a.x - Math.floor(a.x)), (float) (a.y - Math.floor(a.y)), (float)(a.z - Math.floor(a.z)));
    }

    private static Vector2 mod(Vector2 a, float n){
        return new Vector2(a.x % n, a.y % n);
    }

    private static Vector3 mod(Vector3 a, float n){
        return new Vector3(a.x % n, a.y % n, a.z % n);
    }

    private static Vector2 add(Vector2 a, Vector2 b){
        return new Vector2(a).add(b);
    }

    private static Vector3 add(Vector3 a, Vector3 b){
        return new Vector3(a).add(b);
    }

    private static Vector2 sub(Vector2 a, Vector2 b){
        return new Vector2(a).sub(b);
    }

    private static Vector3 sub(Vector3 a, Vector3 b){
        return new Vector3(a).sub(b);
    }

    private static Vector3 scl(Vector3 a, float n){
        return new Vector3(a).scl(n);
    }

    // Modulo 289, optimizes to code without divisions
    public static Vector3 mod289(Vector3 x) {
        return sub(x, floor(scl(x, (1.0f / 289.0f))) * 289.0f);
    }
    float mod289(float x) {
        return x - (float) Math.floor(x * (1.0f / 289.0f)) * 289.0f;
    }

    // Permutation polynomial (ring size 289 = 17*17)
    vec3 permute(vec3 x) {
        return mod289(((x*34.0)+1.0)*x);
    }
    float permute(float x) {
        return mod289(((x*34.0)+1.0)*x);
    }
    // Hashed 2-D gradients with an extra rotation.
    // (The constant 0.0243902439 is 1/41)
    public static Vector2 rgrad2(Vector2 p, float rot) {
        // Map from a line to a diamond such that a shift maps to a rotation.
        float u = permute(permute(p.x) + p.y) * 0.0243902439 + rot; // Rotate by shift
        u = 4.0 * fract(u) - 2.0;
        // (This vector could be normalized, exactly or approximately.)
        return vec2(abs(u) - 1.0, abs(abs(u + 1.0) - 2.0) - 1.0);
    }
    //
    // 2-D tiling simplex noise with rotating gradients,
    // but without the analytical derivative.
    //
    public static float psrnoise(Vector2 pos, Vector2 per, float rot) {
        // Offset y slightly to hide some rare artifacts
        pos.y += 0.001;
        // Skew to hexagonal grid
        Vector2 uv = new Vector2(pos.x + pos.y*0.5f, pos.y);

        Vector2 i0 = floor(uv);
        Vector2 f0 = fract(uv);
        // Traversal order
        Vector2 i1 = (f0.x > f0.y) ? new Vector2(1.0f, 0.0f) : new Vector2(0.0f, 1.0f);

        // Unskewed grid points in (x,y) space
        Vector2 p0 = new Vector2(i0.x - i0.y * 0.5f, i0.y);
        Vector2 p1 = new Vector2(p0.x + i1.x - i1.y * 0.5f, p0.y + i1.y);
        Vector2 p2 = new Vector2(p0.x + 0.5f, p0.y + 1.0f);

        // Integer grid point indices in (u,v) space
        i1 = add(i0, i1);
        Vector2 i2 = add(i0, new Vector2(1.0f, 1.0f));

        // Vectors in unskewed (x,y) coordinates from
        // each of the simplex corners to the evaluation point
        Vector2 d0 = sub(pos, p0);
        Vector2 d1 = sub(pos, p1);
        Vector2 d2 = sub(pos, p2);

        // Wrap i0, i1 and i2 to the desired period before gradient hashing:
        // wrap points in (x,y), map to (u,v)
        Vector3 xw = mod(new Vector3(p0.x, p1.x, p2.x), per.x);
        Vector3 yw = mod(new Vector3(p0.y, p1.y, p2.y), per.y);
        Vector3 iuw = add(xw, yw.scl(0.5f));
        Vector3 ivw = yw;

        // Create gradients from indices
        Vector2 g0 = rgrad2(Vector2(iuw.x, ivw.x), rot);
        Vector2 g1 = rgrad2(Vector2(iuw.y, ivw.y), rot);
        Vector2 g2 = rgrad2(Vector2(iuw.z, ivw.z), rot);

        // Gradients dot vectors to corresponding corners
        // (The derivatives of this are simply the gradients)
        Vector3 w = Vector3(dot(g0, d0), dot(g1, d1), dot(g2, d2));

        // Radial weights from corners
        // 0.8 is the square of 2/sqrt(5), the distance from
        // a grid point to the nearest simplex boundary
        Vector3 t = 0.8 - Vector3(dot(d0, d0), dot(d1, d1), dot(d2, d2));

        // Set influence of each surflet to zero outside radius sqrt(0.8)
        t = max(t, 0.0);

        // Fourth power of t
        Vector3 t2 = t * t;
        Vector3 t4 = t2 * t2;

        // Final noise value is:
        // sum of ((radial weights) times (gradient dot vector from corner))
        float n = dot(t4, w);

        // Rescale to cover the range [-1,1] reasonably well
        return 11.0*n;
    }

    //
    // 2-D tiling simplex noise with fixed gradients,
    // without the analytical derivative.
    // This function is implemented as a wrapper to "psrnoise",
    // at the minimal cost of three extra additions.
    //
    float psnoise(Vector2 pos, Vector2 per) {
        return psrnoise(pos, per, 0.0);
    }
}
