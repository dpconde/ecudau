package com.fr3ts0n.ecu.gui.androbd.model.bo;

import com.fr3ts0n.ecu.gui.androbd.model.Fuel;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;

/**
 * Fuel entity business object
 */
public class FuelBO {

    private static FuelBO instance = null;
    private Realm realm;

    public static FuelBO getInstance()     {
        if (instance == null) {
            instance = new FuelBO();
        }
        return instance;
    }

    /**
     * Local constructor
     */
    private FuelBO(){
        realm = Realm.getDefaultInstance();
        initFuels();
    }

    /**
     * This method return a list of available fuel types
     * @return
     */
    public List<Fuel> getAllFuels(){
        RealmQuery<Fuel> query = realm.where(Fuel.class);
        return query.findAll();
    }

    /**
     * Create default fuel types
     * Gas and Diesel
     */
    private void initFuels(){
        if(getAllFuels().size() < 2){

            Fuel gas = new Fuel();
            gas.setName("Gas");
            gas.setAfr(14.7f);
            gas.setDensity(725.0f);

            Fuel diesel = new Fuel();
            diesel.setName("Diesel");
            diesel.setAfr(14.6f);
            diesel.setDensity(870.0f);

            realm.beginTransaction();
            RealmResults<Fuel> fuelList = realm.where(Fuel.class).findAll();
            fuelList.deleteAllFromRealm();
            realm.copyToRealm(gas);
            realm.copyToRealm(diesel);
            realm.commitTransaction();
        }
    }

}
