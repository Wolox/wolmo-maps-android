package ar.com.wolox.wolmo.maps.fragment;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.View;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import ar.com.wolox.wolmo.core.presenter.BasePresenter;
import ar.com.wolox.wolmo.maps.R;

import java.util.List;

/**
 * Fragment that allows rendering {@link Marker}s and {@link Polyline}s on the {@link GoogleMap}. It
 * can display them as 'selected', giving them a highlighted fashion.
 *
 * The rendering methods <b>have</b> to be called <b>after</b> {@link #onMapReady(GoogleMap)}, which
 * is when the {@link MapView} finishes being laid out.
 */
public abstract class AbstractMapFragment<P extends BasePresenter & IMapPresenter> extends WolmoDrawableMapFragment<P> {

    private static final float ROUTE_Z = 0.0f;
    private static final float SELECTED_ROUTE_Z = 1.0f;
    private static final float ZOOM_WHOLE_WORLD = 4;

    private boolean mWasDestroyed;

    private GoogleMap mMap;

    @Override
    @CallSuper
    public void init() {
        mWasDestroyed = false;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final MapView mapView = getMapView();
        if (mapView == null) return;

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final GoogleMap map) {
                mapView.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mWasDestroyed) return;

                        AbstractMapFragment.this.onMapReady(map);
                    }
                });
            }
        });
    }

    /**
     * Sets an internal field to parameter {@link GoogleMap} and notifies the presenter.
     *
     * @param map readied map
     */
    protected final void onMapReady(@NonNull GoogleMap map) {
        mMap = map;
        mMap.getUiSettings().setMapToolbarEnabled(false);
        setMapParameters();
        setDefaultPosition();
        getPresenter().onMapReady();
    }

    /**
     * Set the default position: latitude, longitude and zoom
     */
    private void setDefaultPosition() {
        if (getDefaultLatlng() != null)
            applyCameraUpdate(CameraUpdateFactory.newLatLngZoom(getDefaultLatlng(), getDefaultZoom()));
    }

    /**
     * Override to set default zoom
     */
    protected float getDefaultZoom() {
        return ZOOM_WHOLE_WORLD;
    }

    /**
     * Override to set default latitude and longitude
     */
    protected @Nullable LatLng getDefaultLatlng() {
        return null;
    }

    /**
     * This method is for the child to set map parameters when the map is instantiated
     */
    protected void setMapParameters() {}

    /**
     * Applies a {@link CameraUpdate} to the map
     *
     * @param cameraUpdate target update
     */
    protected final void applyCameraUpdate(@NonNull CameraUpdate cameraUpdate) {
        applyCameraUpdate(cameraUpdate, mMap.getCameraPosition().zoom > getDefaultZoom());
    }


    /**
     * Applies a {@link CameraUpdate} to the map
     *
     * @param cameraUpdate target update
     */
    protected final void applyCameraUpdate(@NonNull CameraUpdate cameraUpdate, boolean animate) {
        if (animate) {
            mMap.animateCamera(cameraUpdate);
        } else {
            mMap.moveCamera(cameraUpdate);
        }
    }

    /**
     * Draws a {@link Marker} from a position with default pin assets.
     *
     * @param position position of marker
     * @param selected determines color and z-index of the resulting marker
     *
     * @return drawn {@link Marker}
     */
    protected final Marker drawMarker(@NonNull LatLng position, boolean selected) {
        return drawMarker(position,
                          selected ? getSelectedPinRes() : getUnselectedPinRes(),
                          selected ? SELECTED_ROUTE_Z : ROUTE_Z);
    }

    /**
     * Draws a {@link Marker} with the parameters as rendering parameters.
     *
     * @param position   Desired position.
     * @param drawableId {@link StringRes} to use as icon.
     * @param zIndex     Z-index used to render.
     *
     * @return Drawn {@link Marker};
     */
    @NonNull
    private Marker drawMarker(@NonNull LatLng position, @DrawableRes int drawableId, float zIndex) {
        return mMap.addMarker(new MarkerOptions()
                                    .position(position)
                                    .icon(BitmapDescriptorFactory.fromResource(drawableId))
                                    .zIndex(zIndex));
    }

    /**
     * Draws a {@link Marker} with a custom icon.
     *
     * @param position   Desired position.
     * @param drawableId {@link StringRes} to use as icon.
     *
     * @return Drawn {@link Marker}.
     */
    @NonNull
    protected final Marker drawCustomIconMarker(
            @NonNull LatLng position, @DrawableRes int drawableId) {
        return drawMarker(position, drawableId, SELECTED_ROUTE_Z);
    }

    /**
     * Draws a {@link Polyline} from a route of points.
     *
     * @param route points to draw
     * @param selected determines color and z-index of the resulting polyline
     *
     * @return drawn {@link Polyline}
     */
    @NonNull
    protected final Polyline drawPolyline(@NonNull List<LatLng> route, boolean selected) {
        return mMap.addPolyline(new PolylineOptions()
                                        .addAll(route)
                                        .clickable(true)
                                        .color(selected ? getSelectedPolylineColor() : getPolylineColor())
                                        .zIndex(selected ? SELECTED_ROUTE_Z : ROUTE_Z));
    }

    /**
     * Sets {@link Marker}'s icon pin and z-index.
     *
     * @param marker target marker
     * @param selected determines which pin and z-index to set
     */
    @NonNull
    protected final void formatMarker(@NonNull Marker marker, boolean selected) {
        marker.setIcon(BitmapDescriptorFactory.fromResource(
                selected ? getSelectedPinRes() : getUnselectedPinRes()));
        marker.setZIndex(selected ? SELECTED_ROUTE_Z : ROUTE_Z);
    }

    /**
     * Sets {@link Polyline}'s color and z-index.
     *
     * @param polyline target polyline
     * @param selected determines which color and z-index to set
     */
    protected final void formatPolyline(@NonNull Polyline polyline, boolean selected) {
        polyline.setColor(selected ? getSelectedPolylineColor() : getPolylineColor());
        polyline.setZIndex(selected ? SELECTED_ROUTE_Z : ROUTE_Z);
    }

    /**
     * Sets a {@link GoogleMap.OnPolylineClickListener} to the map.
     *
     * @param listener target listener
     */
    protected final void setPolylineClickListener(
            @Nullable GoogleMap.OnPolylineClickListener listener) {
        mMap.setOnPolylineClickListener(listener);
    }

    /**
     * Sets a {@link GoogleMap.OnMarkerClickListener} to the map.
     *
     * @param markerClickListener target listener
     */
    public void setMarkerClickListener(GoogleMap.OnMarkerClickListener markerClickListener) {
        mMap.setOnMarkerClickListener(markerClickListener);
    }

    /**
     * Clears the map
     */
    public void clearMap() {
        mMap.clear();
    }

    @Override
    @CallSuper
    public void onDestroyView() {
        mWasDestroyed = true;
        super.onDestroyView();
    }

    /**
     * Check if all positions in the list are visible in the map.
     * @param positions
     * @return true if all positions in the list are visible.
     */
    protected boolean contains(@NonNull List<LatLng> positions) {
        LatLngBounds latLngBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        for (LatLng latLng : positions) {
            if(!latLngBounds.contains(latLng)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if the position are visible in the map.
     * @param latLng
     * @return true if the position is visible in the map.
     */
    protected boolean contains(@NonNull LatLng latLng) {
        return mMap.getProjection().getVisibleRegion().latLngBounds.contains(latLng);
    }

    /**
     * Set padding that all the elements will have on the map.
     * @param left padding left.
     * @param top padding top.
     * @param right padding right.
     * @param bottom padding bottom.
     */
    protected void setMapPadding(int left, int top, int right, int bottom) {
        mMap.setPadding(left, top, right, bottom);
    }

    //Override to change style
    public @ColorRes int getSelectedPolylineColorId() {
        return R.color.polyline_green;
    }

    private int getSelectedPolylineColor() {
        return getResources().getColor(getSelectedPolylineColorId());
    }

    public @ColorRes int getPolylineColorId() {
        return R.color.polyline_grey;
    }

    private int getPolylineColor() {
        return getResources().getColor(getPolylineColorId());
    }

    public @DrawableRes int getSelectedPinRes() {
        return R.drawable.ic_map_pin_selected;
    }

    public @DrawableRes int getUnselectedPinRes() {
        return R.drawable.ic_map_pin_unselected;
    }

    public void showMapToolBar (boolean enable) {
        mMap.getUiSettings().setMapToolbarEnabled(enable);
    }

}