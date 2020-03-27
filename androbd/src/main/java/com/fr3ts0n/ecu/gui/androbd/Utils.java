package com.fr3ts0n.ecu.gui.androbd;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Utils{

	private static Utils instance = null;

	public static Utils getInstance()     {
		if (instance == null) {
			instance = new Utils();
		}
		return instance;
	}

	/**
	 * Local constructor
	 */
	private Utils(){
	}


	/**
	 * Get a diff between two dates
	 * @param date1 the oldest date
	 * @param date2 the newest date
	 * @return the diff value, in the provided unit
	 */
	public Map<TimeUnit,Long> computeDiff(Date date1, Date date2) {
		long diffInMillies = date2.getTime() - date1.getTime();
		List<TimeUnit> units = new ArrayList<>(EnumSet.allOf(TimeUnit.class));
		Collections.reverse(units);
		Map<TimeUnit,Long> result = new LinkedHashMap<>();
		long millisRest = diffInMillies;
		for ( TimeUnit unit : units ) {
			long diff = unit.convert(millisRest,TimeUnit.MILLISECONDS);
			long diffInMillisForUnit = unit.toMillis(diff);
			millisRest = millisRest - diffInMillisForUnit;
			result.put(unit,diff);
		}
		return result;
	}


	/**
	 * Format dates to string
	 * @param date
	 * @return
	 */
	public String formatDate(Date date){
		return DateFormat.getDateTimeInstance(
				DateFormat.MEDIUM, DateFormat.SHORT).format(date);
	}
}
