/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.Disableable;
import gaiasky.event.EventManager;
import gaiasky.event.Event;
import gaiasky.scenegraph.CelestialBody;
import gaiasky.util.i18n.I18n;
import gaiasky.util.scene2d.OwnCheckBox;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextField;
import gaiasky.util.validator.ExistingLocationValidator;
import gaiasky.util.validator.FloatValidator;

public class LandAtWindow extends GenericDialog {

    private final CelestialBody target;
    private CheckBox latlonCb, locationCb;

    private OwnTextField location, latitude, longitude;

    public LandAtWindow(CelestialBody target, Stage stage, Skin skin) {
        super(I18n.msg("context.landatcoord", target.getName()), skin, stage);
        this.target = target;

        setAcceptText(I18n.msg("gui.ok"));
        setCancelText(I18n.msg("gui.cancel"));

        // Build UI
        buildSuper();

    }

    @Override
    protected void build() {

        latlonCb = new OwnCheckBox(I18n.msg("context.lonlat"), skin, "radio", pad10);
        latlonCb.setChecked(false);
        latlonCb.addListener(new EventListener() {
            @Override
            public boolean handle(com.badlogic.gdx.scenes.scene2d.Event event) {
                if (event instanceof ChangeEvent) {
                    if (latlonCb.isChecked()) {
                        enableComponents(false, location);
                        enableComponents(true, longitude, latitude);
                        stage.setKeyboardFocus(longitude);
                    }
                    return true;
                }
                return false;
            }

        });
        longitude = new OwnTextField("", skin, new FloatValidator(0, 360));
        latitude = new OwnTextField("", skin, new FloatValidator(-90, 90));

        locationCb = new OwnCheckBox(I18n.msg("context.location"), skin, "radio", pad10);
        locationCb.setChecked(true);
        locationCb.addListener(new EventListener() {
            @Override
            public boolean handle(com.badlogic.gdx.scenes.scene2d.Event event) {
                if (event instanceof ChangeEvent) {
                    if (locationCb.isChecked()) {
                        enableComponents(true, location);
                        enableComponents(false, longitude, latitude);
                        stage.setKeyboardFocus(location);
                    }
                    return true;
                }
                return false;
            }

        });
        location = new OwnTextField("", skin, new ExistingLocationValidator(target));

        new ButtonGroup<CheckBox>(latlonCb, locationCb);

        content.add(locationCb).left().top().padBottom(pad10).colspan(4).row();
        content.add(new OwnLabel(I18n.msg("context.location"), skin)).left().top().padRight(pad10);
        content.add(location).left().top().padBottom(pad10 * 2).row();

        content.add(latlonCb).left().top().padBottom(pad10).colspan(4).row();
        content.add(new OwnLabel(I18n.msg("context.longitude"), skin)).left().top().padRight(pad10);
        content.add(longitude).left().top().padRight(pad10 * 2);
        content.add(new OwnLabel(I18n.msg("context.latitude"), skin)).left().top().padRight(pad10);
        content.add(latitude).left().top();

    }

    @Override
    protected void accept() {
        if (latlonCb.isChecked()) {
            EventManager.publish(Event.LAND_AT_LOCATION_OF_OBJECT, this, target, Double.parseDouble(longitude.getText()), Double.parseDouble(latitude.getText()));
        } else if (locationCb.isChecked()) {
            EventManager.publish(Event.LAND_AT_LOCATION_OF_OBJECT, this, target, location.getText());
        }
    }

    @Override
    protected void cancel() {
    }

    /**
     * Sets the enabled property on the given components
     * 
     * @param enabled
     * @param components
     */
    protected void enableComponents(boolean enabled, Disableable... components) {
        for (Disableable c : components) {
            if (c != null)
                c.setDisabled(!enabled);
        }
    }

    @Override
    public void dispose() {

    }

    public void setKeyboardFocus() {
        stage.setKeyboardFocus(location);
    }

}
