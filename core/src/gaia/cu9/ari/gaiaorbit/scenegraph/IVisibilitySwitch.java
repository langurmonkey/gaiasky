package gaia.cu9.ari.gaiaorbit.scenegraph;

public interface IVisibilitySwitch {
    String getName();

    void setName(String name);

    String getDescription();

    void setDescription(String name);

    boolean isVisible();

    void setVisible(boolean visible);
}
