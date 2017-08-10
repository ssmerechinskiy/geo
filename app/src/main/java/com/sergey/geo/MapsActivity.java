package com.sergey.geo;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnCameraMoveStartedListener {
    private final static String TAG = MapsActivity.class.getSimpleName();

    private GoogleMap mMap;
    private MapPresenter presenter;

    private View contentView;
    private View progress;

    private PopupWindow mPopupWindow;
    private int mPoupMenuWidth;
    private int mPopupMenuHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        contentView = findViewById(android.R.id.content);
        progress = findViewById(R.id.progress);
        presenter = new MapPresenter(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        presenter.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        presenter.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        presenter.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        presenter.onDestroy();
    }

    /**---------------------------------------------------------------------------------------------
    * Map events
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady");
        mMap = googleMap;
        presenter.onMapReady(googleMap);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnCameraMoveStartedListener(this);
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        presenter.onMapLongClick(latLng);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return presenter.onMarkerClick(marker);
    }

    @Override
    public boolean onMyLocationButtonClick() {
        presenter.onMyLocationButtonClick();
        return false;
    }

    @Override
    public void onCameraMoveStarted(int i) {
        switch (i) {
            case REASON_GESTURE :
                presenter.onUserDragCamera();
                return;
        }
    }
    /**---------------------------------------------------------------------------------------------
     * Map util methods
     * */
    public void prepareMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    public Marker displayMarker(LatLng latlng, String title, boolean showInfo) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latlng);
        markerOptions.title(title);
//        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
//        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_smiley));
        Marker marker = mMap.addMarker(markerOptions);
        marker.setSnippet("id:" + marker.getId());

        if(showInfo) marker.showInfoWindow();
        return marker;
    }

    public void removeMarker(Marker marker) {
        marker.remove();
    }

    public Circle displayCircle(LatLng latlng, double radius) {
        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(latlng);
        circleOptions.radius(radius);
        circleOptions.fillColor(0x40ff0000);
        circleOptions.strokeColor(Color.TRANSPARENT);
        circleOptions.strokeWidth(2);
        Circle circle = mMap.addCircle(circleOptions);
        return circle;
    }

    public void removeCircle(Circle circle) {
        circle.remove();
    }

    public void animateCameraToLatLng(LatLng latlng, float zoom) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, zoom));
    }


    /**---------------------------------------------------------------------------------------------
     * Activity util methods
     */
    public void showMessage(String message) {
        Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
    }

    public void showPopupMenuForMarker(final MapPresenter.GeoFenceUIModel model, final PopupMenuListener listener) {
        if(model == null) return;
        if (mPopupWindow != null) {
            mPopupWindow.dismiss();
            mPopupWindow = null;
        }

        View popupView = LayoutInflater.from(this).inflate(R.layout.marker_popup_menu, null);
        final PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);

        View createContainer = popupView.findViewById(R.id.create_container);
        TextView createGeofence = (TextView) popupView.findViewById(R.id.create_geofence);
        final EditText geofenceRadius = (EditText) popupView.findViewById(R.id.geofence_radius);
        final EditText geofenceName = (EditText)  popupView.findViewById(R.id.geofence_name);
        View createNetworkContainer = popupView.findViewById(R.id.create_geofence_network_item);
        final TextView networkName = (TextView) popupView.findViewById(R.id.geofence_network_name);
        TextView deleteGeofence = (TextView) popupView.findViewById(R.id.delete_geofence);
        TextView cancel = (TextView) popupView.findViewById(R.id.cancel);

        final boolean useWifi = !TextUtils.isEmpty(model.networkName) ? true : false;
        String errorRadiusMessage = "";
        if(useWifi) {
            geofenceRadius.setText(String.valueOf(GeofenceModel.USE_WIFI_MIN_RADIUS));
            errorRadiusMessage = "min radius with WIFI " + GeofenceModel.USE_WIFI_MIN_RADIUS + " meters";
        } else {
            geofenceRadius.setText(String.valueOf(GeofenceModel.USE_WITHOUT_WIFI_MIN_RADIUS));
            errorRadiusMessage = "min radius without WIFI " + GeofenceModel.USE_WITHOUT_WIFI_MIN_RADIUS + " meters";
        }

        final String finalErrorRadiusMessage = errorRadiusMessage;
        createGeofence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double radius = Double.valueOf(geofenceRadius.getEditableText().toString());
                if(!GeofenceModel.validateRadius(radius, useWifi)) {
                    geofenceRadius.setError(finalErrorRadiusMessage);
                } else {
                    String name = String.valueOf(geofenceName.getEditableText().toString());
                    model.radius = radius;
                    model.geofenceName = name;
                    listener.onCreateGeofence(model);
                    hidePopupMenuForMarker();
                }
            }
        });

        geofenceName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager imm =  (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(geofenceRadius, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        if(model.networkName != null) {
            createNetworkContainer.setVisibility(View.VISIBLE);
            networkName.setText(model.networkName);
        } else {
            createNetworkContainer.setVisibility(View.GONE);
        }

        geofenceRadius.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                geofenceRadius.setError(null);
                InputMethodManager imm =  (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(geofenceRadius, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        deleteGeofence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hidePopupMenuForMarker();
                listener.onDeleteGeofence(model);
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hidePopupMenuForMarker();
            }
        });

        if(model.geoCircle == null) {
            createContainer.setVisibility(View.VISIBLE);
        } else {
            createContainer.setVisibility(View.GONE);
        }

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        popupView.measure(size.x, size.y);

        mPoupMenuWidth = popupView.getMeasuredWidth();
        mPopupMenuHeight = popupView.getMeasuredHeight();

        mPopupWindow = popupWindow;
        updatePopupForMarker(model);
    }

    private void updatePopupForMarker(MapPresenter.GeoFenceUIModel model) {
        if (mPopupWindow != null) {
            // marker is visible
            if (mMap.getProjection().getVisibleRegion().latLngBounds.contains(model.getMarker().getPosition())) {
                if (!mPopupWindow.isShowing()) {
                    mPopupWindow.showAtLocation(contentView, Gravity.NO_GRAVITY, 0, 0);
                }
                Point p = mMap.getProjection().toScreenLocation(model.getMarker().getPosition());
                mPopupWindow.update(p.x - mPoupMenuWidth / 2, p.y - mPopupMenuHeight + 100, -1, -1);
            } else { // marker outside screen
                mPopupWindow.dismiss();
            }
        }
    }

    public void hidePopupMenuForMarker() {
        if(mPopupWindow != null) {
            mPopupWindow.dismiss();
            mPopupWindow = null;
        }
    }

    public void showSnackbar(final int mainTextStringId, final int actionStringId, View.OnClickListener listener) {
        Snackbar.make(findViewById(android.R.id.content), getString(mainTextStringId), Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    public void showSnackbar(final String mainText, final String actionString, View.OnClickListener listener) {
        Snackbar.make(findViewById(android.R.id.content), mainText, Snackbar.LENGTH_INDEFINITE)
                .setAction(actionString, listener).show();
    }

    public void showProgress() {
        progress.setVisibility(View.VISIBLE);
    }

    public void hideProgress() {
        progress.setVisibility(View.GONE);
    }



    /**---------------------------------------------------------------*/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        presenter.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        presenter.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    public interface PopupMenuListener {
//        void onCreateGeofence(int radius);
        void onCreateGeofence(MapPresenter.GeoFenceUIModel model);
        void onDeleteGeofence(MapPresenter.GeoFenceUIModel model);

    }


    public void animateCameraToLocation(Location location, float zoom) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), zoom));
    }


    public void showTipsDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.tips_dialog);

        Button dialogButton = (Button) dialog.findViewById(R.id.ok);
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    public Marker displayMarker(Location location, String title, boolean showInfo) {
        LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        markerOptions.title(title);
        Marker marker = mMap.addMarker(markerOptions);
        marker.setSnippet("id:" + marker.getId());
        if(showInfo) marker.showInfoWindow();
        return marker;
    }

    public Marker displayMarker(LatLng latlng, String title, boolean showInfo, BitmapDescriptor bitmapDescriptor) {
        Marker marker = displayMarker(latlng, title, showInfo);
        if(bitmapDescriptor != null) marker.setIcon(bitmapDescriptor);
        return marker;
    }

    public Circle displayCircle(Location location, double radius) {
        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(new LatLng(location.getLatitude(), location.getLongitude()));
        circleOptions.radius(radius);
        circleOptions.fillColor(0x40ff0000);
        circleOptions.strokeColor(Color.TRANSPARENT);
        circleOptions.strokeWidth(2);
        Circle circle = mMap.addCircle(circleOptions);
        return circle;
    }
}
