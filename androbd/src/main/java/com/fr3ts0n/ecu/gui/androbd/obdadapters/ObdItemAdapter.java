/*
 * (C) Copyright 2015 by fr3ts0n <erwin.scheuch-heilig@gmx.at>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */

package com.fr3ts0n.ecu.gui.androbd.obdadapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;

import com.fr3ts0n.androbd.plugin.mgr.PluginManager;
import com.fr3ts0n.ecu.EcuDataPv;
import com.fr3ts0n.ecu.gui.androbd.view.SettingsActivity;
import com.fr3ts0n.ecu.prot.obd.ObdProt;
import com.fr3ts0n.pvs.IndexedProcessVar;
import com.fr3ts0n.pvs.PvChangeEvent;
import com.fr3ts0n.pvs.PvChangeListener;
import com.fr3ts0n.pvs.PvList;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * Adapter for OBD data items (PVs)
 *
 * @author erwin
 */
public class ObdItemAdapter extends ArrayAdapter<Object> implements PvChangeListener
{
	transient public PvList pvs;
	transient protected boolean isPidList = false;
	transient protected LayoutInflater mInflater;
	transient public static final String FID_DATA_SERIES = "SERIES";
	/** allow data updates to be handled */
	public static boolean allowDataUpdates = true;
	transient SharedPreferences prefs;


	public ObdItemAdapter(Context context, int resource, PvList pvs)
	{
		super(context, resource);
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		mInflater = (LayoutInflater) context
			.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		setPvList(pvs);
	}

	/**
	 * set / update PV list
	 *
	 * @param pvs process variable list
	 */
	@SuppressWarnings("unchecked")
	public synchronized void setPvList(PvList pvs)
	{
		this.pvs = pvs;
		isPidList = (pvs == ObdProt.PidPvs);
		// get set to be displayed (filtered with preferences */
		Collection<Object> filtered = getPreferredItems(pvs, SettingsActivity.KEY_DATA_ITEMS);
		// make it a sorted array
		Object[] pidPvs = filtered.toArray();
		Arrays.sort(pidPvs, pidSorter);

		clear();
		// add all elements
		addAll(pidPvs);

		if (this.getClass() == ObdItemAdapter.class)
			addAllDataSeries();
	}

	@SuppressWarnings("rawtypes")
	static Comparator pidSorter = new Comparator()
	{
		public int compare(Object lhs, Object rhs)
		{
			// criteria 1: ID string
			int result =  lhs.toString().compareTo(rhs.toString());

			// criteria 2: description
			if(result == 0)
			{
				result = String.valueOf(((IndexedProcessVar)lhs).get(EcuDataPv.FID_DESCRIPT))
             .compareTo(String.valueOf(((IndexedProcessVar) rhs).get(EcuDataPv.FID_DESCRIPT)));
			}
			// return compare result
			return result;
		}
	};

	/**
	 * get set of data items filtered with set of preferred items to be displayed
	 * @param pvs list of PVs to be handled
	 * @param preferenceKey key of preference to be used as filter
	 * @return Set of filtered data items
	 */
	public Collection getPreferredItems(PvList pvs, String preferenceKey)
	{
		// filter PVs with preference selections
		Set<String> pidsToShow = prefs.getStringSet( SettingsActivity.KEY_DATA_ITEMS,
		                                             (Set<String>)pvs.keySet());
		return getMatchingItems(pvs, pidsToShow);
	}

	/**
	 * get set of data items filtered with set of preferred items to be displayed
	 * @param pvs list of PVs to be handled
	 * @param pidsToShow Set of keys to be used as filter
	 * @return Set of filtered data items
	 */
	public Collection<Object> getMatchingItems(PvList pvs, Set<String> pidsToShow)
	{
		HashSet<Object> filtered = new HashSet<Object>();
		for(Object key : pidsToShow)
		{
			IndexedProcessVar pv = (IndexedProcessVar) pvs.get(key);
			if(pv != null)
				filtered.add(pv);
		}
		return(filtered);
	}


	/**
	 * Add data series to all process variables
	 */
	protected synchronized void addAllDataSeries()
	{
		String pluginStr = "";
		for (IndexedProcessVar pv : (Iterable<IndexedProcessVar>) pvs.values())
		{
			// assemble data items for plugin notification
			pluginStr += String.format( "%s;%s;%s;%s\n",
				                        pv.get(EcuDataPv.FID_MNEMONIC),
										pv.get(EcuDataPv.FID_DESCRIPT),
										String.valueOf(pv.get(EcuDataPv.FID_VALUE)),
				                        pv.get(EcuDataPv.FID_UNITS)
			                          );
		}

		// notify plugins
		if(PluginManager.pluginHandler != null)
		{
			PluginManager.pluginHandler.sendDataList(pluginStr);
		}
	}

	@Override
	public void pvChanged(PvChangeEvent event)
	{
		if (allowDataUpdates)
		{
			IndexedProcessVar pv = (IndexedProcessVar) event.getSource();

			// send update to plugin handler
			if(PluginManager.pluginHandler != null)
			{
				PluginManager.pluginHandler.sendDataUpdate(
					pv.get(EcuDataPv.FID_MNEMONIC).toString(),
					event.getValue().toString());
			}
		}
	}
}
