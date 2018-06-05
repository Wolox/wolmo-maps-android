package ar.com.wolox.wolmo.maps.fragment;

/**
 * Notification by the view that the map is ready.
 */
public interface IMapPresenter {
    void onMapReady();
    void onMapDestroyed();
}