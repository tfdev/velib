package trouve.mon.velib.util;

import java.text.DecimalFormat;

import trouve.mon.velib.R;
import android.content.Context;

public class Formatter {

	
	public static String formatDistance(int meters, Context context){
		String distanceString = "";
		if(meters != 0){
			if(meters < 1000){
				distanceString = String.format("%d", meters)+ context.getString(R.string.meter);
			}
			else{
				float km = ((float)meters) / 1000;
				DecimalFormat decimalFormat = new DecimalFormat("#.#");
				distanceString = decimalFormat.format(km) + context.getString(R.string.kilometer);
			}
				
		}
		return distanceString;
	}
	

	public static String formatName(String name){
		String formattedName = "";
		if(name != null){
			if(name.indexOf('-') != -1){
                formattedName = name.substring(name.indexOf('-')+2);
            }
            else if(name.indexOf('_') != -1){
                formattedName = name.substring(name.indexOf('_')+1);
            }
		}
		return formattedName;
	}
}
