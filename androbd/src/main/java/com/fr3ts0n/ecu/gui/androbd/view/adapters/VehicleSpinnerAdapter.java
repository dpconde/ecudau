package com.fr3ts0n.ecu.gui.androbd.view.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.fr3ts0n.ecu.gui.androbd.model.Vehicle;
import com.fr3ts0n.ecu.gui.androbd.R;

import java.util.List;

/**
 * Adapter for vehicle spinner when creating a new route
 */
public class VehicleSpinnerAdapter extends ArrayAdapter<Vehicle> {

    private final LayoutInflater mInflater;
    private final Context mContext;
    private final List<Vehicle> items;
    private final int mResource;
    private final String SPACE = " ";

    /**
     * Constructor
     * @param context
     * @param resource
     * @param objects
     */
    public VehicleSpinnerAdapter(Context context, int resource, List objects) {
        super(context, resource, 0, objects);
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mResource = resource;
        items = objects;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }

    private View createItemView(int position, View convertView, ViewGroup parent) {
        final View view = mInflater.inflate(mResource, parent, false);

        TextView vehicleText =  view.findViewById(R.id.spinnerItemText);
        Vehicle veh = items.get(position);
        vehicleText.setText(veh.getManufacturer() + SPACE + veh.getModel());

        return view;
    }
}
