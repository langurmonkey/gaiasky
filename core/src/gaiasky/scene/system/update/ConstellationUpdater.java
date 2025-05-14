/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.GaiaSky;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.util.Constants;
import gaiasky.util.Nature;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.math.Vector3D;
import gaiasky.util.tree.IPosition;

public class ConstellationUpdater extends AbstractUpdateSystem {
    private final Vector3D D31;

    public ConstellationUpdater(Family family, int priority) {
        super(family, priority);

        D31 = new Vector3D();
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
        var label = Mapper.label.get(entity);

        constel.posd.setZero();
        Vector3D p = D31;
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
            label.labelPosition.set(body.pos).add(camera.getPos());

            constel.deltaYears = AstroUtils.getMsSince(GaiaSky.instance.time.getTime(), AstroUtils.JD_J2015_5) * Nature.MS_TO_Y;
        }

    }

}
