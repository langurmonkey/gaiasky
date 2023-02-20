package gaiasky.scene.record;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import gaiasky.GaiaSky;
import gaiasky.scene.record.BilinearInterpolator.GridModel;
import gaiasky.util.GlobalResources;

public class HeightDataPixmap implements IHeightData {

    private final Pixmap heightPixmap;
    private GridModel model;

    public HeightDataPixmap(Pixmap heightPixmap, Runnable finished) {
        this.heightPixmap = heightPixmap;

        if (finished != null) {
            GaiaSky.postRunnable(finished);
        }
        initModel();
    }

    public HeightDataPixmap(String heightTexturePacked, Runnable finished) {
        this(new Pixmap(new FileHandle(GlobalResources.unpackAssetPath(heightTexturePacked))), finished);
    }

    public HeightDataPixmap(Texture texture, Runnable finished) {
        if (texture != null && texture.getTextureData() instanceof PixmapTextureData) {
            heightPixmap = texture.getTextureData().consumePixmap();
            initModel();
        } else {
            heightPixmap = null;
        }
        if (finished != null) {
            GaiaSky.postRunnable(finished);
        }
    }

    private void initModel(){
        this.model = new GridModel() {
            private Color color = new Color();
            @Override
            public int getWidth() {
                assert heightPixmap != null;
                return heightPixmap.getWidth();
            }

            @Override
            public int getHeight() {
                assert heightPixmap != null;
                return heightPixmap.getHeight();
            }

            @Override
            public double getValue(int x, int y) {
                assert heightPixmap != null;
                return color.set(heightPixmap.getPixel(x, y)).r;
            }
        };
    }

    @Override
    public double getNormalizedHeight(double u, double v) {
        if (heightPixmap == null) {
            return 0;
        }

        return BilinearInterpolator.interpolate(u, v, model, true, false);
    }
}
