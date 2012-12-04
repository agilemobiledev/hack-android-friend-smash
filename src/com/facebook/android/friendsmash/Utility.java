package com.facebook.android.friendsmash;

import android.app.Activity;

/** 
 *  General Utility class for static methods that are needed in this application
 */
public class Utility {

	protected static String joinArrayOfStrings(String [] arrayOfStrings, String delimeter) {
		String result = "";
		
		for (int i=0; i<arrayOfStrings.length; i++) {
			result += arrayOfStrings[i];
			// Add a delimeter as long as it's not the last string
			if (i<arrayOfStrings.length-1) {
				result += delimeter;
			}
		}
		
		return result;
	}
	
	protected static int convertPixelToDensityPixel(int pixels, Activity activity) {
		
		// As per the following support Android article:
		// http://developer.android.com/guide/practices/screens_support.html#dips-pels
		
		// Get the screen's density scale
		final float scale = activity.getResources().getDisplayMetrics().density;
		
		// Convert the dps to pixels, based on density scale
		int densityPixels = (int) (pixels * scale + 0.5f);
		
		return densityPixels;
	}
	
}
