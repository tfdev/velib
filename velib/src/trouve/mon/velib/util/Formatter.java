package trouve.mon.velib.util;

import java.util.Locale;

import android.content.Context;
import trouve.mon.velib.R;

public class Formatter {
	
	//TODO find something i18n compliant
	public static String capitalizeString(String string) {
		char[] chars = string.toLowerCase(Locale.getDefault()).toCharArray();
		boolean found = false;
		for (int i = 0; i < chars.length; i++) {
			if (!found && Character.isLetter(chars[i])) {
				chars[i] = Character.toUpperCase(chars[i]);
				found = true;
			} else if (Character.isWhitespace(chars[i]) || chars[i]=='-' || chars[i]=='\'' ) {
				found = false;
			}
		}
		return String.valueOf(chars);
	}
	
	//TODO handle kilometers
	public static String formatDistance(int meters, Context context){
		String distanceString = "";
		if(meters != 0){
			if(meters < 1000){
				distanceString = String.format("%d", meters)+ context.getString(R.string.meter);
			}
			else{
				float km = ((float)meters) / 1000;
				distanceString = km + context.getString(R.string.kilometer);
			}
				
		}
		return distanceString;
	}
	
	//TODO
	public static String formatName(String name){
		return null;
	}
}
