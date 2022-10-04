/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gravwaves;

import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.scene.camera.ICamera;
import gaiasky.util.Constants;
import gaiasky.util.Settings;
import gaiasky.util.math.ITrigonometry;
import gaiasky.util.math.MathManager;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;

/**
 * Central hub where the parameters of the current relativistic
 * effects (aberration, doppler, grav waves) are updated and served to the renderers.
 */
public class RelativisticEffectsManager implements IObserver {

    private static RelativisticEffectsManager instance;

    public static RelativisticEffectsManager getInstance() {
        return instance;
    }

    public static void initialize(ITimeFrameProvider time) {
        instance = new RelativisticEffectsManager(time);
    }

    /*
     * RELATIVISTIC ABERRATION
     */
    /** Camera velocity direction vector **/
    public Vector3 velDir;
    /** v/c **/
    public float vc;

    /*
     * GRAVITATIONAL WAVES
     */

    /** Cartesian coordinates from the origin of the grav wave. Unit vector **/
    public Vector3 gw;
    /** Rotation of gw **/
    public Matrix3 gwmat3;
    /** Rotation of gw **/
    public Matrix4 gwmat4;
    /** Time in seconds, synced to the simulation time. Ready to pass to the shader **/
    public float gwtime;

    /** Wave frequency **/
    public float omgw;

    /** Hterms: hpluscos, hplussin, htimescos, htimessin **/
    public float[] hterms;

    /** Intial time for the counter **/
    private final long initime;

    /** Unit vector **/
    private final Vector3 unitz;
    private final Vector3 screenCoords;
    /** Aux matrices **/
    private final Matrix3 eplus;
    private final Matrix3 etimes;
    private final Matrix3 auxm1;
    private final Matrix3 auxm2;
    private final Matrix3 auxm3;
    private final Matrix3 auxm4;
    /** Aux vectors **/
    private final Vector3d auxd1;
    private final Vector3d auxd2;
    private final Vector3d auxd3;
    private final Vector3d auxd4;
    private final Vector3d auxd5;

    private RelativisticEffectsManager(ITimeFrameProvider time) {
        super();
        velDir = new Vector3();
        gw = new Vector3(0, 0, 1);
        gwmat3 = new Matrix3();
        gwmat4 = new Matrix4();
        initime = time.getTime().toEpochMilli();
        gwtime = 0f;
        hterms = new float[4];
        unitz = new Vector3(0, 0, 1);

        eplus = new Matrix3(new float[] { 1, 0, 0, 0, -1, 0, 0, 0, 0 });
        etimes = new Matrix3(new float[] { 0, 1, 0, 1, 0, 0, 0, 0, 0 });
        auxm1 = new Matrix3();
        auxm2 = new Matrix3();
        auxm3 = new Matrix3();
        auxm4 = new Matrix3();

        auxd1 = new Vector3d();
        auxd2 = new Vector3d();
        auxd3 = new Vector3d();
        auxd4 = new Vector3d();
        auxd5 = new Vector3d();

        screenCoords = new Vector3();

        EventManager.instance.subscribe(this, Event.GRAV_WAVE_START);
    }

    public boolean relAberrationOn() {
        return Settings.settings.runtime.relativisticAberration;
    }

    public boolean gravWavesOn() {
        return Settings.settings.runtime.gravitationalWaves;
    }

    /**
     * This must be called every cycle, it updates
     * the needed parameters for the gravitational waves
     * @param time
     */
    public void update(ITimeFrameProvider time, ICamera camera) {
        /*
         * RELATIVISTIC ABERRATION
         */
        if (Settings.settings.runtime.relativisticAberration) {
            vc = (float) (camera.getSpeed() / Constants.C_KMH);
            if (camera.getVelocity() == null || camera.getVelocity().len() == 0) {
                velDir.set(1, 0, 0);
            } else {
                camera.getVelocity().put(velDir).nor();
            }
        }

        /*
         * GRAVITATIONAL WAVES
         */
        if (Settings.settings.runtime.gravitationalWaves) {
            // Time
            gwtime = (float) ((time.getTime().toEpochMilli() - initime) / 1000d);

            // Frequency
            omgw = 0.8f;

            // Hterms - hpluscos, hplussin, htimescos, htimessin
            hterms[0] = 0.3f;
            hterms[1] = 0.f;
            hterms[2] = 0.f;
            hterms[3] = 0.f;

            // Rotation matrix
            gwmat4.setToRotation(unitz, gw);
            gwmat3.set(gwmat4);
        }
    }

    /**
     * Applies the gravitational wave transformation to the given position
     * @param pos The position for chaining
     */
    public Vector3d gravitationalWavePos(Vector3d pos) {
        if (Settings.settings.runtime.gravitationalWaves) {
            float hpluscos = hterms[0];
            float hplussin = hterms[1];
            float htimescos = hterms[2];
            float htimessin = hterms[3];
            float t = gwtime;
            ITrigonometry trigo = MathManager.instance.trigonometryInterface;

            Vector3d p = auxd4.set(gw);
            Matrix3 P = gwmat3;
            Matrix3 PT = auxm1.set(gwmat3).transpose();
            // PePlusPt = P * ePlus * transpose(P);
            Matrix3 pepluspt = auxm2.set(P).mul(eplus).mul(PT);
            // PeTimesPt = P * eTimes * transpose(P);
            Matrix3 petimespt = auxm3.set(P).mul(etimes).mul(PT);

            // plusPhase = hpluscos * cos(omgw * t) + hplussin * sin(omgw * t);
            double plusphase = hpluscos * trigo.cos(omgw * t) + hplussin * trigo.sin(omgw * t);
            // timesPhase = htimescos * cos(omgw * t) + htimessin * sin(omgw * t);
            double timesphase = htimescos * trigo.cos(omgw * t) + htimessin * trigo.sin(omgw * t);

            // PePlusPt = PePlusPt * plusPhase;
            pepluspt = mul(pepluspt, (float) plusphase);
            // PeTimesPt = PeTimesPt * timesPhase;
            petimespt = mul(petimespt, (float) timesphase);

            // PePt = PePlusPt + PeTimesPt;
            auxm4.set(pepluspt);
            Matrix3 pept = add(auxm4, petimespt);

            // u = [x; y; z];
            // Backup distance to pos
            double poslen = pos.len();
            // Normalize pos
            pos.nor();

            // vec3 huu = pept * pos * pos;
            // huu = transpose(PePt*u)*u;
            Vector3d huu = auxd1.set(pos).mulRight(pept).mul(pos);
            // vec3 hu = 0.5 * pept * pos;
            // hu = 0.5 * PePt * u;
            Vector3d hu = auxd2.set(pos).mulRight(mul(pept, 0.5f));

            // vec3 deltau = ((pos + p) / (2 * (1 + pos * p))) * huu - hu;
            // deltau = ((u + p) / (2 * (1 + transpose(u)*p))) * huu - hu;
            Vector3d deltau = auxd3.set(pos).add(p);
            deltau.div(auxd5.set(pos).mul(p).add(1).scl(2));
            deltau.mul(huu).sub(hu);

            // Apply shift, compute new position using backup distance
            pos.add(deltau);
            pos.nor().scl(poslen);
        }
        return pos;
    }

    private Matrix3 mul(Matrix3 m, float scl) {
        for (int i = 0; i < m.val.length; i++)
            m.val[i] *= scl;
        return m;
    }

    private Matrix3 add(Matrix3 one, Matrix3 two) {
        for (int i = 0; i < one.val.length; i++) {
            one.val[i] += two.val[i];
        }
        return one;
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        if (event == Event.GRAV_WAVE_START) {
            int x = (Integer) data[0];
            int y = (Integer) data[1];

            ICamera cam = GaiaSky.instance.getICamera();
            screenCoords.set(x, y, 0.9f);
            cam.getCamera().unproject(screenCoords);
            screenCoords.nor();

            this.gw.set(screenCoords);
        }

    }
}
