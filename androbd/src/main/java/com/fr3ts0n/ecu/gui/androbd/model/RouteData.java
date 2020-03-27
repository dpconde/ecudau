package com.fr3ts0n.ecu.gui.androbd.model;

import java.util.Date;

import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * Created by dpconde on 16/3/18.
 */

public class RouteData extends RealmObject {


    private Date time;
    private double coordinateX;
    private double coordinateY;
    private RealmList<ObdData> obdData;


    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public double getCoordinateX() {
        return coordinateX;
    }

    public void setCoordinateX(double coordinateX) {
        this.coordinateX = coordinateX;
    }

    public double getCoordinateY() {
        return coordinateY;
    }

    public void setCoordinateY(double coordinateY) {
        this.coordinateY = coordinateY;
    }

    public RealmList<ObdData> getObdData() {
        return obdData;
    }

    public void setObdData(RealmList<ObdData> obdData) {
        this.obdData = obdData;
    }


}
