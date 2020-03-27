package com.fr3ts0n.ecu.gui.androbd.model;

import java.util.Date;
import java.util.UUID;

import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * Created by dpconde on 16/3/18.
 */

public class Route extends RealmObject {

    private String id;
    private Date startDate;
    private Date endDate;
    private RealmList<RouteData> dataList;
    private RealmList<RouteAlert> routeAlertList;
    private Vehicle vehicle;
    private String description;

    public Route(){
        this.id = UUID.randomUUID().toString();
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public RealmList<RouteData> getDataList() {
        if(dataList==null)
            dataList = new RealmList<>();
        return dataList;
    }

    public void setDataList(RealmList<RouteData> dataList) {
        this.dataList = dataList;
    }

    public RealmList<RouteAlert> getRouteAlertList() {
        return routeAlertList;
    }

    public void setRouteAlertList(RealmList<RouteAlert> routeAlertList) {
        this.routeAlertList = routeAlertList;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
