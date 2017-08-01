package com.sergey.geo;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnCameraMoveListener {
    private final static String TAG = MapsActivity.class.getSimpleName();

    private GoogleMap mMap;
    private MapPresenter presenter;

    private View contentView;

    private PopupWindow mPopupWindow;
    private int mPoupMenuWidth;
    private int mPopupMenuHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        contentView = findViewById(android.R.id.content);
        presenter = new MapPresenter(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        presenter.onStart();
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

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady");
        mMap = googleMap;
        presenter.onMapReady(googleMap);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnCameraMoveListener(this);
    }

    /**---------------------------------------------------------------*/
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

    public void removeMarker(Marker marker) {
        marker.remove();
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

    public void removeCircle(Circle circle) {
        circle.remove();
    }

    public void animateCameraToLocation(Location location, float zoom) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), zoom));
    }

    public void showMessage(String message) {
        Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
    }

    public void showPopupMenuForMarker(Marker marker, final PopupMenuListener listener) {
        View popupView = LayoutInflater.from(this).inflate(R.layout.marker_popup_menu, null);
        final PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        TextView createGeofence = (TextView) popupView.findViewById(R.id.create_geofence);
        createGeofence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
                listener.onCreateGeofence();
            }
        });
        TextView deleteGeofence = (TextView) popupView.findViewById(R.id.delete_geofence);
        deleteGeofence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
                listener.onDeleteGeofence();
            }
        });
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        popupView.measure(size.x, size.y);

        mPoupMenuWidth = popupView.getMeasuredWidth();
        mPopupMenuHeight = popupView.getMeasuredHeight();

        mPopupWindow = popupWindow;
        updatePopupForMarker(marker);
    }

    private void updatePopupForMarker(Marker marker) {
        if (mPopupWindow != null) {
            // marker is visible
            if (mMap.getProjection().getVisibleRegion().latLngBounds.contains(marker.getPosition())) {
                if (!mPopupWindow.isShowing()) {
                    mPopupWindow.showAtLocation(contentView, Gravity.NO_GRAVITY, 0, 0);
                }
                Point p = mMap.getProjection().toScreenLocation(marker.getPosition());
                mPopupWindow.update(p.x - mPoupMenuWidth / 2, p.y - mPopupMenuHeight + 100, -1, -1);
            } else { // marker outside screen
                mPopupWindow.dismiss();
            }
        }
    }

    /**---------------------------------------------------------------*/


    @Override
    public void onMapLongClick(LatLng latLng) {
        presenter.onMapLongClick(latLng);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return presenter.onMarkerClick(marker);
    }

    @Override
    public void onCameraMove() {
//        presenter.onCameraMove();
    }

    public View getContentView() {
        return contentView;
    }

    public interface PopupMenuListener {
        void onCreateGeofence();
        void onDeleteGeofence();
    }
}
