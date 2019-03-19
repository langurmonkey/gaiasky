package gaia.cu9.ari.gaiaorbit.interfce;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

public interface IMinimapScale {
    boolean isActive(Vector3d campos);

    void initialize(OrthographicCamera ortho, SpriteBatch sb, ShapeRenderer sr, BitmapFont font, int side, int sideshort);

    void renderSideProjection(FrameBuffer fb);

    void renderTopProjection(FrameBuffer fb);
}
