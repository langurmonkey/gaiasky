package gaiasky.render.system;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect;
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader;
import com.badlogic.gdx.graphics.g3d.particles.ParticleSystem;
import com.badlogic.gdx.graphics.g3d.particles.batches.PointSpriteParticleBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import gaiasky.GaiaSky;
import gaiasky.render.IRenderable;
import gaiasky.render.RenderingContext;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Constants;

public class ParticleSystemRenderSystem extends AbstractRenderSystem {
    private ParticleSystem particleSystem;
    private PointSpriteParticleBatch pointSpriteParticleBatch;
    private ParticleEffect explosion, explosionInstance;

    private ModelBatch modelBatch;
    private Matrix4 trf;

    public ParticleSystemRenderSystem() {
        super(null, null, null);
        modelBatch = new ModelBatch();
        particleSystem = new ParticleSystem();
        // Since our particle effects are PointSprites, we create a PointSpriteParticleBatch
        pointSpriteParticleBatch = new PointSpriteParticleBatch();
        particleSystem.add(pointSpriteParticleBatch);

        ParticleEffectLoader pel = new ParticleEffectLoader(new InternalFileHandleResolver());
        ParticleEffectLoader.ParticleEffectLoadParameter loadParam = new ParticleEffectLoader.ParticleEffectLoadParameter(particleSystem.getBatches());
        String explosionStr = "assets/effects/explosion.pfx";
        pel.getDependencies(explosionStr, Gdx.files.internal(explosionStr), loadParam);
        explosion = pel.loadSync(GaiaSky.instance.assetManager, explosionStr, Gdx.files.internal(explosionStr), loadParam);
        trf = new Matrix4();
    }

    public void render(Array<IRenderable> renderables, ICamera camera, double t, RenderingContext rc) {
        renderStud(null, camera, t);
    }

    @Override
    public void renderStud(Array<IRenderable> renderables, ICamera camera, double t) {
        pointSpriteParticleBatch.setCamera(camera.getCamera());

        if (explosionInstance == null) {
            explosionInstance = explosion.copy();
            explosionInstance.init();
            explosionInstance.start();  // optional: particle will begin playing immediately
            particleSystem.add(explosionInstance);
        }
        Vector3 pos = new Vector3((float) Constants.AU_TO_U, (float) 0, (float) Constants.AU_TO_U);
        pos.sub(camera.getPos().put(new Vector3()));
        trf.idt().translate(pos);
        modelBatch.begin(camera.getCamera());
        explosionInstance.setTransform(trf);
        particleSystem.update(); // technically not necessary for rendering
        particleSystem.begin();
        particleSystem.draw();
        particleSystem.end();
        modelBatch.render(particleSystem);
        modelBatch.end();
    }
}
