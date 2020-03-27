package com.fr3ts0n.ecu.gui.androbd.model;

import java.util.UUID;

import io.realm.RealmObject;


public class
Fuel extends RealmObject {

    private String id;
    private String name;
    private float density;
    private float afr;

    public Fuel(){
        this.id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getDensity() {
        return density;
    }

    public void setDensity(float density) {
        this.density = density;
    }

    public float getAfr() {
        return afr;
    }

    public void setAfr(float afr) {
        this.afr = afr;
    }

}
