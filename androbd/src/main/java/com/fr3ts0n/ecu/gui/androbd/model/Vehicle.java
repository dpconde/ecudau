package com.fr3ts0n.ecu.gui.androbd.model;

import java.util.UUID;

import io.realm.RealmObject;

/**
 * Created by dpconde on 16/3/18.
 */

public class
Vehicle extends RealmObject {

    private String id;
    private String manufacturer;
    private String model;
    private Fuel fuelType;

    public Vehicle(){
        this.id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Fuel getFuelType() {
        return fuelType;
    }

    public void setFuelType(Fuel fuelType) {
        this.fuelType = fuelType;
    }

    @Override
    public String toString() {
        return manufacturer + " " + model;
    }

}
