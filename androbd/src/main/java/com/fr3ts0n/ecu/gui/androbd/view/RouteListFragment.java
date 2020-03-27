package com.fr3ts0n.ecu.gui.androbd.view;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fr3ts0n.ecu.gui.androbd.model.bo.RouteBO;
import com.fr3ts0n.ecu.gui.androbd.view.adapters.RouteListAdapter;
import com.fr3ts0n.ecu.gui.androbd.model.Route;
import com.fr3ts0n.ecu.gui.androbd.R;
import com.google.android.gms.maps.GoogleMap;

import java.util.List;

/**
 * Fragment to see the route list
 */
public class RouteListFragment extends Fragment implements View.OnClickListener{

    private RecyclerView mRecyclerView;
    private OnFragmentInteractionListener mListener;
    private List<Route> routeList;
    private RouteListAdapter mAdapter;

    public RouteListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.route_list_fragment, container, false);

        //Get all routes from database
        routeList = RouteBO.getInstance().getAllRoutes();

        mRecyclerView = rootView.findViewById(R.id.routeList);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setRecyclerListener(mRecycleListener);

        // use a linear layout manager
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter
        mAdapter = new RouteListAdapter(routeList, this);
        mRecyclerView.setAdapter(mAdapter);

        return rootView;
    }


    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View view) {

    }

    /**
     * Open full screen stats dialog
     * @param route
     */
    public void openStatsDialog(Route route){
        RouteStatsDialogFragment dialog = RouteStatsDialogFragment.newInstance(route.getId());
        FragmentTransaction ft = getActivity().getFragmentManager().beginTransaction();
        dialog.show(ft, RouteStatsDialogFragment.TAG);
    }



    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }


    private RecyclerView.RecyclerListener mRecycleListener = new RecyclerView.RecyclerListener() {

        @Override
        public void onViewRecycled(RecyclerView.ViewHolder holder) {
            RouteListAdapter.RouteViewHolder mapHolder = (RouteListAdapter.RouteViewHolder) holder;
            if (mapHolder != null && mapHolder.googleMap != null) {
                mapHolder.googleMap.clear();
                mapHolder.googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            }
        }
    };

    public void listHasChanged(){
        mAdapter.notifyDataSetChanged();
    }


}
