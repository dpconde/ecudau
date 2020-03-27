package com.fr3ts0n.ecu.gui.androbd.view.adapters;


import com.fr3ts0n.ecu.gui.androbd.R;

public enum ViewPagerPages {

    BasicData(0, "", "", R.layout.view_pager_item_basic_data),
    Rpm(1, "engine_speed", "1", R.layout.view_pager_item_chart),
    Speed(2, "vehicle_speed", "2", R.layout.view_pager_item_chart),
    Consumption(3, "consumption", "3", R.layout.view_pager_item_chart);

    private String mTypeKey;
    private int mLayoutResId;
    private int mIndex;


    private String mChartTitle;

    ViewPagerPages(int index, String typeKey, String chartTitle, int layoutResId) {
        mIndex = index;
        mTypeKey = typeKey;
        mLayoutResId = layoutResId;
        mChartTitle = chartTitle;
    }

    public String getTypeKey() {
        return mTypeKey;
    }

    public int getLayoutResId() {
        return mLayoutResId;
    }

    public int getIndex() {
        return mIndex;
    }

    public String getChartTitle() {
        return mChartTitle;
    }


}