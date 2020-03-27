package com.fr3ts0n.ecu.gui.androbd.services;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;

/**
 * Created by dpconde on 8/4/18.
 */
public class LocationService {

    private static LocationService instance = null;


    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 2000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    /**
     * Provides access to the Location Settings API.
     */
    private SettingsClient mSettingsClient;

    /**
     * Provides access to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private LocationRequest mLocationRequest;

    /**
     * Callback for Location events.
     */
    private LocationCallback mLocationCallback;

    /**
     * Represents a geographical location.
     */
    private Location mCurrentLocation;

    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    private LocationSettingsRequest mLocationSettingsRequest;


    public double longitude;
    public double latitude;
    private GoogleMap map;
    private RouteService.RouteStatus routeStatus;
    private PolylineOptions polyline;
    private Activity activity;

    /**
     * Singleton implementation
     * @return
     */
    public static LocationService getInstance(Activity activity)     {
        if (instance == null) {
            instance = new LocationService(activity);
        }
        return instance;
    }

    /**
     * Local constructor
     */
    private LocationService( Activity activity)     {
        this.activity = activity;
        initLocationService(activity);
        routeStatus = RouteService.RouteStatus.READY;

        polyline = new PolylineOptions();
        polyline.width(20).color(Color.RED).geodesic(true);
    }



    /**
     * Sets up location service after permissions is granted
     */
    @TargetApi(23)
    private void initLocationService(Activity activity) {

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(activity.getApplicationContext());
        mSettingsClient = LocationServices.getSettingsClient(activity.getApplicationContext());

        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();

        if ( Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission( activity.getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission( activity.getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            startLocationUpdates();
        } catch (Exception ex)  {
            Log.d("Error", ex.toString());
        }
    }

    /**
     * Update current coordinate values
     */
    private void updateCoordinates(){
        longitude = mCurrentLocation.getLongitude();
        latitude = mCurrentLocation.getLatitude();
    }


    /**
     * Update the "New Route" fragment map
     * @param location
     */
    public void updateMap(Location location){

        updateCoordinates();

        if(map!=null) {
            //Update user position
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(location.getLatitude(), location.getLongitude()))  // Sets the center of the map to Mountain View
                    .zoom(17)                   // Sets the zoom
                    .bearing(90)                // Sets the orientation of the camera to east
                    .tilt(55)                   // Sets the tilt of the camera to 55 degrees
                    .build();                   // Creates a CameraPosition from the builder
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }

        //Draw a polyline in map if a route has been started
        if(map!=null && routeStatus.equals(RouteService.RouteStatus.STARTED)){
            if(polyline==null){
                polyline = new PolylineOptions();
                polyline.width(20).color(Color.RED).geodesic(true);
            }

            //Update user position
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(location.getLatitude(), location.getLongitude()))      // Sets the center of the map to Mountain View
                    .zoom(17)                          // Sets the zoom
                    .bearing(location.getBearing())    // Sets the orientation of the camera to east
                    .tilt(55)                          // Sets the tilt of the camera to 30 degrees
                    .build();                          // Creates a CameraPosition from the builder
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

            //Add polyline
            polyline.getPoints().add(new LatLng(location.getLatitude(), location.getLongitude()));
            map.addPolyline(polyline);
        }
    }

    /**
     * Uses a {@link com.google.android.gms.location.LocationSettingsRequest.Builder} to build
     * a {@link com.google.android.gms.location.LocationSettingsRequest} that is used for checking
     * if a device has the needed location settings.
     */
    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }


    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


    /**
     * Creates a callback for receiving location events.
     */
    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                mCurrentLocation = locationResult.getLastLocation();
                updateMap(mCurrentLocation);
            }
        };
    }

    /**
     * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
     * runtime permission has been granted.
     */
    private void startLocationUpdates() {
        // Begin by checking if the device has the necessary location settings.
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(activity, new OnSuccessListener<LocationSettingsResponse>() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                    }
                });
    }

    /**
     * Finish the current route.
     * Clean the map and stop drawing the polyline
     */
    public void finishRoute(){
        if(polyline!=null){
            polyline = null;
            map.clear();
        }
        this.routeStatus = RouteService.RouteStatus.CANCELLED;
    }

    public void setMap(GoogleMap map) {
        this.map = map;
    }

    public void setRouteStatus(RouteService.RouteStatus routeStatus) {
        this.routeStatus = routeStatus;
    }
}