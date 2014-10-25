package trouve.mon.velib.util;

import java.text.DecimalFormat;

import trouve.mon.velib.R;
import android.content.Context;
import android.widget.Toast;

public class Helper {

	public static void setUp(Context c){
		context = c;
	}
	
	private static Context context;
	
	/*public static final void setUpActionBarCustomTheme(Activity activity) {
		activity.setTheme(R.style.CustomActionBarTheme);
		ActionBar actionBar = activity.getActionBar();
	    actionBar.setDisplayShowTitleEnabled(false);
	    actionBar.setDisplayShowHomeEnabled(true);
	    actionBar.setDisplayUseLogoEnabled(true);
	}*/
	
	public static void showMessageQuick(int resId) {
		Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show();
	}
	
	public static void showMessageQuick(String msg) {
		Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
	}
	
	public static void showMessageLong(int resId) {
		Toast.makeText(context, context.getString(resId), Toast.LENGTH_LONG).show();
	}
	
	public static void showMessageLong(String msg) {
		Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
	}
	
	public static String formatDistance(int meters){
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
            else{
            	formattedName = name;
            }
		}
		return formattedName;
	}
}
