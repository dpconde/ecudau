package com.fr3ts0n.ecu.gui.androbd.view.adapters;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fr3ts0n.ecu.gui.androbd.model.bo.VehicleBO;
import com.fr3ts0n.ecu.gui.androbd.model.Vehicle;
import com.fr3ts0n.ecu.gui.androbd.R;
import com.fr3ts0n.ecu.gui.androbd.view.VehicleListFragment;

import java.util.List;

/**
 * Adapter for the vehicle recycler view
 */
public class VehicleListAdapter extends RecyclerView.Adapter<VehicleListAdapter.VehicleViewHolder> {

    private List<Vehicle> data;
    private VehicleListFragment fragment;

    /**
     * Constructor
     * @param data
     * @param fragment
     */
    public VehicleListAdapter(List<Vehicle> data, VehicleListFragment fragment) {
        this.data = data;
        this.fragment = fragment;
    }

    @Override
    public VehicleViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.vehicle_list_item, viewGroup, false);
        VehicleViewHolder tvh = new VehicleViewHolder(itemView, fragment);

        return tvh;
    }

    @Override
    public void onBindViewHolder(VehicleViewHolder viewHolder, int pos) {
        Vehicle item = data.get(pos);
        viewHolder.bindTitular(item);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }


    /**
     * Custom ViewHolder class for vehicle information
     */
    public static class VehicleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        private TextView vehicleText;
        private RelativeLayout deleteButton;
        private RelativeLayout editButton;
        private VehicleListFragment fragment;
        private Vehicle vehicle;
        private AlertDialog deleteVehicleDialog;

        public VehicleViewHolder(View itemView, VehicleListFragment fragment) {
            super(itemView);

            this.fragment = fragment;
            vehicleText = itemView.findViewById(R.id.vehicleName);

            //Delete button
            deleteButton = itemView.findViewById(R.id.deleteVehicleButton);
            deleteButton.setOnClickListener(this);

            //Edit button
            editButton = itemView.findViewById(R.id.editVehicleButton);
            editButton.setOnClickListener(this);

        }

        public void bindTitular(Vehicle v) {
            this.vehicle = v;
            vehicleText.setText(v.toString());
        }

        @Override
        public void onClick(View view) {
            if(view.getId()==R.id.deleteVehicleButton){
                showConfirmDeleteVehicle();
            }else if(view.getId() == R.id.editVehicleButton){
                //Show create/edit dialog with the selected vehicle information
                fragment.showCreateEditVehicleDialog(vehicle);
            }
        }

        /**
         * Dialog to confirm  vehicle deletion
         */
        private void showConfirmDeleteVehicle(){

            //Create dialog builder
            AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getActivity());
            builder.setTitle(vehicle.getManufacturer() + " " + vehicle.getModel());
            builder.setMessage(fragment.getString(R.string.vehicle_adapter_delete_body));

            //Create listener for positive answer
            builder.setPositiveButton(fragment.getString(R.string.vehicle_adapter_delete_confirm), new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    boolean result = VehicleBO.getInstance().deleteVehicle(vehicle.getId());
                    Toast.makeText(fragment.getActivity(),
                            result ? fragment.getString(R.string.delete_vehicle_success) : fragment.getString(R.string.delete_vehicle_error),
                            Toast.LENGTH_SHORT).show();
                    fragment.listHasChanged();
                }
            });

            //Create listener for negative answer
            builder.setNegativeButton(fragment.getString(R.string.vehicle_adapter_delete_cancel), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    deleteVehicleDialog.dismiss();
                }
            });

            //Show dialog
            deleteVehicleDialog = builder.create();
            deleteVehicleDialog.show();
        }
    }

}