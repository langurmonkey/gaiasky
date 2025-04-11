package gaiasky.gui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.gui.bookmarks.BookmarksManager.BookmarkNode;
import gaiasky.gui.window.GenericDialog;
import gaiasky.util.TextUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Vector3d;
import gaiasky.util.scene2d.OwnLabel;

import java.time.Instant;
import java.util.Objects;

/**
 * Displays information of a given bookmark.
 */
public class BookmarkInfoDialog extends GenericDialog {

    private BookmarkNode bookmark;

    public BookmarkInfoDialog(Stage stage,
                              Skin skin) {
        super(I18n.msg("gui.bookmark.info"), skin, stage);

        this.setModal(false);

        setAcceptText(I18n.msg("gui.close"));

        buildSuper();
    }

    @Override
    protected void build() {
        content.clear();
        float pad = 8f;
        float pad34 = 70f;
        float minWidth = 300f;

        if (bookmark != null) {

            // Type
            String type = TextUtils.trueCapitalise(I18n.msg("gui.bookmark.info.type." + bookmark.type.name().toLowerCase()));
            OwnLabel bookmarkType = new OwnLabel(type, skin, "default-blue");
            content.add(new OwnLabel(I18n.msg("gui.bookmark.info.type"), skin, "header-s")).left().padRight(pad34).padBottom(pad10).minWidth(minWidth);
            content.add(bookmarkType).pad(pad, 0, pad20, pad).left().padBottom(pad10).minWidth(minWidth).row();


            OwnLabel bookmarkObject;
            if (bookmark.isTypeObject()) {

                // Object name
                bookmarkObject = new OwnLabel(bookmark.name, skin, "default-pink");
                content.add(new OwnLabel(I18n.msg("gui.bookmark.info.object.name"), skin, "header-s")).left().padRight(pad34).padBottom(pad10).minWidth(minWidth);
                content.add(bookmarkObject).pad(pad, 0, pad20, pad).left().padBottom(pad10).minWidth(minWidth).row();

            } else if (bookmark.isTypeLocation()) {

                // Name
                OwnLabel bookmarkName = new OwnLabel(bookmark.name, skin, "default-pink");
                content.add(new OwnLabel(I18n.msg("gui.bookmark.info.name"), skin, "header-s")).left().padRight(pad34).padBottom(pad10).minWidth(minWidth);
                content.add(bookmarkName).pad(pad, 0, pad20, pad).left().padBottom(pad10).minWidth(minWidth).row();

                // Position
                OwnLabel bookmarkPosition = new OwnLabel(vectorToString(bookmark.position), skin);
                content.add(new OwnLabel(I18n.msg("gui.bookmark.info.location.position"), skin, "header-s")).left().padRight(pad34).padBottom(pad10).minWidth(minWidth);
                content.add(bookmarkPosition).pad(pad, 0, pad20, pad).left().padBottom(pad10).minWidth(minWidth).row();

                // Direction
                OwnLabel bookmarkDirection = new OwnLabel(vectorToString(bookmark.direction), skin);
                content.add(new OwnLabel(I18n.msg("gui.bookmark.info.location.direction"), skin, "header-s")).left().padRight(pad34).padBottom(pad10).minWidth(minWidth);
                content.add(bookmarkDirection).pad(pad, 0, pad20, pad).left().padBottom(pad10).minWidth(minWidth).row();

                // Up
                OwnLabel bookmarkUp = new OwnLabel(vectorToString(bookmark.up), skin);
                content.add(new OwnLabel(I18n.msg("gui.bookmark.info.location.up"), skin, "header-s")).left().padRight(pad34).padBottom(pad10).minWidth(minWidth);
                content.add(bookmarkUp).pad(pad, 0, pad20, pad).left().padBottom(pad10).minWidth(minWidth).row();

                // Time
                OwnLabel bookmarkTime = new OwnLabel(instantToString(bookmark.time), skin);
                content.add(new OwnLabel(I18n.msg("gui.bookmark.info.location.time"), skin, "header-s")).left().padRight(pad34).padBottom(pad10).minWidth(minWidth);
                content.add(bookmarkTime).pad(pad, 0, pad20, pad).left().padBottom(pad10).minWidth(minWidth).row();

                // Focus
                bookmarkObject = new OwnLabel(stringToString(bookmark.focus), skin);
                content.add(new OwnLabel(I18n.msg("gui.bookmark.info.location.focus"), skin, "header-s")).left().padRight(pad34).padBottom(pad10).minWidth(minWidth);
                content.add(bookmarkObject).pad(pad, 0, pad20, pad).left().padBottom(pad10).minWidth(minWidth).row();

                // Settings
                OwnLabel bookmarkSettings = new OwnLabel(yesNo(bookmark.uuid), skin);
                content.add(new OwnLabel(I18n.msg("gui.bookmark.info.location.settings"), skin, "header-s")).left().padRight(pad34).padBottom(pad10).minWidth(minWidth);
                content.add(bookmarkSettings).pad(pad, 0, pad20, pad).left().padBottom(pad10).minWidth(minWidth).row();

                // UUID
                OwnLabel bookmarkUUID = new OwnLabel(stringToString(bookmark.uuid), skin, bookmark.uuid != null ? "mono-pink" : "default");
                content.add(new OwnLabel(I18n.msg("gui.bookmark.info.location.settings.id"), skin, "header-s")).left().padRight(pad34).padBottom(pad10).minWidth(minWidth);
                content.add(bookmarkUUID).pad(pad, 0, pad20, pad).left().padBottom(pad10).minWidth(minWidth).row();

            } else if (bookmark.isTypeFolder()) {

                // Folder name
                bookmarkObject = new OwnLabel(bookmark.name, skin, "default-pink");
                content.add(new OwnLabel(I18n.msg("gui.bookmark.info.folder.name"), skin, "header-s")).left().padRight(pad34).padBottom(pad10).minWidth(minWidth);
                content.add(bookmarkObject).pad(pad, 0, pad20, pad).left().padBottom(pad10).minWidth(minWidth).row();

                // Folder path
                OwnLabel bookmarkPath = new OwnLabel(bookmark.path.toString(), skin);
                content.add(new OwnLabel(I18n.msg("gui.bookmark.info.folder.path"), skin, "header-s")).left().padRight(pad34).padBottom(pad10).minWidth(minWidth);
                content.add(bookmarkPath).pad(pad, 0, pad20, pad).left().padBottom(pad10).minWidth(minWidth).row();

                // Number
                OwnLabel bookmarksNum = new OwnLabel(Integer.toString(bookmark.countBookmarksRec()), skin);
                content.add(new OwnLabel(I18n.msg("gui.bookmark.info.folder.number"), skin, "header-s")).left().padRight(pad34).padBottom(pad10).minWidth(minWidth);
                content.add(bookmarksNum).pad(pad, 0, pad20, pad).left().padBottom(pad10).minWidth(minWidth).row();

            }
        }

    }

    private String vectorToString(Vector3d v) {
        if (v != null) {
            return v.toString();
        } else {
            return I18n.msg("gui.none");
        }
    }

    private String instantToString(Instant i) {
        if (i != null) {
            return i.toString();
        } else {
            return I18n.msg("gui.none");
        }
    }

    private String stringToString(String s) {
        return Objects.requireNonNullElse(s, I18n.msg("gui.none"));
    }

    private String yesNo(String s) {
        if (s != null) {
            return I18n.msg("gui.yes");
        } else {
            return I18n.msg("gui.no");
        }
    }

    /**
     * Updates the dialog with the information from the given bookmark.
     *
     * @param bookmark The new bookmark.
     */
    public void updateView(BookmarkNode bookmark) {
        this.bookmark = bookmark;
        build();
    }


    @Override
    protected boolean accept() {
        return true;
    }


    @Override
    protected void cancel() {

    }

    @Override
    public void dispose() {

    }

}
