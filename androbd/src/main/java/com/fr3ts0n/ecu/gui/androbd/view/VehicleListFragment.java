package com.fr3ts0n.ecu.gui.androbd.view;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.fr3ts0n.ecu.gui.androbd.model.bo.FuelBO;
import com.fr3ts0n.ecu.gui.androbd.model.bo.VehicleBO;
import com.fr3ts0n.ecu.gui.androbd.view.adapters.FuelSpinnerAdapter;
import com.fr3ts0n.ecu.gui.androbd.view.adapters.VehicleListAdapter;
import com.fr3ts0n.ecu.gui.androbd.model.Fuel;
import com.fr3ts0n.ecu.gui.androbd.model.Vehicle;
import com.fr3ts0n.ecu.gui.androbd.R;

import java.util.List;

public class VehicleListFragment extends Fragment implements View.OnClickListener{

    private RecyclerView mRecyclerView;
    private FloatingActionButton createNewVehicleButton;
    private AlertDialog createNewVehicleDialog;
    private List<Vehicle> vehicleList;
    private VehicleListAdapter mAdapter;

    public VehicleListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.vehicle_list_fragment, container, false);

        //New vehicle button
        createNewVehicleButton = rootView.findViewById(R.id.createNewVehicleButton);
        createNewVehicleButton.setOnClickListener(this);

        //List view
        mRecyclerView = rootView.findViewById(R.id.followerList);
        mRecyclerView.setHasFixedSize(true);

        //Get all vehicle list
        vehicleList = VehicleBO.getInstance().getAllVehicles();

        //Use a linear layout manager
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        mRecyclerView.setLayoutManager(mLayoutManager);

        //Specify an adapter
        mAdapter = new VehicleListAdapter(vehicleList, this);
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();

        if (getArguments() != null) {
            if(getArguments().getBoolean("openNewVehicleDialog", false)){
                showCreateEditVehicleDialog(null);
            }
        }

        return rootView;
    }


    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onClick(View view) {

        //Open dialog to create a new vehicle
        if(view.getId() == R.id.createNewVehicleButton){
            showCreateEditVehicleDialog(null);
        }
    }

    /**
     * Show create/edit vehicle dialog
     * @param vehicle
     */
    public void showCreateEditVehicleDialog(final Vehicle vehicle) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.new_vehicle_dialog, null);
        dialogBuilder.setView(dialogView);

        //Get form fields
        final EditText vehicleManufacturer = dialogView.findViewById(R.id.manufacturerValue);
        final EditText vehicleModel = dialogView.findViewById(R.id.modelValue);
        final Spinner fuelType = dialogView.findViewById(R.id.fuelValue);

        //Load Spinner values
        final List<Fuel> fuelList = FuelBO.getInstance().getAllFuels();

        // Creating adapter for spinner
        final FuelSpinnerAdapter fuelSpinnerAdapter = new FuelSpinnerAdapter(getActivity(), R.layout.spinner_item, fuelList);

        //Attaching data adapter to spinner
        fuelType.setAdapter(fuelSpinnerAdapter);
        fuelType.setSelected(false);  // must
        fuelType.setSelection(0, false);  //must

        //If null, we are creating. If not, we are modifying
        if (vehicle != null) {
            vehicleManufacturer.setText(vehicle.getManufacturer());
            vehicleModel.setText(vehicle.getModel());
            fuelType.setSelection(fuelSpinnerAdapter.getPosition(vehicle.getFuelType()));
        }

        //Create listener for positive answer
        dialogBuilder.setTitle(vehicle==null ? getString(R.string.vehicle_fragment_create_title) : getString(R.string.vehicle_fragment_edit_title));

        //Create listener for positive answer
        dialogBuilder.setPositiveButton(vehicle==null ?
                getString(R.string.vehicle_fragment_create_button) : getString(R.string.vehicle_fragment_edit_button), null);

        //Create listener for positive answer
        dialogBuilder.setNegativeButton(getString(R.string.vehicle_fragment_cancel_button),new DialogInterface.OnClickListener(){

            public void onClick (DialogInterface dialog,int which){
                mAdapter.notifyDataSetChanged();
            }
        });

        createNewVehicleDialog = dialogBuilder.create();

        createNewVehicleDialog.setOnShowListener(new DialogInterface.OnShowListener(){
            @Override
            public void onShow (DialogInterface dialogInterface){}
        });

        //Refresh list after closing the dialog
        createNewVehicleDialog.setOnDismissListener(new DialogInterface.OnDismissListener(){
            @Override
            public void onDismiss (DialogInterface dialogInterface){
                mAdapter.notifyDataSetChanged();
            }
        });

        //Show dialog
        createNewVehicleDialog.show();

        Button positive = createNewVehicleDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positive.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick (View view){

                boolean validatedData = true;
                String manufacturerValue = vehicleManufacturer.getText().toString();
                String modelValue = vehicleModel.getText().toString();

                //Validate manufacturer field
                if ("".equals(manufacturerValue.trim())) {
                    vehicleManufacturer.setError(getString(R.string.vehicle_fragment_mandatory_field));
                    validatedData = false;
                }

                //Validate model value
                if ("".equals(modelValue.trim())) {
                    vehicleModel.setError(getString(R.string.vehicle_fragment_mandatory_field));
                    validatedData = false;
                }

                //Validate data before saving
                if (validatedData) {
                    if (vehicle == null) {
                        Vehicle veh = new Vehicle();
                        veh.setManufacturer(manufacturerValue);
                        veh.setModel(modelValue);
                        veh.setFuelType((Fuel) fuelType.getSelectedItem());
                        VehicleBO.getInstance().saveVehicle(veh);
                        Toast.makeText(getActivity().getApplicationContext(), getString(R.string.create_vehicle_success), Toast.LENGTH_SHORT).show();
                    } else {
                        VehicleBO.getInstance().updateVehicle(vehicle.getId(), manufacturerValue, modelValue, (Fuel)fuelType.getSelectedItem());
                        Toast.makeText(getActivity().getApplicationContext(), getString(R.string.edit_vehicle_success), Toast.LENGTH_SHORT).show();
                    }

                    createNewVehicleDialog.dismiss();
                }
            }
        });
    }

    /**
     * Refresh vehicle list
     */
    public void listHasChanged(){
        mAdapter.notifyDataSetChanged();
    }

}