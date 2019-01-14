package gaia.cu9.ari.gaiaorbit.scenegraph;

public interface IVisibilitySwitch {
    public String getName();
    public void setName(String name);

    public String getDescription();
    public void setDescription(String name);

    public boolean isVisible();
    public void setVisible(boolean visible);
}
