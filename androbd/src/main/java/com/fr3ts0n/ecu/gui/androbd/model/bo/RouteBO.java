package com.fr3ts0n.ecu.gui.androbd.model.bo;

import com.fr3ts0n.ecu.EcuDataPv;
import com.fr3ts0n.ecu.gui.androbd.model.ObdData;
import com.fr3ts0n.ecu.gui.androbd.model.Route;
import com.fr3ts0n.ecu.gui.androbd.model.RouteData;
import com.fr3ts0n.pvs.PvList;

import java.util.Calendar;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Route entity business object
 */
public class RouteBO {

    private static RouteBO instance = null;
    private Realm realm;


    public static RouteBO getInstance()     {
        if (instance == null) {
            instance = new RouteBO();
        }
        return instance;
    }

    /**
     * Local constructor
     */
    private RouteBO(){
        realm = Realm.getDefaultInstance();
    }

    /**
     * Return all available routes
     * @return
     */
    public List<Route> getAllRoutes(){
        RealmQuery<Route> query = realm.where(Route.class).isNotNull("vehicle").sort("startDate", Sort.DESCENDING);
        return query.findAll();
    }

    /**
     * Save route
     * @param route
     * @return
     */
    public Route saveRoute(Route route){
        realm.beginTransaction();
        route = realm.copyToRealm(route);
        realm.commitTransaction();
        return route;
    }

    /**
     * Save route description
     * @param route
     * @param description
     */
    public void setRouteDescription(Route route, String description){
        realm.beginTransaction();
        route.setDescription(description);
        realm.commitTransaction();
    }

    /**
     * Save route start date
     * @param route
     */
    public void setRouteStartDate(Route route){
        realm.beginTransaction();
        route.setStartDate(Calendar.getInstance().getTime());
        realm.commitTransaction();
    }

    /**
     * Save route end date
     * @param route
     */
    public void setRouteEndDate(Route route){
        realm.beginTransaction();
        route.setEndDate(Calendar.getInstance().getTime());
        realm.commitTransaction();
    }

    /**
     * Delete Route
     * @param routeId
     */
    public void deleteRoute(String routeId){

        RealmResults<Route> routeList = realm.where(Route.class).equalTo("id", routeId).findAll();
        final Route route = routeList.get(0);

        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                route.getDataList().deleteAllFromRealm();
                route.deleteFromRealm();
            }
        });
    }

    /**
     * Find route by route ID. It use a custom instance of Realm or the default one.
     * (Useful to be used in threads)
     * @param routeId
     * @param realm
     * @return
     */
    public Route findRouteById(String routeId, Realm realm){
        Route route;
        if(realm!=null){
            route = realm.where(Route.class).equalTo("id", routeId).findFirst();
        }else{
            route = this.realm.where(Route.class).equalTo("id", routeId).findFirst();
        }

        return route;
    }


    /**
     * Save route data in a specific place and in a specific time
     * @param routeID
     * @param data
     * @param latitude
     * @param longitude
     */
    public void saveRouteInformation(final String routeID, final PvList data, final double latitude, final double longitude){

        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm bgRealm) {
                RouteData routeData = bgRealm.createObject(RouteData.class);
                routeData.setTime(Calendar.getInstance().getTime());
                RealmResults<Route> routeList = bgRealm.where(Route.class).equalTo("id", routeID).findAll();
                Route route = routeList.get(0);
                RealmList<ObdData> list = new RealmList<>();

                for(Object ed: data.keySet()){

                    EcuDataPv ecuDataPv = (EcuDataPv) data.get(ed);
                    ObdData odbdata = bgRealm.createObject(ObdData.class); // Create managed objects directly

                    odbdata.setMaxValue(ecuDataPv.get("MAX") == null ? null : ecuDataPv.get("MAX").toString());
                    odbdata.setDescription(ecuDataPv.get("DESCRIPTION") == null ? null : ecuDataPv.get("DESCRIPTION").toString());
                    odbdata.setMinValue(ecuDataPv.get("MIN") == null ? null : ecuDataPv.get("MIN").toString());
                    odbdata.setValue(ecuDataPv.get("VALUE") == null ? null : ecuDataPv.get("VALUE").toString());
                    odbdata.setId(ecuDataPv.get("PID") == null ? null : ecuDataPv.get("PID").toString());
                    odbdata.setMnemonic(ecuDataPv.get("MNEMONIC") == null ? null : ecuDataPv.get("MNEMONIC").toString());
                    odbdata.setUnits(ecuDataPv.get("UNITS") == null ? null : ecuDataPv.get("UNITS").toString());

                    list.add(odbdata);
                }

                routeData.setCoordinateX(latitude);
                routeData.setCoordinateY(longitude);
                routeData.setObdData(list);
                route.getDataList().add(routeData);

            }
        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {

            }
        });
    }
}
