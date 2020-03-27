package com.fr3ts0n.ecu.gui.androbd.view.adapters;

import android.content.Context;
import android.location.Location;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.fr3ts0n.ecu.gui.androbd.model.bo.RouteBO;
import com.fr3ts0n.ecu.gui.androbd.model.ObdData;
import com.fr3ts0n.ecu.gui.androbd.model.Route;
import com.fr3ts0n.ecu.gui.androbd.model.RouteData;
import com.fr3ts0n.ecu.gui.androbd.R;
import com.fr3ts0n.ecu.gui.androbd.Utils;
import com.fr3ts0n.ecu.gui.androbd.view.RouteStatsDialogFragment;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * View pager adapter for route statistics chart view
 */
public class RouteViewPagerAdapter extends PagerAdapter {

    private Context mContext;
    private String mRouteId;
    public RouteStatsDialogFragment mFragment;

    /**
     * Max, min, average and count values
     */
    int count = 0;
    float sum = 0;
    float max = 0;
    float min = 99999;


    /**
     * Constructor
     * @param context
     * @param routeId
     * @param fragment
     */
    public RouteViewPagerAdapter(Context context, String routeId, RouteStatsDialogFragment fragment) {
        mContext = context;
        mRouteId = routeId;
        mFragment = fragment;
    }

    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
        ViewPagerPages modelObject = ViewPagerPages.values()[position];

        LayoutInflater inflater = LayoutInflater.from(mContext);
        ViewGroup view = (ViewGroup) inflater.inflate(modelObject.getLayoutResId(), collection, false);

        //Basic type of view page layout, no chart
        if(modelObject.getLayoutResId() == R.layout.view_pager_item_basic_data){

            //Get route from database
            Route route = RouteBO.getInstance().findRouteById(mRouteId, null);

            //Description value
            TextView description = view.findViewById(R.id.viewpager_item_route_description);
            description.setText(route.getDescription());

            //Vehicle value
            TextView vehicle = view.findViewById(R.id.viewpager_item_route_vehicle_value);
            vehicle.setText(route.getVehicle().getManufacturer() + " " + route.getVehicle().getModel());

            //Start date value
            TextView date = view.findViewById(R.id.viewpager_item_route_startdate_value);
            date.setText(Utils.getInstance().formatDate(route.getStartDate()));

            //Calculate route duration
            TextView duration = view.findViewById(R.id.viewpager_item_route_duration_value);
            if(route.getStartDate() == null || route.getEndDate() == null){
                duration.setText("undefined");
            }else{
                Map<TimeUnit, Long> timeDifference  = Utils.getInstance().computeDiff(route.getStartDate(), route.getEndDate());
                String timeDifferenceString = "";
                for(TimeUnit tu: timeDifference.keySet()) {
                    switch (tu) {
                        case HOURS:
                            if(timeDifference.get(tu)!=0) {
                                timeDifferenceString = timeDifferenceString + timeDifference.get(tu).toString() + " " + mFragment.getString(R.string.pager_adapter_hour) + ", ";
                            }
                            break;
                        case MINUTES:
                            timeDifferenceString = timeDifferenceString + timeDifference.get(tu).toString() + " " + mFragment.getString(R.string.pager_adapter_minutes) + ", ";
                            break;
                        case SECONDS:
                            timeDifferenceString = timeDifferenceString + timeDifference.get(tu).toString() + " " + mFragment.getString(R.string.pager_adapter_seconds);
                            break;
                    }
                }
                duration.setText(timeDifferenceString);
            }



            //Calculate route distance
            TextView distance = view.findViewById(R.id.viewpager_item_route_distance_value);
            Location lastPoint = null;
            float dist = 0L;
            for(RouteData routeData: route.getDataList()){
                if(lastPoint==null){
                    lastPoint = new Location("");
                    lastPoint.setLatitude(routeData.getCoordinateX());
                    lastPoint.setLongitude(routeData.getCoordinateY());
                }else{
                    Location location = new Location("");
                    location.setLatitude(routeData.getCoordinateX());
                    location.setLongitude(routeData.getCoordinateY());
                    dist = dist + lastPoint.distanceTo(location);
                    lastPoint = location;
                }
            }
            //If distance is less than 1km...
            if(dist<1000){
                distance.setText(String.format(Locale.US,  "%.0f", dist)+" m");

            //If distance is more than 1km...
            }else{
                distance.setText(String.format(Locale.US,  "%.1f", dist/1000)+" Kms");
            }


        //Custom page information with chart
        }else if(modelObject.getLayoutResId() == R.layout.view_pager_item_chart){

            //Create chart
            LineChart chart = view.findViewById(R.id.chart);
            this.buildChart(chart, mRouteId, modelObject.getTypeKey());

            //Chart title value
            TextView chartTitle = view.findViewById(R.id.chartTitle);
            switch (modelObject.getChartTitle()){
                case "1":
                    chartTitle.setText(mFragment.getString(R.string.pager_adapter_chart_title1));
                    break;
                case "2":
                    chartTitle.setText(mFragment.getString(R.string.pager_adapter_chart_title2));
                    break;
                case "3":
                    chartTitle.setText(mFragment.getString(R.string.pager_adapter_chart_title3));
                    break;
            }

            //Chart min value
            TextView chartMIN = view.findViewById(R.id.chart_min_value);
            chartMIN.setText(String.format(Locale.US, modelObject.getTypeKey().equals("consumption") ? "%.1f" : "%.0f", min));

            //Chart average value
            TextView chartAVG = view.findViewById(R.id.chart_avg_value);
            chartAVG.setText(String.format(Locale.US, modelObject.getTypeKey().equals("consumption") ? "%.1f" : "%.0f", (sum/count)));

            //Chart max value
            TextView chartMAX = view.findViewById(R.id.chart_max_value);
            chartMAX.setText(String.format(Locale.US, modelObject.getTypeKey().equals("consumption") ? "%.1f" : "%.0f", max));
        }

        collection.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup collection, int position, Object view) {
        collection.removeView((View) view);
    }

    @Override
    public int getCount() {
        return ViewPagerPages.values().length;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    /**
     * Create chart and calculate max, min, average values
     * @param chart
     * @param routeId
     * @param dataType
     */
    private void buildChart(LineChart chart, String routeId, String dataType){
        Route route = RouteBO.getInstance().findRouteById(routeId, null);
        List<Entry> entries = new ArrayList<>();

        //Init values
         count = 0;
         sum = 0;
         max = 0;
         min = 99999;

         //Get chart entries and calculate min, max and average values
        for (RouteData data : route.getDataList()) {
            for(ObdData obdData: data.getObdData()){
                if(dataType.equals(obdData.getMnemonic()) || dataType.equals("consumption")){

                    float value;
                    if(!dataType.equals("consumption")){ //Consumption value need extra calculation
                        value = Float.parseFloat(obdData.getValue());
                    }else{ //The rest of data, just get the value
                        value = mFragment.getConsumptionValue(data, route);
                    }
                    //Add entries
                    entries.add(new Entry(count, value));
                    sum = sum + value;
                    count++;
                    min = min < value ? min : value;
                    max = max > value ? max : value;
                }
            }
        }

        if(!entries.isEmpty()){
            //Styling
            LineDataSet dataSet = new LineDataSet(entries, null); // add entries to dataset
            dataSet.setColor(R.color.md_red_500);
            dataSet.setValueTextColor(R.color.md_blue_700);

            //Hide XAxis
            XAxis xAxis = chart.getXAxis();
            xAxis.setEnabled(false);

            //Hide legend
            Legend legend = chart.getLegend();
            legend.setEnabled(false);

            //Hide chart description
            chart.getDescription().setEnabled(false);

            //Set chart data
            LineData lineData = new LineData(dataSet);
            chart.setData(lineData);
            chart.invalidate();
        }

    }

}