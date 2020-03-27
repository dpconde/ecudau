package com.fr3ts0n.ecu.gui.androbd.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.fr3ts0n.ecu.gui.androbd.services.bluetooth.CommService;
import com.fr3ts0n.ecu.gui.androbd.model.bo.VehicleBO;
import com.fr3ts0n.ecu.gui.androbd.view.adapters.VehicleSpinnerAdapter;
import com.fr3ts0n.ecu.gui.androbd.services.LocationService;
import com.fr3ts0n.ecu.gui.androbd.model.Vehicle;
import com.fr3ts0n.ecu.gui.androbd.R;
import com.fr3ts0n.ecu.gui.androbd.services.RouteService;
import com.fr3ts0n.ecu.prot.obd.ObdProt;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;


public class NewRouteFragment extends Fragment implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback{



    private MapView mMapView;
    private GoogleMap googleMap;

    private RelativeLayout connectButton;
    private ImageView connectButtonImage;
    private RelativeLayout dialogBackground;
    private AlertDialog newRouteDialog;
    private RelativeLayout onRouteLayout;
    private RelativeLayout mainLayout;
    private RelativeLayout playPauseButton;
    private RelativeLayout incidenceButton;
    private AlertDialog pauseDialog;
    private AlertDialog noCarAvailableDialog;
    private AlertDialog newIncidenceDialog;
    private PowerManager.WakeLock wl;
    private MainActivity mainActivity;
    private Context context;
    final int REQUEST_FINE_LOCATION = 2;

    //Real-time data views
    private TextView rpm;
    private TextView speed;
    private TextView consumption;
    private TextView consumptionUnit;
    private boolean isTooSlow = false;


    public enum VIEW {
        NOT_CONNECTED,
        CONNECTED,
        ON_ROUTE
    }

    public NewRouteFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.new_route_fragment, container, false);

        //Avoid screen to be switched off
        PowerManager pm = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Tag");

        mainLayout = rootView.findViewById(R.id.mainLayout);

        mainActivity = (MainActivity) getActivity();
        context = getActivity().getApplicationContext();

        //Connect/start route button
        connectButton = rootView.findViewById(R.id.addRoute);
        connectButton.setOnClickListener(this);
        connectButtonImage = rootView.findViewById(R.id.addRouteImage);

        //Dark background for dialog
        dialogBackground = rootView.findViewById(R.id.dialogBackground);

        //Reat-time data views
        rpm = rootView.findViewById(R.id.realTime_rpm_value);
        speed = rootView.findViewById(R.id.realTime_speed_value);
        consumption = rootView.findViewById(R.id.realTime_consumption_value);
        consumptionUnit = rootView.findViewById(R.id.realTime_consumption_unit);

        //On route layout
        onRouteLayout = rootView.findViewById(R.id.onRouteLayout);

        //Play/pause route button
        playPauseButton = rootView.findViewById(R.id.pauseRoute);
        playPauseButton.setOnClickListener(this);

        //Create incidence/alert button
        incidenceButton = rootView.findViewById(R.id.addIncidence);
        incidenceButton.setOnClickListener(this);

        //Map view
        mMapView = rootView.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(context);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Request permissions if already not granted
        if(canAccessLocation()) {
            this.loadMap();
        }else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {


                final String[] PERMISSIONS_LOCATION = {
                        Manifest.permission.ACCESS_FINE_LOCATION,

                };
                requestPermissions(PERMISSIONS_LOCATION, REQUEST_FINE_LOCATION);
            }
        }

        //Update connect button style depending on bluetooth connection state
        if(mainActivity.getConnectionState()!= null && mainActivity.getConnectionState().equals(CommService.STATE.CONNECTED)){
            updateView(NewRouteFragment.VIEW.CONNECTED);
        }else{
            updateView(NewRouteFragment.VIEW.NOT_CONNECTED);
        }

        return rootView;
    }


    @Override
    public void onClick(View view) {

        //Create a new route
        if(view.getId() == R.id.addRoute){

            CommService.STATE status = mainActivity.getConnectionState();

            //If offline state, try to connect
            if(status==null || status.equals(CommService.STATE.OFFLINE)){
                mainActivity.setMode(MainActivity.MODE.ONLINE);
            }else{

                //If already connected, start route
                if(status.equals(CommService.STATE.CONNECTED)){

                    //Check if there are vehicles available
                    if(VehicleBO.getInstance().getAllVehicles().isEmpty()){
                        showNoCarAvailableDialog();
                        return;
                    }

                    //Get vehicle information
                    mainActivity.setObdService(ObdProt.OBD_SVC_VEH_INFO, "");
                    RouteService.getInstance().createNewRoute();
                    showStartRouteDialog();

                }else if(status.equals(CommService.STATE.CONNECTING)){
                }
            }

        //Pause route
        }else if(view.getId() == R.id.pauseRoute){
            showPauseDialog();
            RouteService.getInstance().changeRouteStatus(RouteService.RouteStatus.PAUSED);

        //Add incidence
        }else if(view.getId() == R.id.addIncidence){
            showIncidenceDialog();

        //Add noise incidence
        }else if(view.getId() == R.id.incidence_type1){
            RouteService.getInstance().createAlert(RouteService.RouteAlertType.NOISE);
            newIncidenceDialog.dismiss();
            Toast.makeText(mainActivity, getString(R.string.new_route_new_incidence), Toast.LENGTH_SHORT).show();

        //Add engine incidence
        }else if(view.getId() == R.id.incidence_type2){
            RouteService.getInstance().createAlert(RouteService.RouteAlertType.ENGINE);
            newIncidenceDialog.dismiss();
            Toast.makeText(mainActivity, getString(R.string.new_route_new_incidence), Toast.LENGTH_SHORT).show();

        //Add brakes incidence
        }else if(view.getId() == R.id.incidence_type3){
            RouteService.getInstance().createAlert(RouteService.RouteAlertType.BRAKES);
            newIncidenceDialog.dismiss();
            Toast.makeText(mainActivity, getString(R.string.new_route_new_incidence), Toast.LENGTH_SHORT).show();

        //Add gear box incidence
        }else if(view.getId() == R.id.incidence_type4){
            RouteService.getInstance().createAlert(RouteService.RouteAlertType.GEAR_BOX);
            newIncidenceDialog.dismiss();
            Toast.makeText(mainActivity, getString(R.string.new_route_new_incidence), Toast.LENGTH_SHORT).show();

        //Add temperature incidence
        }else if(view.getId() == R.id.incidence_type5){
            RouteService.getInstance().createAlert(RouteService.RouteAlertType.TEMPERATURE);
            newIncidenceDialog.dismiss();
            Toast.makeText(mainActivity, getString(R.string.new_route_new_incidence), Toast.LENGTH_SHORT).show();

            //Add steer incidence
        }else if(view.getId() == R.id.incidence_type6){
            RouteService.getInstance().createAlert(RouteService.RouteAlertType.OVERSUBSTEER);
            newIncidenceDialog.dismiss();
            Toast.makeText(mainActivity, getString(R.string.new_route_new_incidence), Toast.LENGTH_SHORT).show();

            //Add speak incidence
        }else if(view.getId() == R.id.incidence_type7){
            //TODO record audio
            Toast.makeText(mainActivity, getString(R.string.new_route_coming_soon), Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Show pause/finish route dialog
     */
    private void showPauseDialog(){

        //Create dialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.new_route_pause_dialog_title));
        builder.setMessage(R.string.new_route_pause_dialog_body);

        //Create listener for positive answer
        builder.setPositiveButton(getString(R.string.new_route_restart_route), new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                RouteService.getInstance().changeRouteStatus(RouteService.RouteStatus.STARTED);
            }
        });

        //Create listener for negative answer
        builder.setNegativeButton(getString(R.string.new_route_finish_route), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                unlockDrawerMenu();
                wl.release();
                RouteService.getInstance().changeRouteStatus(RouteService.RouteStatus.CANCELLED);
                Toast.makeText(getActivity(), getString(R.string.routes_adapter_route_created), Toast.LENGTH_SHORT).show();
                updateView(VIEW.CONNECTED);
                rpm.setText("-");
                speed.setText("-");
                consumption.setText("-");
            }
        });

        pauseDialog = builder.create();
        pauseDialog.show();
    }


    /**
     * Open dialog to create a new incidence/alert
     */
    private void showIncidenceDialog(){

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.new_incidence_dialog, null);
        dialogBuilder.setView(dialogView);

        //Noise
        RelativeLayout incidence_type1 = dialogView.findViewById(R.id.incidence_type1);
        incidence_type1.setOnClickListener(this);

        //Engine
        RelativeLayout incidence_type2 = dialogView.findViewById(R.id.incidence_type2);
        incidence_type2.setOnClickListener(this);

        //Brakes
        RelativeLayout incidence_type3 = dialogView.findViewById(R.id.incidence_type3);
        incidence_type3.setOnClickListener(this);

        //Gear box
        RelativeLayout incidence_type4 = dialogView.findViewById(R.id.incidence_type4);
        incidence_type4.setOnClickListener(this);

        //Temperature
        RelativeLayout incidence_type5 = dialogView.findViewById(R.id.incidence_type5);
        incidence_type5.setOnClickListener(this);

        //Steering
        RelativeLayout incidence_type6 = dialogView.findViewById(R.id.incidence_type6);
        incidence_type6.setOnClickListener(this);

        //Audio -- TODO
        RelativeLayout incidence_type7 = dialogView.findViewById(R.id.incidence_type7);
        incidence_type7.setOnClickListener(this);

        newIncidenceDialog = dialogBuilder.create();

        //Change the layout in order to show incidence dialog
        newIncidenceDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                dialogBackground.setVisibility(View.VISIBLE);
                playPauseButton.setVisibility(View.INVISIBLE);
                incidenceButton.setVisibility(View.INVISIBLE);
            }
        });

        //Change the layout again once the dialog is closed
        newIncidenceDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                dialogBackground.setVisibility(View.INVISIBLE);
                playPauseButton.setVisibility(View.VISIBLE);
                incidenceButton.setVisibility(View.VISIBLE);
            }
        });

        //Change dialog measures
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(newIncidenceDialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = getResources().getDimensionPixelSize(R.dimen.incidence_dialog_height);

        //Show dialog
        newIncidenceDialog.show();
        newIncidenceDialog.getWindow().setAttributes(lp);
    }


    /**
     * Do stuff when permissions are granted
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    //If permission granted, start location service
                    LocationService.getInstance(mainActivity);

                    //and init map
                    loadMap();

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(context, getString(R.string.new_route_location_not_granted), Toast.LENGTH_LONG).show();
                }
            }
        }
    }


    /**
     * Load map and make zoom to user current position
     */
    private void loadMap(){
        mMapView.getMapAsync(new OnMapReadyCallback() {
            @SuppressLint("MissingPermission")
            @Override
            public void onMapReady(GoogleMap mMap) {
                googleMap = mMap;

                LocationService.getInstance(mainActivity).setMap(googleMap);

                googleMap.setMyLocationEnabled(true);
                googleMap.getUiSettings().setMyLocationButtonEnabled(false);
                googleMap.getUiSettings().setCompassEnabled(false);

                //Change dialog view
                CameraPosition cameraPosition = new CameraPosition.Builder().target(
                        new LatLng(LocationService.getInstance(mainActivity).latitude,
                                LocationService.getInstance(mainActivity).longitude))
                        .zoom(17)
                        .bearing(90)
                        .tilt(55)
                        .build();
                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        });
    }


    /**
     * Open dialog to start route.
     * A description and a car must be provided
     */
    private void showStartRouteDialog() {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.start_route_dialog, null);
        dialogBuilder.setView(dialogView);

        final EditText routeDescription = dialogView.findViewById(R.id.routeDescription);

        //Load Spinner values
        final Spinner carSpinner = dialogView.findViewById(R.id.carSelect);
        List<Vehicle> vehicleList = new ArrayList<>();

        //Create blank vehicle
        Vehicle blankVeh = new Vehicle();
        blankVeh.setManufacturer(getString(R.string.new_route_select_vehicle_empty));
        blankVeh.setModel("");
        vehicleList.add(blankVeh);
        vehicleList.addAll(VehicleBO.getInstance().getAllVehicles());

        // Creating adapter for spinner
        final VehicleSpinnerAdapter vehicleSpinnerAdapter = new VehicleSpinnerAdapter(getActivity(), R.layout.spinner_item, vehicleList);

        // attaching data adapter to spinner
        carSpinner.setAdapter(vehicleSpinnerAdapter);
        carSpinner.setSelected(false);  // must
        carSpinner.setSelection(0,false);  //must

        //Spiner on change listener
        carSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                Vehicle veh = vehicleSpinnerAdapter.getItem(pos);
                if("".equals(veh.getModel())){
                    RouteService.getInstance().setCurrentVehicle(null);
                }
            }

            public void onNothingSelected(AdapterView<?> parent) {}
        });

        //Create listener for positive answer
        dialogBuilder.setPositiveButton(getString(R.string.new_route_start_route), null);

        //Create listener for negative answer
        dialogBuilder.setNegativeButton(getString(R.string.new_route_cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {}
        });

        dialogBuilder.setTitle(getString(R.string.new_route_title));

        newRouteDialog = dialogBuilder.create();

        //Hide connect/start route button when the dialog is displayed
        newRouteDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                connectButton.setVisibility(View.INVISIBLE);
            }
        });

        //Show connect/start route button when the dialog is dismissed
        newRouteDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                connectButton.setVisibility(View.VISIBLE);
            }
        });

        //Show dialog
        newRouteDialog.show();

        Button positive = newRouteDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positive.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                boolean validatedFields = true;

                String routeDescriptionText = routeDescription.getText().toString().trim();
                if("".equals(routeDescriptionText)){
                    validatedFields = false;
                    routeDescription.setError(getString(R.string.new_route_fragment_mandatory_field));
                }

                Vehicle selectedVehicle = (Vehicle) carSpinner.getSelectedItem();
                if("".equals(selectedVehicle.getModel())){
                    TextView errorText = carSpinner.getSelectedView().findViewById(R.id.spinnerItemText);
                    errorText.setError("");
                    errorText.setTextColor(Color.RED);
                    validatedFields = false;
                }


                if(validatedFields){
                    RouteService.getInstance().setRouteDescription(routeDescriptionText);
                    RouteService.getInstance().setCurrentVehicle(selectedVehicle);
                    updateView(VIEW.ON_ROUTE);
                    newRouteDialog.dismiss();
                    startRecordingRoute();
                    Toast.makeText(getActivity(), getString(R.string.new_route_route_started), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Start recording route
     */
    private void startRecordingRoute(){

        //Make screen to be awake if recording route
        wl.acquire();
        //Lock drawer menu
        this.lockDrawerMenu();
        //Change route status
        LocationService.getInstance(mainActivity).setRouteStatus(RouteService.RouteStatus.STARTED);
        mainActivity.setObdService(ObdProt.OBD_SVC_DATA, "");
        RouteService.getInstance().changeRouteStatus(RouteService.RouteStatus.STARTED);

    }

    /**
     * Update the view with real-time data (rpm, speed and )
     * @param rpm
     * @param speed
     * @param consumption
     * @param tooSlow
     */
    public void changeOnRouteData(String rpm, String speed, String consumption, boolean tooSlow){
        this.speed.setText(speed);
        this.rpm.setText(rpm);
        this.consumption.setText(consumption);

        //If vehicle goes to slow, it makes no sense l/100km unit
        if(this.isTooSlow != tooSlow){
            this.isTooSlow = tooSlow;
            if(tooSlow){
                this.consumptionUnit.setText(getString(R.string.unit_l_h));
            }else{
                this.consumptionUnit.setText(getString(R.string.unit_l_100km));
            }
        }
    }

    /**
     * Update view depending on route status
     * @param view
     */
    protected void updateView(VIEW view){
        switch (view){
            case NOT_CONNECTED:
                //Change connect/start route button background and icon
                connectButton.setBackground(getResources().getDrawable(R.drawable.connect_buttom));
                connectButtonImage.setImageResource(R.drawable.power_button_white);
                break;
            case CONNECTED:
                //Change connect/start route button background and icon
                connectButton.setBackground(getResources().getDrawable(R.drawable.start_route_buttom));
                connectButtonImage.setImageResource(R.drawable.add_white);

                //Change view layout
                mainLayout.setVisibility(View.VISIBLE);
                onRouteLayout.setVisibility(View.INVISIBLE);
                break;
            case ON_ROUTE:
                //Change view layout
                mainLayout.setVisibility(View.INVISIBLE);
                onRouteLayout.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }

    }

    /**
     * If there is no vehicle created, a new route cannot be created
     */
    private void showNoCarAvailableDialog(){

        //Create dialog builder
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.new_route_fragment_no_vehicle_title));
        builder.setMessage(getString(R.string.new_route_fragment_no_vehicle_body));


        //Create listener for negative answer
        builder.setPositiveButton(getString(R.string.new_route_fragment_ok_button), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                noCarAvailableDialog.dismiss();
            }
        });

        noCarAvailableDialog = builder.create();
        noCarAvailableDialog.show();
    }


    /**
     * Lock drawer menu
     */
    public void lockDrawerMenu(){
        DrawerLayout drawerLayout = mainActivity.getDrawerMenu().getDrawerLayout();
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        mainActivity.getDrawerMenu().getActionBarDrawerToggle().setDrawerIndicatorEnabled(false);
    }

    /**
     * Unlock drawer menu
     */
    public void unlockDrawerMenu(){
        DrawerLayout drawerLayout = mainActivity.getDrawerMenu().getDrawerLayout();
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        mainActivity.getDrawerMenu().getActionBarDrawerToggle().setDrawerIndicatorEnabled(true);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the mainActivity and potentially other fragments contained in that
     * mainActivity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    private boolean canAccessLocation() {
        return(hasPermission(Manifest.permission.ACCESS_FINE_LOCATION));
    }

    private boolean hasPermission(String perm) {
        return(PackageManager.PERMISSION_GRANTED== ContextCompat.checkSelfPermission(context, perm));
    }
}
