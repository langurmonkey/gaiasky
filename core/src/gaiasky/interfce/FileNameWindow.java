/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnLabel;
import gaia.cu9.ari.gaiaorbit.util.scene2d.OwnTextField;
import gaia.cu9.ari.gaiaorbit.util.validator.LengthValidator;
import gaia.cu9.ari.gaiaorbit.util.validator.RegexpValidator;

public class FileNameWindow extends GenericDialog{

    private String defaultName;
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

        content.add(label).padRight(pad).padBottom(pad);
        content.add(fileName).padBottom(pad);
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
