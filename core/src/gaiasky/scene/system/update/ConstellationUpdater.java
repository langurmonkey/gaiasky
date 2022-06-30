package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.GaiaSky;
import gaiasky.scene.Mapper;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Constants;
import gaiasky.util.Nature;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.math.Vector3d;
import gaiasky.util.tree.IPosition;

public class ConstellationUpdater extends AbstractUpdateSystem {
    private final Vector3d D31;

    public ConstellationUpdater(Family family, int priority) {
        super(family, priority);

        D31= new Vector3d();
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        ICamera camera = GaiaSky.instance.getICamera();
        var constel = Mapper.constel.get(entity);
        var body = Mapper.body.get(entity);

        constel.posd.setZero();
        Vector3d p = D31;
        int nStars = 0;
        for (IPosition[] line : constel.lines) {
            if (line != null) {
                p.set(line[0].getPosition()).add(camera.getInversePos());
                constel.posd.add(p);
                nStars++;
            }
        }
        if (nStars > 0) {
            constel.posd.scl(1d / nStars);
            constel.posd.nor().scl(100d * Constants.PC_TO_U);
            body.pos.set(constel.posd);

            constel.deltaYears = AstroUtils.getMsSince(GaiaSky.instance.time.getTime(), AstroUtils.JD_J2015_5) * Nature.MS_TO_Y;
        }

    }

}
