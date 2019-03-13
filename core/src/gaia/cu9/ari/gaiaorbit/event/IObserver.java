package gaia.cu9.ari.gaiaorbit.event;

public interface IObserver {

    void notify(Events event, Object... data);

}
