package com.fr3ts0n.ecu.gui.androbd.services;

import android.app.Activity;

import com.fr3ts0n.ecu.EcuDataPv;
import com.fr3ts0n.ecu.gui.androbd.model.bo.RouteBO;
import com.fr3ts0n.ecu.gui.androbd.model.Route;
import com.fr3ts0n.ecu.gui.androbd.model.RouteAlert;
import com.fr3ts0n.ecu.gui.androbd.model.Vehicle;
import com.fr3ts0n.ecu.gui.androbd.view.MainActivity;
import com.fr3ts0n.ecu.gui.androbd.view.NewRouteFragment;
import com.fr3ts0n.pvs.PvList;

import java.util.Calendar;
import java.util.Locale;

import io.realm.Realm;

/**
 * Created by dpconde on 14/4/18.
 */

public class RouteService {

    /**
     * Current instance
     */
    private static RouteService instance;

    /**
     * Current data to be saved
     */
    private Route currentRoute;
    private Vehicle currentVehicle;

    /**
     * Current views
     */
    private RouteStatus currentRouteStatus;
    private MainActivity currentActivity;
    private NewRouteFragment currentMapFragment;

    /**
     * Realm object
     */
    private Realm realm;

    private int count = 2; //Skipped data


    /**
     * Route status
     */
    public enum RouteStatus {
        NOT_CONNECTED,
        SETTING_UP_VEHICLE,
        READY,
        STARTED,
        PAUSED,
        CANCELLED
    }

    /**
     * List of route alerts/incidences
     */
    public enum RouteAlertType {
        NOISE,
        ENGINE,
        BRAKES,
        GEAR_BOX,
        TEMPERATURE,
        OVERSUBSTEER,
        SPEAK,
        START_ROUTE,
        PAUSE_ROUTE,
        FINISH_ROUTE;

        @Override
        public String toString() {
            switch (this) {
                case NOISE:
                    return "NOISE";
                case ENGINE:
                    return "ENGINE";
                case BRAKES:
                    return "BRAKES";
                case GEAR_BOX:
                    return "GEAR_BOX";
                case TEMPERATURE:
                    return "TEMPERATURE";
                case OVERSUBSTEER:
                    return "OVERSUBSTEER";
                case SPEAK:
                    return "SPEAK";
                case START_ROUTE:
                    return "START_ROUTE";
                case PAUSE_ROUTE:
                    return "PAUSE_ROUTE";
                case FINISH_ROUTE:
                    return "FINISH_ROUTE";
                default:
                    throw new IllegalArgumentException();
            }
        }
    }


    /**
     * Singleton implementation
     * @return
     */
    public static RouteService getInstance()     {
        if (instance == null) {
            instance = new RouteService();
        }
        return instance;
    }

    /**
     * Local constructor
     */
    private RouteService(){
    }

    /**
     * Init class
     * @param activity
     */
    public void init(Activity activity){
        realm = Realm.getDefaultInstance();
        currentRouteStatus = RouteStatus.NOT_CONNECTED;
        currentActivity = (MainActivity) activity;
        setVehicleInformationAvailable(true);
    }

    /**
     * Save OBD route data
     * @param data
     */
    public void saveRouteData(PvList data){

       if(currentRouteStatus.equals(RouteStatus.STARTED)){

          currentMapFragment = currentActivity.getMapFragment();

          //Save data every two requests
          if(count==2){
              //Save data in DataBase
              RouteBO.getInstance().saveRouteInformation(
                      currentRoute.getId(),
                      data,
                      LocationService.getInstance(currentActivity).latitude,
                      LocationService.getInstance(currentActivity).longitude);
              count=1;
          }else{
              count++;
          }


           //Show real time data on screen
           boolean tooLow;
           float rpm = (float) ((EcuDataPv)data.get("0C.0.0")).get("VALUE"); //RPM value
           float speed =  (float) ((EcuDataPv)data.get("0D.0.0")).get("VALUE"); //Speed value
           float massAirFlow =  (float) ((EcuDataPv)data.get("10.0.0")).get("VALUE"); //Mass Air Flow value

           //Get consumption
           float volumetricFuelFlow, consumption;

           //Car has Lambda value
           try{
               float lambdaValue =  (float) ((EcuDataPv)data.get("24.0.0")).get("VALUE");
               float lambdaRatio = 0.23478f / ( 0.218911f - 0.18415f * lambdaValue );
               volumetricFuelFlow = (massAirFlow/1000 * 3600/(lambdaRatio * currentVehicle.getFuelType().getAfr())) * (currentVehicle.getFuelType().getDensity()/1000);

           //If there is an Exception, car has no Lambda sensor
           }catch (Exception e){
               //car has no lambda
               volumetricFuelFlow = (massAirFlow/1000 * 3600/(currentVehicle.getFuelType().getAfr())) * (currentVehicle.getFuelType().getDensity()/1000);
           }

           if(volumetricFuelFlow<0){
               tooLow = false; //show l/100km
               consumption = 0.0f;
           }else{
               if(speed<10){
                   tooLow = true;// show litres/hour
                   consumption = volumetricFuelFlow; // l/h
               }else{
                   tooLow = false; //show l/100km
                   consumption = volumetricFuelFlow * 100 / speed; //l/100km
               }
           }

           currentMapFragment.changeOnRouteData(
                   String.format(Locale.US,"%.0f", rpm),
                   String.format(Locale.US,"%.0f", speed),
                   String.format(Locale.US,"%.1f", consumption),
                   tooLow);
       }
    }

    /**
     * Create new route
     */
    public void createNewRoute(){
        currentRoute = new Route();
    }

    /**
     * Method to save vehicle information
     * ** Still not developed **
     * @param data
     * @return
     */
    public boolean saveCarInformation(PvList data){

        if(currentRouteStatus.equals(RouteStatus.SETTING_UP_VEHICLE)){
            //TODO
            this.changeRouteStatus(RouteStatus.READY);
        }
        return false;
    }

    /**
     * Change route status
     * @param status
     */
    public void changeRouteStatus(RouteStatus status){

        currentRouteStatus=status;

        switch (status) {
            case NOT_CONNECTED:
                break;

            case SETTING_UP_VEHICLE:
                break;

            case READY:
                break;

            case STARTED:
                currentRoute = RouteBO.getInstance().saveRoute(currentRoute);
                createAlert(RouteAlertType.START_ROUTE);
                if(currentRoute.getStartDate()==null){
                    RouteBO.getInstance().setRouteStartDate(currentRoute);
                }
                break;

            case PAUSED:
                createAlert(RouteAlertType.PAUSE_ROUTE);
                break;

            case CANCELLED:
                createAlert(RouteAlertType.FINISH_ROUTE);
                RouteBO.getInstance().setRouteEndDate(currentRoute);
                LocationService.getInstance(currentActivity).finishRoute();
                currentRoute = null;
                break;

            default:
                throw new IllegalArgumentException();
        }
    }


    /**
     * Confirm that vehicle information is available
     * ** Still not developed **
     * @param vehicleInformationAvailable
     */
    public void setVehicleInformationAvailable(boolean vehicleInformationAvailable) {
        //TODO
        if(!vehicleInformationAvailable){
            changeRouteStatus(RouteStatus.READY);
        }
    }


    /**
     * Create alert/incidence and save it in database
     * @param type
     */
    public void createAlert(RouteAlertType type){

        RouteAlert routeAlert = new RouteAlert();
        routeAlert.setType(type.toString());
        routeAlert.setDate(Calendar.getInstance().getTime());
        routeAlert.setLatitude(LocationService.getInstance(currentActivity).latitude);
        routeAlert.setLongitude(LocationService.getInstance(currentActivity).longitude);

        realm.beginTransaction();
            final RouteAlert managedAlert = realm.copyToRealm(routeAlert);
            currentRoute.getRouteAlertList().add(managedAlert);
        realm.commitTransaction();
    }

    /**
     * Set vehicle to current route
     * @param vehicle
     */
    public void setCurrentVehicle(Vehicle vehicle){
        if(vehicle != null){
            this.currentVehicle = vehicle;

        if(!"".equals(vehicle.getModel())) //No option selected
            currentRoute.setVehicle(vehicle);
        }
    }

    /**
     * Save description to current route
     * @param description
     */
    public void setRouteDescription(String description){
        RouteBO.getInstance().setRouteDescription(currentRoute, description);
    }
}
