package com.fr3ts0n.ecu.gui.androbd.view;

import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.fr3ts0n.ecu.gui.androbd.model.bo.RouteBO;
import com.fr3ts0n.ecu.gui.androbd.view.adapters.RouteViewPagerAdapter;
import com.fr3ts0n.ecu.gui.androbd.model.ObdData;
import com.fr3ts0n.ecu.gui.androbd.model.Route;
import com.fr3ts0n.ecu.gui.androbd.model.RouteAlert;
import com.fr3ts0n.ecu.gui.androbd.model.RouteData;
import com.fr3ts0n.ecu.gui.androbd.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.rd.PageIndicatorView;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;

/**
 * Full screen dialog to show route statistics
 */
public class RouteStatsDialogFragment extends DialogFragment implements ViewPager.OnPageChangeListener, OnMapReadyCallback {

    public static final String TAG = "FullScreenDialog";
    public static final String ROUTE_ID = "route_id";

    private String mRouteId;
    private GoogleMap googleMap;
    private MapView mapView;
    private Route route;
    private ProgressBar progressBar;
    private PageIndicatorView pageIndicatorView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle);

        if (getArguments() != null) {
            mRouteId = getArguments().getString(ROUTE_ID);
            route = RouteBO.getInstance().findRouteById(mRouteId, null);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state) {
        super.onCreateView(inflater, parent, state);

        //Create viewpager
        View view = getActivity().getLayoutInflater().inflate(R.layout.route_stats_dialog, parent, false);
        ViewPager viewPager = view.findViewById(R.id.viewPager);
        viewPager.setAdapter(new RouteViewPagerAdapter(getActivity().getApplicationContext(),mRouteId, this));
        viewPager.addOnPageChangeListener(this);

        //Add the page indicator to the viewpager
        pageIndicatorView = view.findViewById(R.id.pageIndicatorView);
        pageIndicatorView.setCount(viewPager.getAdapter().getCount()); // specify total count of indicators

        //Loading icon
        progressBar = view.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);

        //Map view
        mapView = view.findViewById(R.id.route_item_map);

        if (mapView != null) {

            // Initialise the MapView
            mapView.onCreate(null);

           // mapView.setClickable(false);
            mapView.onResume();

            // Set the map ready callback to receive the GoogleMap object
            mapView.getMapAsync(this);
        }

        return view;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {

        switch (position) {
            case 0: //Basic route data
                pageIndicatorView.setSelection(0);
                googleMap.clear();
                setBasicPolyline(route, false);
                break;

            case 1: //Engine speed statistics
                pageIndicatorView.setSelection(1);
                googleMap.clear();
                new PrintMapAsynchronous().execute(route.getId(), "engine_speed");
                break;

            case 2: //Vehicle speed statistics
                pageIndicatorView.setSelection(2);
                googleMap.clear();
                new PrintMapAsynchronous().execute(route.getId(), "vehicle_speed");
                break;

            case 3: //Consumption speed statistics
                pageIndicatorView.setSelection(3);
                googleMap.clear();
                new PrintMapAsynchronous().execute(route.getId(), "consumption");
                break;
        }

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    /**
     * Instantiating this object and adding arguments
     * @param routeId
     * @return
     */
    public static RouteStatsDialogFragment newInstance(String routeId) {
        RouteStatsDialogFragment myFragment = new RouteStatsDialogFragment();

        Bundle args = new Bundle();
        args.putString(ROUTE_ID, routeId);
        myFragment.setArguments(args);

        return myFragment;
    }

    /**
     * Create and add items to the map when ready
     * @param mMap
     */
    @Override
    public void onMapReady(GoogleMap mMap) {
        MapsInitializer.initialize(getActivity());
        googleMap = mMap;
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        setBasicPolyline(RouteBO.getInstance().findRouteById(mRouteId,null), true);
        setMarkers(RouteBO.getInstance().findRouteById(mRouteId,null));

    }


    /**
     * Add a basic polyline to the map
     * @param route
     * @param firstTime
     */
    private void setBasicPolyline(Route route, final boolean firstTime){

        final List<RouteData> dataList = route.getDataList();

        if(googleMap!=null){
            googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                @Override
                public void onMapLoaded() {

                    PolylineOptions polylineOptions = new PolylineOptions();

                    for(RouteData data: dataList){
                        polylineOptions.add(new LatLng(data.getCoordinateX(), data.getCoordinateY()));
                    }

                    polylineOptions.color(Color.RED).geodesic(true);
                    Polyline line = googleMap.addPolyline(polylineOptions);

                    //If first time, center route
                    if(firstTime){
                        centerMapFromPolyline(line);
                    }
                }
            });

            //Add markers
            setMarkers(route);
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }
    }


    /**
     * Add route alerts/incidences to the map as custom markers
     * @param route
     */
    private void setMarkers(final Route route){
        final List<RouteAlert> alertList = route.getRouteAlertList();

        for(RouteAlert routeAlert: alertList) {
            BitmapDescriptor bitmapDescriptor = null;

            switch (routeAlert.getType()){
                case "NOISE":
                    bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.marker_noise);
                    break;
                case "ENGINE":
                    bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.marker_engine);
                    break;
                case "BRAKES":
                    bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.marker_brakes);
                    break;
                case "GEAR_BOX":
                    bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.marker_gears);
                    break;
                case "TEMPERATURE":
                    bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.marker_temperature);
                    break;
                case "OVERSUBSTEER":
                    bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.marker_steer);
                    break;
                case "SPEAK":
                    bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.marker_audio);
                    break;
                case "START_ROUTE":
                    bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.marker_play);
                    break;
                case "PAUSE_ROUTE":
                    bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.marker_pause);
                    break;
                case "FINISH_ROUTE":
                    bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.marker_stop);
                    break;
                default:
            }

            //Add marker
            googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(routeAlert.getLatitude(), routeAlert.getLongitude()))
                    .icon(bitmapDescriptor));

        }
    }


    /**
     * Center route in map
     * @param line
     */
   private void centerMapFromPolyline(Polyline line){
       boolean hasPoints = false;
       Double maxLat = null, minLat = null, minLon = null, maxLon = null;

       if (line != null && line.getPoints() != null) {
           List<LatLng> pts = line.getPoints();
           for (LatLng coordinate : pts) {
               // Find out the maximum and minimum latitudes & longitudes
               // Latitude
               maxLat = maxLat != null ? Math.max(coordinate.latitude, maxLat) : coordinate.latitude;
               minLat = minLat != null ? Math.min(coordinate.latitude, minLat) : coordinate.latitude;

               // Longitude
               maxLon = maxLon != null ? Math.max(coordinate.longitude, maxLon) : coordinate.longitude;
               minLon = minLon != null ? Math.min(coordinate.longitude, minLon) : coordinate.longitude;

               hasPoints = true;
           }
       }

       if (hasPoints) {
           LatLngBounds.Builder builder = new LatLngBounds.Builder();
           builder.include(new LatLng(maxLat, maxLon));
           builder.include(new LatLng(minLat, minLon));
           googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 48));
       }
   }

    /**
     * Set polyline color depending on values
     * @param list
     * @param min
     * @param max
     */
    private void setPolylineColor(List<PolylineDTO> list, float min, float max){

        for(PolylineDTO dto: list){
            final int colorStart = Color.parseColor("green");
            final int colorEnd = Color.parseColor("red");
            int color = interpolateColor(colorStart, colorEnd, dto.value / (max + min));
            dto.polyline.color(color).geodesic(true);
        }
    }

    /**
     * Interpolate method
     * @param a
     * @param b
     * @param proportion
     * @return
     */
    private float interpolate(final float a, final float b, final float proportion) {
        return (a + ((b - a) * proportion));
    }

    /**
     * Interpolate color method
     * @param a
     * @param b
     * @param proportion
     * @return
     */
    private int interpolateColor(final int a, final int b, final float proportion) {
        final float[] hsva = new float[3];
        final float[] hsvb = new float[3];
        Color.colorToHSV(a, hsva);
        Color.colorToHSV(b, hsvb);
        for (int i = 0; i < 3; i++) {
            hsvb[i] = interpolate(hsva[i], hsvb[i], proportion);
        }
        return Color.HSVToColor(hsvb);
    }

    /**
     * Small class to store polyline information
     */
    private class PolylineDTO {
        public float value;
        public PolylineOptions polyline = new PolylineOptions();
    }

    /**
     * Calculate consumption data
     * @param routeData
     * @param route
     * @return
     */
    public float getConsumptionValue(RouteData routeData, Route route){

        Float massAirFlow=null, volumetricFuelFlow, lambdaValue=null;

        for(ObdData obdData: routeData.getObdData()){
            if("mass_airflow".equals(obdData.getMnemonic())){
                massAirFlow = Float.parseFloat(obdData.getValue());
            }else if("o2_sensor_lambda_b1s1".equals(obdData.getMnemonic())){
                try {
                    lambdaValue = Float.parseFloat(obdData.getValue());
                }catch (Exception e){
                    //No lambda value
                }
            }
        }

        if(lambdaValue!=null){
            float lambdaRatio = 0.23478f / ( 0.218911f - 0.18415f * lambdaValue );
            volumetricFuelFlow = (massAirFlow/1000 * 3600/(lambdaRatio * route.getVehicle().getFuelType().getAfr())) * (route.getVehicle().getFuelType().getDensity()/1000);
        }else{
            //car has no lambda
            volumetricFuelFlow = (massAirFlow/1000 * 3600/(route.getVehicle().getFuelType().getAfr())) * (route.getVehicle().getFuelType().getDensity()/1000);
        }

        if(volumetricFuelFlow<0){
            return 0.0f;
        }else{
            return volumetricFuelFlow;
        }
    }

    /**
     * Due to some calculations, it is necessary to print the task in another thread
     */
    private class PrintMapAsynchronous extends AsyncTask<Object, Integer, Boolean> {

        List<PolylineDTO> polylineList;

        @Override
        protected Boolean doInBackground(Object... params) {

            Realm realm = null;
            try {
                realm = Realm.getDefaultInstance();

                //Get route
                Route route = RouteBO.getInstance().findRouteById((String)params[0],realm);
                String dataType = (String) params[1];


                float max = 0;
                float min = 99999;
                LatLng lastPoint = null;
                polylineList = new ArrayList<>();
                for (RouteData data : route.getDataList()) {
                    for(ObdData obdData: data.getObdData()){
                        if(dataType.equals(obdData.getMnemonic()) || dataType.equals("consumption")){

                            float value;
                            if(!dataType.equals("consumption")){ //If consumption, extra calculation is needed
                                value = Float.parseFloat(obdData.getValue());
                            }else{ //If not, just get the value
                                value = getConsumptionValue(data, route);
                            }

                            //Draw multiple polyline colors to get a colorful polyline
                            if(lastPoint==null){
                                lastPoint = new LatLng(data.getCoordinateX(), data.getCoordinateY());
                            }else{
                                PolylineDTO dto = new PolylineDTO();
                                dto.value = value;
                                dto.polyline.add(lastPoint, new LatLng(data.getCoordinateX(), data.getCoordinateY()));
                                polylineList.add(dto);
                                lastPoint = new LatLng(data.getCoordinateX(), data.getCoordinateY());
                            }

                            //Get max and min values
                            min = min < value ? min : value;
                            max = max > value ? max : value;
                        }
                    }
                }

                setPolylineColor(polylineList, min, max);

            } finally {
                if (realm != null) {
                    realm.close();
                }
            }

            return true;
        }


        @Override
        protected void onProgressUpdate(Integer... values) {

        }

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Boolean result) {

            //Print polyline in main thread
            for(PolylineDTO dto: polylineList){
                googleMap.addPolyline(dto.polyline);
            }

            //Print markers in the main thread
            setMarkers(route);
            progressBar.setVisibility(View.INVISIBLE);
        }

        @Override
        protected void onCancelled() {

        }
    }
}