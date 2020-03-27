package com.fr3ts0n.ecu.gui.androbd.model.bo;

import com.fr3ts0n.ecu.gui.androbd.model.Fuel;
import com.fr3ts0n.ecu.gui.androbd.model.Route;
import com.fr3ts0n.ecu.gui.androbd.model.Vehicle;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;


/**
 * Vehicle entity business object
 */
public class VehicleBO {

    private static VehicleBO instance = null;
    private Realm realm;


    /**
     * Singleton pattern
     * @return
     */
    public static VehicleBO getInstance()     {
        if (instance == null) {
            instance = new VehicleBO();
        }
        return instance;
    }

    /**
     * Local constructor
     */
    private VehicleBO(){
        realm = Realm.getDefaultInstance();
    }

    /**
     * Get all available vehicles
     * @return
     */
    public List<Vehicle> getAllVehicles(){
        RealmQuery<Vehicle> query = realm.where(Vehicle.class);
        return query.findAll();
    }

    /**
     * Save vehicle
     * @param vehicle
     * @return
     */
    public Vehicle saveVehicle(Vehicle vehicle){
        Vehicle persisted;
        realm.beginTransaction();
        persisted = realm.copyToRealm(vehicle);
        realm.commitTransaction();
        return persisted;
    }

    /**
     * Delete vehicle
     * @param vehicleId
     * @return
     */
    public boolean deleteVehicle(String vehicleId){

        RealmResults<Route> routeList = realm.where(Route.class).equalTo("vehicle.id", vehicleId).findAll();
        if(!routeList.isEmpty()){
            return false;
        }else{
            RealmResults<Vehicle> vehicleList = realm.where(Vehicle.class).equalTo("id", vehicleId).findAll();
            if(!vehicleList.isEmpty()){
                realm.beginTransaction();
                vehicleList.get(0).deleteFromRealm();
                realm.commitTransaction();

                return true;
            }
        }
        return false;
    }

    /**
     * Update vehicle
     * @param vehicleId
     * @param manufacturer
     * @param model
     */
    public void updateVehicle(String vehicleId, String manufacturer, String model, Fuel fuelType){
        RealmResults<Vehicle> vehicleList = realm.where(Vehicle.class).equalTo("id", vehicleId).findAll();

        if(!vehicleList.isEmpty()){
            Vehicle vehicle = vehicleList.get(0);
            realm.beginTransaction();
            vehicle.setModel(model);
            vehicle.setManufacturer(manufacturer);
            vehicle.setFuelType(fuelType);
            realm.commitTransaction();
        }

    }
}
