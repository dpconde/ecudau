package com.fr3ts0n.ecu.gui.androbd.view.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.fr3ts0n.ecu.gui.androbd.model.Fuel;
import com.fr3ts0n.ecu.gui.androbd.R;

import java.util.List;

/**
 * Fuel spinner adapter
 */
public class FuelSpinnerAdapter extends ArrayAdapter<Fuel> {

    private final LayoutInflater mInflater;
    private final Context mContext;
    private final List<Fuel> items;
    private final int mResource;

    /**
     * Local constructor
     * @param context
     * @param resource
     * @param objects
     */
    public FuelSpinnerAdapter(Context context, int resource, List objects) {
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

        TextView gasText =  view.findViewById(R.id.spinnerItemText);

        Fuel fuel = items.get(position);
        String fuelLabel = "";

        if("Gas".equals(fuel.getName())){
            fuelLabel = mContext.getResources().getString(R.string.fuel_type_gas);
        }else if("Diesel".equals(fuel.getName())){
            fuelLabel = mContext.getResources().getString(R.string.fuel_type_diesel);
        }

        gasText.setText(fuelLabel);

        return view;
    }
}
