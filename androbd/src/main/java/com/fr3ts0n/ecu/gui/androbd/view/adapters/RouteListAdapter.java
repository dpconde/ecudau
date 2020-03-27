package com.fr3ts0n.ecu.gui.androbd.view.adapters;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fr3ts0n.ecu.gui.androbd.model.bo.RouteBO;
import com.fr3ts0n.ecu.gui.androbd.model.Route;
import com.fr3ts0n.ecu.gui.androbd.model.RouteData;
import com.fr3ts0n.ecu.gui.androbd.R;
import com.fr3ts0n.ecu.gui.androbd.Utils;
import com.fr3ts0n.ecu.gui.androbd.view.RouteListFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

/**
 * Recycler view adapter for listing routes
 */
public class RouteListAdapter extends RecyclerView.Adapter<RouteListAdapter.RouteViewHolder> {

    private List<Route> data;
    private static RouteListFragment fragment;

    /**
     * Constructor
     * @param data
     * @param fragment
     */
    public RouteListAdapter(List<Route> data, RouteListFragment fragment) {
        this.data = data;
        this.fragment = fragment;
    }

    @Override
    public RouteViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.route_list_item_map, viewGroup, false);

        RouteViewHolder tvh = new RouteViewHolder(itemView);

        return tvh;
    }

    @Override
    public void onBindViewHolder(RouteViewHolder viewHolder, int pos) {
        Route item = data.get(pos);
        viewHolder.bindRoute(item);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }


    /**
     * Custom ViewHolder class for Route information
     */
    public static class RouteViewHolder extends RecyclerView.ViewHolder implements OnMapReadyCallback, View.OnClickListener{

        private TextView descriptionText;
        private TextView dateText;
        private TextView vehicleText;
        private MapView mapView;
        public GoogleMap googleMap;
        private Route route;
        private RelativeLayout statsButton;
        private RelativeLayout deleteRouteButton;
        private AlertDialog deleteRouteDialog;


        public RouteViewHolder(View itemView) {
            super(itemView);

            //Route description text
            descriptionText = itemView.findViewById(R.id.route_item_description);

            //Route date text
            dateText = itemView.findViewById(R.id.route_item_date);

            //Route vehicle text
            vehicleText = itemView.findViewById(R.id.route_item_vehicle);

            //MapVew
            mapView = itemView.findViewById(R.id.route_item_map);

            //Open statistics button
            statsButton = itemView.findViewById(R.id.statsButton);
            statsButton.setOnClickListener(this);

            //Delete route button
            deleteRouteButton = itemView.findViewById(R.id.deleteRouteButton);
            deleteRouteButton.setOnClickListener(this);

             if (mapView != null) {
                // Initialise the MapView
                mapView.onCreate(null);
                mapView.setClickable(false);
                mapView.onResume();

                // Set the map ready callback to receive the GoogleMap object
                mapView.getMapAsync(this);
             }
        }


        @Override
        public void onMapReady(GoogleMap mMap) {
            MapsInitializer.initialize(fragment.getActivity());
            googleMap = mMap;
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

            //Customize map view
            googleMap.getUiSettings().setMyLocationButtonEnabled(false);
            googleMap.getUiSettings().setMapToolbarEnabled(false);

            //Add polyline
            setPolyline(route);
        }


        /**
         * Set Route data
         * @param route
         */
        public void bindRoute(Route route) {
            descriptionText.setText(route.getDescription());
            dateText.setText(Utils.getInstance().formatDate(route.getStartDate()));
            vehicleText.setText(route.getVehicle().getManufacturer() + " " + route.getVehicle().getModel());
            this.route = route;
            setPolyline(route);
        }


        /**
         * Create and add a polyline with the route data
         * @param route
         */
        private void setPolyline(Route route){

            //Get data from route object
            final List<RouteData> dataList = route.getDataList();

            if(googleMap!=null){
                googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                    @Override
                    public void onMapLoaded() {

                        PolylineOptions polylineOptions = new PolylineOptions();

                        if(!dataList.isEmpty()){
                            //Add polyline points
                            for(RouteData data: dataList){
                                polylineOptions.add(new LatLng(data.getCoordinateX(), data.getCoordinateY()));
                            }

                            //Set polyline styles
                            polylineOptions.color(Color.RED).geodesic(true);

                            //Add polyline to the map
                            Polyline line = googleMap.addPolyline(polylineOptions);

                            //Center and zoom map depending on polyline points
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

                    }
                });
                googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            }


        }

        @Override
        public void onClick(View view) {
            if(view.getId() == R.id.statsButton){
                //Open statistics dialog
                fragment.openStatsDialog(route);
            }else if(view.getId() == R.id.deleteRouteButton){
                //Show confirm dialog to delete route
                showConfirmDeleteRoute();
            }
        }


        /**
         * Show confirm dialog in order to delete route
         */
        private void showConfirmDeleteRoute(){

            //Create dialog builder
            AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getActivity());
            builder.setTitle(route.getDescription());
            builder.setMessage(fragment.getString(R.string.routes_adapter_delete_body));

            //Create listener for positive answer
            builder.setPositiveButton(fragment.getString(R.string.routes_adapter_delete_button), new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    RouteBO.getInstance().deleteRoute(route.getId());
                    Toast.makeText(fragment.getActivity(), fragment.getString(R.string.routes_adapter_delete_message), Toast.LENGTH_SHORT).show();
                    fragment.listHasChanged();
                }
            });

            //Create listener for negative answer
            builder.setNegativeButton(fragment.getString(R.string.routes_adapter_cancel_button), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    deleteRouteDialog.dismiss();
                }
            });

            deleteRouteDialog = builder.create();
            deleteRouteDialog.show();
        }

    }


}