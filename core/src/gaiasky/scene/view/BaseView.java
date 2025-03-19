/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.view;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import gaiasky.GaiaSky;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Base;
import gaiasky.scene.component.Body;
import gaiasky.util.Settings;

public non-sealed class BaseView extends AbstractView {

    /** The base component. **/
    public Base base;
    /** The body component. **/
    public Body body;

    public BaseView() {
    }

    public BaseView(Entity entity) {
        super(entity);
    }

    @Override
    protected boolean componentsCheck(Entity entity) {
        return entity != null && Mapper.base.get(entity) == base && Mapper.body.get(entity) == body;
    }

    @Override
    protected void entityCheck(Entity entity) {
        check(entity, Mapper.base, Base.class);
        check(entity, Mapper.body, Body.class);
    }

    @Override
    protected void entityChanged() {
        this.base = Mapper.base.get(entity);
        this.body = Mapper.body.get(entity);
    }

    @Override
    protected void entityCleared() {
        this.base = null;
        this.body = null;
    }

    public void setColor(float[] color) {
        body.setColor(color);
    }

    public <T extends Component> T getComponent(Class<T> c) {
        return entity.getComponent(c);
    }

    public float getOpacity() {
        assert base != null;
        return base.opacity;
    }

    public ComponentTypes getComponentType() {
        return base.ct;
    }
    public Base getBase() {
        return base;
    }

    public Body getBody() {
        return body;
    }

    public boolean isVisible() {
        return base.visible || base.msSinceStateChange() <= Settings.settings.scene.fadeMs;
    }

    public void setVisible(boolean visible) {
        base.visible = visible;
        base.lastStateChangeTimeMs = (long) (GaiaSky.instance.getT() * 1000f);
    }

    public void setVisible(boolean visible, String name) {
        setVisible(visible);
    }

    public boolean isVisible(boolean attributeValue) {
        if (attributeValue)
            return base.visible;
        else
            return this.isVisible();
    }

    public boolean hasCt(ComponentType ct) {
        return ct != null && base.ct.isEnabled(ct);
    }

}
