/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.util.GlobalConf;
import gaiasky.util.I18n;
import gaiasky.util.scene2d.OwnLabel;
import gaiasky.util.scene2d.OwnTextField;
import gaiasky.util.validator.LengthValidator;
import gaiasky.util.validator.RegexpValidator;

public class FileNameWindow extends GenericDialog{

    private final String defaultName;
    private OwnTextField fileName;

    public FileNameWindow(String defaultName, Stage stage, Skin skin){
        super("Choose file name", skin, stage);
        setModal(true);
        this.defaultName = defaultName;

        setAcceptText(I18n.txt("gui.ok"));
        setCancelText(I18n.txt("gui.cancel"));

        buildSuper();

        pack();
    }

    @Override
    protected void build() {
        OwnLabel label = new OwnLabel("File name: ", skin);
        LengthValidator lengthValidator = new LengthValidator(3, 40);
        RegexpValidator nameValidator = new RegexpValidator(lengthValidator, "^[^*&%\\s\\+\\=\\\\\\/@#\\$&\\*()~]+$");
        fileName = new OwnTextField(defaultName, skin, nameValidator);
        fileName.setWidth(250 * GlobalConf.UI_SCALE_FACTOR);

        content.add(label).padRight(pad10).padBottom(pad10);
        content.add(fileName).padBottom(pad10);
    }

    /**
     * Returns the file name text field
     * @return The text field
     */
    public OwnTextField getFileNameField(){
        return fileName;
    }


    @Override
    protected void accept() {

    }

    @Override
    protected void cancel() {

    }
}
